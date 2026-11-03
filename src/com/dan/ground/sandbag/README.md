# com.dan.ground.sandbag – Sandsäcke

Leinen von Megascans „Military Trenches Detail Sandbag Canvas“ (Kachel 1 × 1 m), die Säcke sind im Code geformt: Superquader 60 × 32 × 13 cm, im Grundriss kastenförmig, im Schnitt gerundet, unten flach, oben zu den Enden eingesunken, leicht beulig. Jeder Sack ist etwas anders (Größe, Beulen, Lage, Drehung), aus einem Hash, also reproduzierbar. Form `OBJECT`.

| Variante | Aufbau | Dreiecke Stufe 0 / 3 |
|---|---|---|
| A Sack | ein Sack | 2 300 / 64 |
| B Reihe 2,4 m | vier Säcke längs (entlang x) | 9 200 / 256 |
| C Mauer 2,4 m | vier Lagen versetzt, unten zwei Reihen tief, etwa 0,45 m hoch | 48 000 / 1 300 |
| D Stellung | Ring mit 1,4 m Halbmesser, zwei Lagen, Öffnung nach +x | 55 000 / 1 500 |

Die Teilung je Sack ist 48 × 24, 24 × 12, 12 × 8 und 8 × 4 in den Stufen 0–3.

## Demo und Selbsttest

- `SandbagDemo [ansicht] [variante]`: Vorschau auf „Erde rau“. Ohne Fenster: `SandbagDemo --bild datei.png [einzel|gruppe|stufen] [variante] …`.
- `SandbagGlDemo [anzahl] [fläche m]`: viele Stücke mit JOGL. Ohne Fenster: `SandbagGlDemo --bild datei.png 40 12`.
- `SandbagCheck`: prüft Material, Netze, Größe, Stufen und Stillstand. Endet mit „Alles in Ordnung.“
