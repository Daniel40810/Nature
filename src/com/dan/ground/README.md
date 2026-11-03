# com.dan.ground – Böden und Gegenstände aus Megascans

Erde, Sandsäcke, Wellblech, Felsen und ein Schützengraben, nach dem Muster von `com.dan.tree` und `com.dan.grass`. Jede Art liegt in einem eigenen Paket `com.dan.ground.<art>` mit Demo in `com.dan.ground.<art>.demo`. Alles ist eine `TreeSpecies` und wird von den Renderern aus `com.dan.tree` gezeichnet (Prozessor und JOGL).

Anders als Bäume und Gras kommen die Netze nicht aus Dateien, sondern entstehen im Code (`TreeSpecies.generator`). Aus den Megascans-Assets stammen die **Oberflächen**: Farbe, Normal-Map und Höhenkarte. Die Höhenkarte formt auch die Geometrie, etwa das Relief der Erde oder die Wellen des Blechs.

## Kern

- **`GroundMaterial`:** eine kachelbare Oberfläche. Sie besteht aus Farbe (mit 50 % Verdeckung), Normal-Map (OpenGL-Format), Höhenkarte (16 bit) und `<prefix>.material` mit Kachelgröße, Relief und Mitte der Höhe aus der Megascans-JSON. `displacement(x, z)` liefert das Relief in m, `ground(…)` dasselbe als `GrassField.Ground`.
- **`GroundPatch`:** ein Stück Boden als Raster, Höhe = Form + Relief des Materials. Stufe 0 hat Zellen von `cell` m, jede weitere doppelt so große. Die Formen `hills`, `bowl` und `sum` sind reproduzierbar.
- **`GroundBlend`:** mischt zwei Böden über eine Maske zu einem Bild (Farbe und Normal-Map). An der Grenze entscheidet die Höhe: Steine des einen Bodens ragen in den anderen hinein.
- **`MeshBuilder`:** baut statische Netze (Raster, Normalen, Anhängen mit Drehung) und macht daraus ein `TreeAsset` ohne Laub.
- **`GroundSpecies`:**
  - `create(…)`: eine Art aus einem Generator (Variante, Stufe → Netz), Form `GROUND` oder `OBJECT`, je Variante vier eigene Stufen, ohne Wind.
  - `surface(…)`: die üblichen Geländestücke eines Materials.
- **`demo/`:** `GroundDemo` (Prozessor), `GroundGlDemo` (JOGL mit Normal-Maps) und `GroundCheck` (prüft das Material, dann alles aus `TreeCheck`). Die Pakete der Arten rufen sie mit einer Zeile auf.

Zum Herauskopieren reichen `src/com/dan/tree/` (ohne die Art-Unterordner), `src/com/dan/grass/` (ohne die Art-Unterordner, außer `ground/`), `src/com/dan/ground/` (ohne die Art-Unterordner) und der Ordner der Art. Der Graben braucht alle Arten und `com.dan.grass.wildgrass`. Die Demos brauchen `FStyle.jar`, `gl/` braucht JOGL.

## Arten

| Paket | Inhalt | Varianten | Dreiecke Stufe 0 | Form |
|---|---|---|---|---|
| `com.dan.ground.dirtrough` | Erde rau: Kalk, Kies, kantige Steine. Kachel 1 m, Relief 8 cm | Kachel 1 m, Fläche 6 m (Hügel), Mulde 8 m | 80 000–115 000 | Gelände |
| `com.dan.ground.dirtcoarse` | Erde grob: Moos, Steinchen. Kachel 2 m, Relief 3 cm | wie oben, dazu Übergang 6 m zum Rasen aus `com.dan.grass.ground` | 80 000–115 000 | Gelände |
| `com.dan.ground.sandbag` | Sandsäcke aus Leinen (60 × 32 × 13 cm), im Code geformt | Sack, Reihe 2,4 m, Mauer 2,4 m (4 Lagen), Stellung (Ring 1,4 m) | 2 300–55 000 | Gegenstand |
| `com.dan.ground.corrugated` | Wellblech, rostig; Wellen aus der Höhenkarte, beidseitig | Platte 0,75 × 2,4 m, Wandstück 2,2 × 1,8 m (10° geneigt), Bogen (Unterstand, Halbmesser 1 m) | 4 600–37 000 | Gegenstand |
| `com.dan.ground.rock` | Felsen aus dem Low-Poly-Fels (glTF), verformt und unterteilt, Gestein aus „Erde rau“ | Brocken 1,2 m, Platte 1,6 m, Spitze 1,3 m, Stein 0,4 m | 6 800 | Gegenstand |
| `com.dan.ground.trench` | Schützengraben mit Szene: Wellblech, Sandsäcke, Stellung, Felsen, Wildgras | Graben 26 m im Zickzack (34 × 20 m Gelände) | 136 000 (Gelände) | Gelände |

Die Stufen 1–3 haben Zellen bzw. Teilungen von ½, ¼ und ⅛. Stufe 3 liegt unter 1 000 Dreiecken oder unter 7 % von Stufe 0.

