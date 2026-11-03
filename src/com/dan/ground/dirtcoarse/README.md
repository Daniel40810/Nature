# com.dan.ground.dirtcoarse – Erde grob

Megascans „Military Trenches Ground Dirt Coarse 01“: festgetretene Erde mit Kalk, Moos und Steinchen, Scan 2 × 2 m, Relief 2,8 cm. Umgewandelt mit `tools/ground/megascans_surface.py`. Form `GROUND`, statisch.

Varianten: Kachel 1 m, Fläche 6 m, Mulde 8 m (wie `dirtrough`) und **Übergang 6 m**: Erde auf der Seite −z, Rasen aus `com.dan.grass.ground` auf +z, Grenze geschwungen, gebacken mit `GroundBlend` (2048², etwa 1,5 s beim ersten Gebrauch). Als Rasen dient nur die Farbe, seine Höhe ist die Helligkeit.

## Demo und Selbsttest

- `DirtCoarseDemo [ansicht] [variante]`: Vorschau auf dem Prozessor. Ohne Fenster: `DirtCoarseDemo --bild datei.png [einzel|oben|stufen] [variante] …`.
- `DirtCoarseGlDemo [variante]`: JOGL mit Normal-Map. Ohne Fenster: `DirtCoarseGlDemo --bild datei.png 3` zeigt den Übergang.
- `DirtCoarseCheck`: prüft Material, Netze, Stufen und Stillstand. Endet mit „Alles in Ordnung.“

Neu umwandeln: `tools/ground/convert_ground.sh`.
