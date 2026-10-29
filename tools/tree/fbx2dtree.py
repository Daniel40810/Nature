#!/usr/bin/env python3
"""FBX-Baum (Stamm + Laubkarten mit Alpha-Textur, ohne Skelett) -> .dtree für com.dan.tree.

Liest binäres oder ASCII-FBX (fbxread.py), legt alle Meshes in den Baumraum (Meter, y oben), trennt
nach Material: Name mit "leaf" -> Laubkarten, sonst Stamm. Texturen werden verkleinert neben die
.dtree geschrieben (einmal je Präfix, mehrere Varianten teilen sie) und in der Datei mit Namen genannt.
Ein Skelett gibt es nicht; com.dan.tree leitet den Wind aus der Geometrie ab (ein Gelenk am Fuß).

Aufruf:
  pip install numpy pillow
  python3 fbx2dtree.py baum.fbx ziel.dtree --name "Name" --bark rinde.png --leaf laub.png --prefix art
"""
import argparse
import gzip
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import fbxread  # noqa: E402
from dtree import MAGIC, VERSION, Out, compact, write_mesh, write_tail  # noqa: E402

BARK_SIZE = 1024
LEAF_SIZE = 1024


def srgb_to_linear(c):
    c = np.asarray(c, dtype=np.float64)
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def write_textures(bark, leaf, outdir, prefix):
    """Verkleinern; im Laub die Farbe unter durchsichtige Stellen ziehen (keine Säume beim Filtern)."""
    names, means = [], []
    for kind, src, size in (('bark', bark, BARK_SIZE), ('leaf', leaf, LEAF_SIZE)):
        if not src:
            names.append('')
            means.append(None)
            continue
        im = Image.open(src).convert('RGBA')
        im = im.resize((min(size, im.width), min(size, im.height)), Image.LANCZOS)
        a = np.asarray(im).astype(np.float64) / 255
        if kind == 'leaf':
            alpha = a[..., 3:4]
            rgb = a[..., :3]
            fill = rgb.copy()
            for r in (2, 6, 16, 48):
                solid = (alpha >= 0.98).astype(np.float64)
                pm = Image.fromarray(np.uint8(np.clip(np.concatenate([rgb * solid, solid], 2) * 255, 0, 255)), 'RGBA')
                b = np.asarray(pm.filter(ImageFilter.GaussianBlur(r))).astype(np.float64) / 255
                ok = (b[..., 3:4] > 1e-3) & (alpha < 0.98)
                fill = np.where(ok & (fill == rgb), b[..., :3] / np.maximum(b[..., 3:4], 1e-3), fill)
            # Ränder (halb durchsichtig) tragen oft noch die Hintergrundfarbe der Vorlage: auch dort ersetzen
            a = np.concatenate([np.where(alpha < 0.98, fill, rgb), alpha], 2)
            sel = a[..., 3] > 0.5
            means.append(srgb_to_linear(a[sel][:, :3]).mean(0) if sel.any() else None)
            out = Image.fromarray(np.uint8(np.clip(a * 255, 0, 255)), 'RGBA')
        else:
            means.append(srgb_to_linear(a[..., :3].reshape(-1, 3)).mean(0))
            out = Image.fromarray(np.uint8(np.clip(a[..., :3] * 255, 0, 255)), 'RGB')
        name = f'{prefix}_{kind}.' + ('png' if kind == 'leaf' else 'jpg')
        path = os.path.join(outdir, name)
        if not os.path.exists(path):
            if kind == 'leaf':
                out.save(path, optimize=True)
            else:
                out.save(path, quality=88)
        names.append(name)
    return names, means


