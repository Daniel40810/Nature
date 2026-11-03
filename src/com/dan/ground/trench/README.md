# com.dan.ground.trench – Schützengraben

Bausatz aus allen Paketen von `com.dan.ground` und Wildgras aus `com.dan.grass`. `Trench.standard()` ist ein Graben mit Schulterwehren: Abschnitte von 7,2 m im Wechsel um 2,4 m versetzt, 26 m lang, auf einem Gelände von 34 × 20 m.

- **Gelände (`species()`):** Sohle 1 m breit, 1,9 m tief, Wände 10° geneigt, außen sanfte Hügel. Relief aus „Erde grob“. Textur gebacken mit `GroundBlend` (4096 × 2048): im Graben und an der Kante „Erde rau“, außen „Erde grob“, Grenze geschwungen. Stufen mit Zellen 0,1 / 0,2 / 0,4 / 0,8 m (136 000 bis 2 150 Dreiecke).
- **Szene (`scene()`):** je Art die Stücke, alles auf der Höhe des Geländes:
  - Wellblech-Wandstücke an beiden Wänden, auf der Sohle
  - Sandsack-Mauern auf beiden Kanten (Brustwehr und Rückenwehr)
  - eine Stellung aus Sandsäcken hinter dem Graben
  - Felsen (`Rock.field()`)
  - Wildgras in Flecken, ab 1,6 m hinter der Kante, dichter je weiter weg
- **Eigene Gräben:** `new Trench(x0, z0, x1, z1, …)` mit den Punkten der Mittellinie. `depth`, `floorWidth`, `sizeX`, `sizeZ` und die Schalter `revetment`, `parapet`, `emplacement`, `rocks`, `grass` sind öffentlich. Abschnitte in Vielfachen von 2,4 m passen genau zu den Wandstücken und Mauern.

## Demo und Selbsttest

- `TrenchDemo [ansicht]`: Vorschau auf dem Prozessor, nah am Graben (etwa 5 s je Bild mit Gras). Ohne Fenster: `TrenchDemo --bild datei.png [einzel|oben|stufen]`.
- `TrenchGlDemo [gras je m²]`: alles mit JOGL, Gras im Wind (Vorgabe 10, 0 = ohne Gras). Ohne Fenster: `TrenchGlDemo --bild datei.png 10`.
- `TrenchCheck`: prüft Form (Sohle, Kante, außen eben) und Szene (Wellblech auf der Sohle, Sandsäcke außerhalb, kein Fels und kein Gras im Graben, reproduzierbar), dann das Gelände. Endet mit „Alles in Ordnung.“
