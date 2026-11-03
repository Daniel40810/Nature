# com.dan.ground.rock – Felsen

Netz aus „3D Lowpoly Rock“ (glTF, 428 Dreiecke), umgewandelt mit `tools/tree/gltf2dtree.py` nach `rock_lowpoly.dtree`. Die Textur im Pack ist nur ein 174-Byte-Bild für Metall und Rauheit. Als Oberfläche dient deshalb Gestein aus Megascans „Ground Dirt Rough 02“, entsättigt und aufgehellt (`megascans_surface.py --stone`). Form `OBJECT`.

Je Variante wird das verschweißte Netz gedreht, gestreckt und mit Rauschen nach außen verformt. Stufe 0 ist zweimal unterteilt (6 800 Dreiecke), Stufe 1 einmal, Stufe 2 ist das Original, Stufe 3 vereinfacht (unter 200). Die Flächen sind facettiert wie im Original, die Textur ist je Fläche von der Seite projiziert, zu der sie zeigt. Ein Zehntel der Höhe steckt im Boden.

Varianten: A Brocken (1,2 m, liegend), B Platte (1,6 m, flach), C Spitze (1,3 m hoch, stehend), D Stein (0,4 m). `Rock.field()` streut sie wie Gras: 0,04 je m², Maßstab 0,6–1,5, kleine Steine häufiger.

## Demo und Selbsttest

- `RockDemo [ansicht] [variante]`: Vorschau auf „Erde grob“. Ohne Fenster: `RockDemo --bild datei.png [einzel|gruppe|stufen] [variante] …`.
- `RockGlDemo [anzahl] [fläche m]`: viele Felsen mit JOGL.
- `RockCheck`: prüft Streuung, Material, Netze, Größe, Stufen und Stillstand. Endet mit „Alles in Ordnung.“

Neu umwandeln: `tools/ground/convert_ground.sh`.