## Weg einer Oberfläche

```
Megascans-Zip (4K-JPEGs + JSON) ─▶ tools/ground/megascans_surface.py ─▶ <art>/<prefix>_albedo.jpg   (1024², Farbe × AO)
                                                                    ├▶ <art>/<prefix>_normal.jpg   (1024², OpenGL-Format)
                                                                    ├▶ <art>/<prefix>_height.png   (512², 16 bit)
                                                                    └▶ <art>/<prefix>.material     (Kachel, Relief, Mitte, Farbe)
glTF-Fels ─────────────────────────▶ tools/tree/gltf2dtree.py ─────▶ rock/rock_lowpoly.dtree
```

- **Normal-Map:** Ob sie OpenGL- oder DirectX-Format hat, bestimmt das Skript aus dem Gefälle der Höhenkarte. Bei DirectX dreht es Grün um. Alle vier Packs sind OpenGL.
- **Gestein (`--stone`):** Farbe entsättigt und aufgehellt. Der Fels bringt selbst keine Farbtextur mit.

Alles erzeugt `tools/ground/convert_ground.sh` neu (es läuft auch am Ende von `tools/tree/convert_all.sh`).

## Benutzung

```java
// Gelände
TreeSpecies erde = DirtRough.species();
TreeGl boden = new TreeGl(erde);
boden.add(1, 0, 0, 0, 0, 1);                            // Fläche 6 m

// Gegenstände auf dem Boden
TreeSpecies säcke = Sandbag.species();
TreeGl gl = new TreeGl(säcke);
gl.add(2, x, boden(x, z), z, drehung, 1);              // Mauer 2,4 m entlang der gedrehten x-Achse

// Felsen streuen wie Gras
GrassField felsen = Rock.field();                       // 0,04 je m², kleine häufiger
felsen.ground = DirtRough.ground();

// Schützengraben mit allem
Trench t = Trench.standard();                           // oder new Trench(x0, z0, x1, z1, …)
TreeGl graben = new TreeGl(t.species());
graben.add(0, 0, 0, 0, 0, 1);
for (var e : t.scene().entrySet()) {                    // Wellblech, Sandsäcke, Felsen, Gras
    TreeGl teil = new TreeGl(e.getKey());
    for (GrassField.Clump c : e.getValue()) teil.add(c.variant(), c.x(), c.y(), c.z(), c.yaw(), c.scale());
}
```

Drehung wie überall in `com.dan.tree`: x' = x·cos − z·sin, z' = x·sin + z·cos. Ein Stück zeigt seine Vorderseite (örtlich +z) in Richtung (fx, fz) bei `atan2(-fx, fz)`.

## Demos und Selbsttests

Je Art drei Klassen in `<art>.demo`:
- **`…Demo`:** Vorschau auf dem Prozessor. Ohne Fenster: `--bild datei.png [einzel|oben|gruppe|stufen] [variante] …`.
- **`…GlDemo`:** JOGL mit Normal-Maps. Bei Gelände wählt die erste Zahl die Variante, bei Gegenständen ist sie die Anzahl.
- **`…Check`:** Selbsttest. Er endet mit „Alles in Ordnung.“

Gegenstände stehen in den Demos auf „Erde rau“ (Sandsäcke) bzw. „Erde grob“ (Wellblech, Felsen).

## Grenzen

- **Normal-Maps** wirken nur in `gl/` (JOGL). Der Prozessor-Renderer beleuchtet je Punkt, dort zeigt nur die Geometrie das Relief.
- **Gelände ist ein Höhenraster.** Steile Wände (Graben) strecken die von oben projizierte Textur. Im Graben verdeckt das Wellblech das meiste davon, an den Querstücken bleibt es sichtbar.
- **Wellblech:** Die Wellen sind 3 cm tief statt der 12 cm, die der Scan als Höhe angibt. Pfosten oder Halter gibt es nicht, ein Netz trägt nur eine Textur.
- **Sandsäcke:** Sie durchdringen sich beim Stapeln leicht (sie sollen eingedrückt wirken). Am oberen Pol des Sacks läuft das Leinen sternförmig zusammen.
- **Felsen:** Das Original hat nur 428 Dreiecke und keine Farbe. Die Oberfläche ist Gestein aus „Erde rau“, je Fläche projiziert, und hat an den Kanten der Facetten Nähte.
- **Graben:** Die Textur wird beim ersten Gebrauch gebacken (4096 × 2048, etwa 3 s, rund 8 mm je Punkt). Aus der Nähe ist sie gröber als die Kacheln der Erdböden. An inneren Ecken fehlen Wellblech-Stücke, die sonst in den Gang ragen würden. Gras fällt wie in `com.dan.grass` ab 35 Grashöhen weg, aus der Übersicht sieht man es kaum.
- `Assets/Ground` enthielt die Zip „Ground Dirt Rough 02“ doppelt (gleiche Prüfsumme). Die Kopie `(1)` ist gelöscht.