def convert(src, dst, name, bark, leaf, prefix):
    root = fbxread.read(src)
    axes, unit = fbxread.axes(root)
    parts = {'trunk': ([], [], [], []), 'leaf': ([], [], [], [])}
    for m in fbxread.meshes(root):
        M = m['matrix']
        lin = axes @ M[:3, :3]
        pts = ((axes @ (M[:3, :3] @ m['pts'].T + M[:3, 3:4])).T) * unit
        ninv = np.linalg.inv(lin).T
        pv, poly = m['pv'], m['poly']
        nc = len(pv)
        nrm = m['nrm'] if m['nrm'] is not None else np.zeros((nc, 3))
        nrm = (ninv @ nrm.T).T
        nrm /= np.maximum(np.linalg.norm(nrm, axis=1, keepdims=True), 1e-9)
        uv = m['uv'] if m['uv'] is not None else np.zeros((nc, 2))
        ends = np.concatenate([poly[1:], [nc]])
        for f, (a, b) in enumerate(zip(poly, ends)):
            mi = m['mat'][f] if f < len(m['mat']) else 0
            mname = m['materials'][mi] if mi < len(m['materials']) else ''
            target = parts['leaf' if 'leaf' in mname.lower() else 'trunk']
            for k in range(a + 1, b - 1):
                target[3].append((len(target[0]) + 0, len(target[0]) + 1, len(target[0]) + 2))
                for c in (a, k, k + 1):
                    target[0].append(pts[pv[c]])
                    target[1].append(nrm[c])
                    target[2].append((uv[c][0], 1 - uv[c][1]))     # Bildzeilen von oben

    def weld(p, n, t, tris):
        """Gleiche Ecken (Lage, UV, Normale) zusammenlegen."""
        p, n, t, tris = np.array(p), np.array(n), np.array(t), np.array(tris, dtype=np.int64)
        if len(p) == 0:
            return None
        key = np.concatenate([np.round(p / 1e-4), np.round(t * 8192), np.round(n * 64)], 1).astype(np.int64)
        _, first, inv = np.unique(key, axis=0, return_index=True, return_inverse=True)
        inv = inv.reshape(-1)
        tris = inv[tris]
        tris = tris[(tris[:, 0] != tris[:, 1]) & (tris[:, 1] != tris[:, 2]) & (tris[:, 0] != tris[:, 2])]
        p, n, t = p[first], n[first], t[first]
        p, tris, used = compact(p, tris)
        return p, n[used], t[used], tris

    trunk = weld(*parts['trunk'])
    foliage = weld(*parts['leaf'])
    if trunk is None:
        sys.exit('kein Stamm gefunden')
    tp, tn, tuv, tt = trunk
    # Fuß: Mitte der Stammpunkte im untersten halben Meter
    y0 = tp[:, 1].min()
    low = tp[tp[:, 1] < y0 + 0.5]
    cx, cz = low[:, 0].mean(), low[:, 2].mean()
    radius = float(np.median(np.hypot(low[:, 0] - cx, low[:, 2] - cz))) if len(low) else 0.2

    outdir = os.path.dirname(os.path.abspath(dst))
    (bark_name, leaf_name), (bark_mean, leaf_mean) = write_textures(bark, leaf, outdir, prefix)

    o = Out()
    o.i(MAGIC)
    o.i(VERSION)
    o.utf(name)
    o.utf(os.path.basename(src))
    o.f(0.0)
    o.z(False)
    o.fa(bark_mean if bark_mean is not None else [0.1, 0.08, 0.06])
    o.fa(leaf_mean if leaf_mean is not None else [0.06, 0.12, 0.03])
    o.i(1)                                   # ein Gelenk am Fuß
    o.ia([-1])
    o.fa([cx, max(y0, 0.0), cz])
    o.fa([max(radius, 0.02)])
    write_mesh(o, tp, tn, tt, np.zeros((len(tp), 2), dtype=np.int64), np.tile([1.0, 0.0], (len(tp), 1)))
    o.i(0)                                   # keine Zweig-Prototypen
    o.i(0)                                   # keine Instanzen
    write_tail(o, trunk_uv=tuv, foliage=foliage, bark_texture=bark_name, leaf_texture=leaf_name)
    with open(dst, 'wb') as raw, gzip.GzipFile(filename='', fileobj=raw, mode='wb', compresslevel=9, mtime=0) as fh:
        fh.write(bytes(o.b))       # ohne Zeitstempel: gleiche Eingabe, gleiche Datei
    nf = 0 if foliage is None else len(foliage[3])
    print(f'{os.path.basename(dst)}: Stamm {len(tt)} Dreiecke, Laub {nf} Dreiecke, '
          f'Höhe {max(tp[:, 1].max(), foliage[0][:, 1].max() if foliage else 0):.1f} m, '
          f'Texturen {bark_name or "-"} / {leaf_name or "-"}, {os.path.getsize(dst) / 1e6:.2f} MB')


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('src')
    ap.add_argument('dst')
    ap.add_argument('--name', default=None)
    ap.add_argument('--bark', default=None, help='Rindentextur (PNG, TGA, …)')
    ap.add_argument('--leaf', default=None, help='Laubtextur mit Alpha')
    ap.add_argument('--prefix', default=None, help='Präfix der geschriebenen Texturen (Vorgabe: Name der Zieldatei)')
    a = ap.parse_args()
    base = os.path.splitext(os.path.basename(a.dst))[0]
    convert(a.src, a.dst, a.name or base, a.bark, a.leaf, a.prefix or base)


if __name__ == '__main__':
    main()
