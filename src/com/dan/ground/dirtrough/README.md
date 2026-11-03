# com.dan.ground.dirtrough – Erde rau

Megascans „Military Trenches Ground Dirt Rough 02“: trockene Erde mit Kalk, Kies und kantigen Steinen, Scan 1 × 1 m, Relief 8,1 cm. Umgewandelt mit `tools/ground/megascans_surface.py` (Farbe 1024², Normalen 1024², Höhe 512² 16 bit). Form `GROUND`, statisch.

Varianten aus `GroundSpecies.surface`: Kachel 1 m (Zellen 5 mm, 80 000 Dreiecke), Fläche 6 m mit sanften Hügeln (2,5 cm, 115 000), Mulde 8 m (4 cm, 80 000). `DirtRough.material()` liefert das Material (etwa für eigene `GroundPatch`-Stücke oder `GroundBlend`), `DirtRough.ground()` das Relief als `GrassField.Ground`.

## Demo und Selbsttest

- `DirtRoughDemo [ansicht] [variante]`: Vorschau auf dem Prozessor. Ohne Fenster: `DirtRoughDemo --bild datei.png [einzel|oben|stufen] [variante] …`.
- `DirtRoughGlDemo [variante]`: JOGL mit Normal-Map. Ohne Fenster: `DirtRoughGlDemo --bild datei.png [variante]`.
- `DirtRoughCheck`: prüft Material (nahtlos, Relief, Normalen), Netze, Stufen und Stillstand. Endet mit „Alles in Ordnung.“

Neu umwandeln: `tools/ground/convert_ground.sh`.
