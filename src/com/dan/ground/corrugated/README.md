# com.dan.ground.corrugated – Wellblech

Megascans „Military Trenches Metal Corrugated 02“: rostiges Wellblech, Kachel 2 × 2 m mit 16 Wellen. Die Wellen sind Geometrie. Ihr Profil ist das Mittel der Höhenkarte über alle Zeilen (`Corrugated.profile()`), so liegen die Rippen von Textur und Netz übereinander. Die Tiefe ist 3 cm (`DEPTH`). Die 12 cm aus dem Scan wären für Blech viel zu tief. Jede Platte hat zwei Lagen im Abstand von 4 mm, also zwei Seiten. Form `OBJECT`.

| Variante | Aufbau | Dreiecke Stufe 0 / 3 |
|---|---|---|
| A Platte | 0,75 × 2,4 m, liegt leicht verzogen am Boden | 4 600 / 16 |
| B Wandstück | drei Platten nebeneinander, 2,2 m breit, 1,8 m hoch, 10° nach hinten geneigt, Vorderseite +z: Verschalung einer Grabenwand | 9 200 / 48 |
| C Bogen | Unterstand aus gebogenen Platten, Halbmesser 1 m, 2,2 m lang entlang z | 37 000 / 96 |

Je Welle 16, 8, 4 Punkte in den Stufen 0–2, Stufe 3 ist flach.

## Demo und Selbsttest

- `CorrugatedDemo [ansicht] [variante]`: Vorschau auf „Erde grob“. Ohne Fenster: `CorrugatedDemo --bild datei.png [einzel|gruppe|stufen] [variante] …`.
- `CorrugatedGlDemo [anzahl] [fläche m]`: viele Stücke mit JOGL.
- `CorrugatedCheck`: prüft Material, Netze, Größe, Stufen und Stillstand. Endet mit „Alles in Ordnung.“
