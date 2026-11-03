#!/usr/bin/env python3
"""Megascans-Oberfläche (Fab-Zip mit 4K-JPEGs und JSON) -> Material für com.dan.ground.GroundMaterial.

Schreibt neben das Ziel:
  <prefix>_albedo.jpg    Farbe, mit etwas Verdeckung (AO) malgenommen, verkleinert
  <prefix>_normal.jpg    Normalen im OpenGL-Format (grün = +v nach oben im Bild), verkleinert
  <prefix>_height.png    Höhe 16 bit (0 tief .. 65535 hoch), verkleinert
  <prefix>.material      Eigenschaften: Name, Kachelgröße (m), Relief (m), Mitte der Höhe, mittlere Farbe (linear)

Die Größe der Kachel kommt aus "physicalSize" bzw. "scanArea", das Relief aus "height" in der JSON.
Ob die Normal-Map OpenGL oder DirectX ist, wird aus dem Gefälle der Höhenkarte bestimmt.

Aufruf:
  pip install numpy pillow
  python3 megascans_surface.py surface.zip zielordner prefix [--size 1024] [--height-size 512] [--ao 0.5] [--name "…"]
  python3 megascans_surface.py surface.zip zielordner prefix --stone      # Gestein: entsättigt, heller (für Felsen)
"""
import argparse
import glob
import io
import json
import os
import zipfile

import numpy as np
from PIL import Image

Image.MAX_IMAGE_PIXELS = None


def srgb_to_linear(c):
    c = np.asarray(c, dtype=np.float64)
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def linear_to_srgb(c):
    c = np.clip(c, 0, 1)
    return np.where(c <= 0.0031308, c * 12.92, 1.055 * c ** (1 / 2.4) - 0.055)


class Source:
    """Zip oder Ordner mit den Megascans-Dateien."""

    def __init__(self, path):
        self.zip = zipfile.ZipFile(path) if path.endswith('.zip') else None
        self.dir = None if self.zip else path
        self.names = self.zip.namelist() if self.zip else [os.path.relpath(p, path) for p in glob.glob(os.path.join(path, '**', '*'), recursive=True)]

    def find(self, suffix):
        for n in self.names:
            if n.lower().endswith(suffix.lower()):
                return n
        raise SystemExit(f'nicht gefunden: *{suffix}')

    def read(self, name):
        if self.zip:
            return self.zip.read(name)
        with open(os.path.join(self.dir, name), 'rb') as f:
            return f.read()

    def image(self, suffix):
        return Image.open(io.BytesIO(self.read(self.find(suffix))))


def prop(meta, key, default=None):
    for m in meta:
        if m.get('key') == key:
            return m.get('value')
    return default


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('src')
    ap.add_argument('dst')
    ap.add_argument('prefix')
    ap.add_argument('--size', type=int, default=1024)
    ap.add_argument('--height-size', type=int, default=512)
    ap.add_argument('--ao', type=float, default=0.5, help='Anteil der Verdeckung in der Farbe (0 = keine)')
    ap.add_argument('--name', default=None)
    ap.add_argument('--stone', action='store_true', help='Gestein: Farbe entsättigt und aufgehellt')
    a = ap.parse_args()
    src = Source(a.src)
    os.makedirs(a.dst, exist_ok=True)
    info = json.loads(src.read(src.find('.json')))
    meta = info.get('meta', [])
    tags = info.get('semanticTags', {})
    name = a.name or tags.get('name') or a.prefix
    size = str(prop(meta, 'physicalSize', prop(meta, 'scanArea', '1x1'))).lower().split('x')
    tile_x, tile_z = float(size[0]), float(size[-1])
    relief = float(str(prop(meta, 'height', '0.05')).split()[0])
    bias = 0.5
    for m in info.get('maps', []):
        if m.get('type') == 'displacement' and m.get('mimeType') == 'image/jpeg' and 'bias' in m:
            bias = float(m['bias'])

    # Farbe mit Verdeckung
    base = np.asarray(src.image('_Basecolor.jpg').convert('RGB').resize((a.size, a.size), Image.LANCZOS), dtype=np.float64) / 255
    ao = np.asarray(src.image('_AO.jpg').convert('L').resize((a.size, a.size), Image.LANCZOS), dtype=np.float64) / 255
    lin = srgb_to_linear(base) * (1 - a.ao + a.ao * ao)[..., None]
    if a.stone:
        lum = lin @ np.array([0.2126, 0.7152, 0.0722])
        lin = lin * 0.3 + lum[..., None] * 0.7
        lin = lin * 1.15 + 0.01
    Image.fromarray((linear_to_srgb(lin) * 255 + 0.5).astype(np.uint8)).save(os.path.join(a.dst, f'{a.prefix}_albedo.jpg'), quality=90)
    mean = lin.reshape(-1, 3).mean(0)

    # Höhe 16 bit
    hsrc = src.image('_Displacement.jpg').convert('F')                # Fließkomma: weich verkleinert, ohne Stufen
    h = np.clip(np.asarray(hsrc.resize((a.height_size, a.height_size), Image.LANCZOS), dtype=np.float64) / 255, 0, 1)
    Image.fromarray(np.clip(h * 65535 + 0.5, 0, 65535).astype(np.uint16)).save(os.path.join(a.dst, f'{a.prefix}_height.png'))

    # Normalen; Richtung von Grün aus dem Gefälle der Höhe bestimmen
    nimg = src.image('_Normal.jpg').convert('RGB').resize((a.size, a.size), Image.LANCZOS)
    n = np.asarray(nimg, dtype=np.float64) / 255 * 2 - 1
    hh = np.asarray(hsrc.resize((a.size, a.size), Image.LANCZOS), dtype=np.float64)
    dhdrow = np.roll(hh, -1, 0) - np.roll(hh, 1, 0)          # Höhe nimmt nach unten im Bild zu
    dhdcol = np.roll(hh, -1, 1) - np.roll(hh, 1, 1)
    # Normale zeigt bergab: nx ~ -dh/dx; OpenGL: ny ~ -dh/d(nach oben) = +dh/drow
    corr_x = np.corrcoef(n[..., 0].ravel(), -dhdcol.ravel())[0, 1]
    corr_y = np.corrcoef(n[..., 1].ravel(), dhdrow.ravel())[0, 1]
    flipped = corr_y < 0
    if flipped:
        n[..., 1] *= -1
    out = np.clip((n + 1) / 2 * 255 + 0.5, 0, 255).astype(np.uint8)
    Image.fromarray(out).save(os.path.join(a.dst, f'{a.prefix}_normal.jpg'), quality=92)

    with open(os.path.join(a.dst, f'{a.prefix}.material'), 'w', encoding='utf-8') as f:
        f.write(f'# aus {os.path.basename(a.src)} (tools/ground/megascans_surface.py)\n')
        f.write(f'name={name}\n')
        f.write(f'tileX={tile_x:g}\ntileZ={tile_z:g}\n')
        f.write(f'relief={relief:g}\n')
        f.write(f'heightMid={bias:.4f}\n')
        f.write(f'color={mean[0]:.4f},{mean[1]:.4f},{mean[2]:.4f}\n')
        f.write(f'albedo={a.prefix}_albedo.jpg\nnormal={a.prefix}_normal.jpg\nheight={a.prefix}_height.png\n')
    print(f'{a.prefix}: {name}, Kachel {tile_x:g} × {tile_z:g} m, Relief {relief:g} m, '
          f'Normalen {"DirectX → OpenGL" if flipped else "OpenGL"} (Bezug x {corr_x:+.2f}, y {corr_y:+.2f})')


if __name__ == '__main__':
    main()
