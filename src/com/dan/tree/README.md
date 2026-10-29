# com.dan.tree – Bäume aus Unreal-Assets

Gemeinsamer Kern für Bäume, die im Procedural Vegetation Editor von Unreal entstanden sind (USD mit Skelett und Nanite-Zweigen). Jede Art liegt in einem eigenen Paket `com.dan.tree.<art>` mit Demo in `com.dan.tree.<art>.demo`. Der Kern braucht nur Java 21. Er liefert Dreiecksnetze in vier Stufen mit allen Werten für Wind und Jahreszeit. `gl/` zeichnet ganze Bestände mit JOGL (OpenGL 4.3, instanziert), und `demo/` enthält einen Renderer auf dem Prozessor, eine Vorschau, die JOGL-Demo und den Selbsttest. Alle Demos sind für jede Art gleich.

Zum Herauskopieren reichen `src/com/dan/tree/` (ohne die Art-Unterordner) und der Ordner der Art, etwa `src/com/dan/tree/blackalder/`. Die Demos brauchen zusätzlich `FStyle.jar`, `gl/` braucht JOGL.

Arten bisher:

| Paket | Art | Varianten |
|---|---|---|
| `com.dan.tree.blackalder` | Schwarzerle (*Alnus glutinosa*) | A breit 16 m, B jung 11 m, C schlank 15 m, D groß 18 m |

## Weg eines Baums

```
Unreal (PVE) ─ USD + DynamicWind.json ─▶ tools/tree/usd2dtree.py ─▶ <art>/*.dtree ─▶ TreeAsset ─▶ TreeBaker ─▶ TreeMesh ─▶ SoftRenderer / gl/TreeGl
```

1. **Umwandeln, einmal:** `python3 tools/tree/usd2dtree.py Tree_X_01_A.usd src/com/dan/tree/<art>/x_a.dtree --name "X A"` (braucht `pip install usd-core scipy numpy`). Die Ast-Bibliothek (`*_Branches.usd`) und die `*_DynamicWind.json` müssen im selben Ordner liegen. Das Skript schreibt:
   - das Skelett (Eltern, Lage und Radius je Gelenk),
   - Stamm und Äste im Original (je Punkt zwei Gelenke),
   - die Zweig-Prototypen: Holz als Knotengraph im 3-cm-Raster, Blätter als Sechseck aus Stiel, Mitte, Normale und Rand,
   - die Zweig-Instanzen (Matrix und zwei Gelenke),
   - Grundfarben und Böendämpfung aus Unreal.

   Eine Variante der Schwarzerle braucht 0,4 bis 1,6 MB statt 4 bis 14 MB USD plus 144 MB Ast-Bibliothek.
2. **Laden:** `TreeAsset.load(Owner.class, "x_a.dtree")`, nur Daten.
3. **Backen:** `TreeBaker.bake(asset, TreeLod.of(stufe))` liefert ein `TreeMesh`: 13 Werte je Punkt, nämlich Lage, Normale, Ausschlag von Hauptast und Zweig, Zittern, zwei Phasen, Art und Zufall mit Verdeckung.
4. **Zeichnen:** Wind und Jahreszeit rechnen `TreeWind.apply` bzw. `TreeMesh.color` auf dem Prozessor und `gl/tree.vert` auf der Grafikkarte, beide mit derselben Formel. Für einen anderen Tag muss nichts neu gebacken werden.

## Stufen

| Stufe | Stamm und Äste | Zweigholz | Laub | Schwarzerle A / C |
|---|---|---|---|---|
| 0 | Originalnetz aus Unreal | Röhren mit 3 Seiten, Ketten bis 15° Knick zusammengefasst | jedes Blatt als Sechseck | 2,8 Mio. / 320 000 Dreiecke |
| 1 | Röhren mit 5 Seiten ab 6 mm Radius | Röhren ab 3 mm | 35 % der Blätter als Raute, größer | 177 000 / 20 000 |
| 2 | Röhren mit 4 Seiten ab 2 cm | – | 10 % als Raute (wie die Büschel in `com.dan.forest`) | 50 000 / 5 700 |
| 3 | Stamm und Hauptäste ab 3 cm, 3 Seiten | – | Hülle der Krone, 8 × 5 | 344 / 272 |

Wenn Blätter wegfallen, werden die übrigen um 1/√Anteil größer, damit die Laubfläche gleich bleibt. Ohne Laub zeigt auch Stufe 3 das Gerippe aus Stamm und Hauptästen. Alle Felder von `TreeLod` sind öffentlich, eigene Stufen sind möglich. `TreeGl` wählt die Stufe nach der Entfernung in Baumhöhen (`lodDistance`, voreingestellt 1,4 / 3,5 / 9).

## Wind

`TreeSkeleton` wertet das Skelett aus. An jeder Gabel setzt das Kind den Ast fort, das am besten in die bisherige Richtung zeigt (dickere Kinder werden bevorzugt). Die übrigen Kinder beginnen einen Ast der nächsten Ordnung: 0 Stamm, 1 Hauptast, 2 und mehr Zweige. Je Punkt entstehen daraus:

- **Stamm:** Er neigt sich mit h² in den Wind und pendelt leicht (0,22 Hz, Phase je Ort).
- **Hauptast:** Ausschlag (Weg im Hauptast / Reichweite)² · Reichweite, mit dem Wind und quer dazu, eine Phase je Hauptast. Die Reichweite schließt die Zweige ein, die an ihm hängen, damit der Übergang zum Zweig ohne Sprung bleibt.
- **Zweig:** Er wippt in eine eigene Richtung. Alle Zweige eines Asts haben dessen Phase, sonst würden sie an ihrem Ansatz reißen.
- **Blatt:** Es zittert entlang der Normale, von 0 am Stiel bis 1 an der Spitze.

Die Stärke wächst mit (U/20 m/s)², höchstens 2. Böen laufen als Welle mit dem Wind über das Land und werden mit `gustAttenuation` aus Unreal gedämpft. `TreeSpecies.flexTrunk`, `flexBranch` und `flutter` stellen die Biegsamkeit je Art ein. Wind von außen kommt über `TreeWind.external` (Schnittstelle `Wind`, {Ost, oben, Süd} in m/s), etwa aus `com.dan.wind`.

## Jahreszeit

`TreeSeason` (Felder wie in `com.dan.forest.Species`: `leafOut`, `colorStart`, `colorFull`, `bare`, `spring`, `summer`, `autumn`) liefert je Tag einen `Look`. Sichtbar sind die Blätter mit `lo ≤ Zufall < hi`: Beim Austrieb erscheinen zuerst die mit kleinem Zufall, im Herbst färben sich diese zuerst und fallen zuerst. Schnee legt sich auf nach oben weisende Flächen.

## Benutzung

```java
TreeSpecies erle = BlackAlder.species();
TreeMesh m = erle.bake(0, 1);                         // Variante A, Stufe 1

// Prozessor:
TreeWind wind = new TreeWind();
wind.speed = 8; wind.fromDeg = 250; wind.gustiness = 0.5f;
float[] pos = new float[m.vertices * 3];
wind.displace(m, erle, erle.load(0).gustAttenuation, t, x, y, z, drehung, pos);
TreeSeason.Look look = erle.season.look(tag, schnee);
m.color(i, erle, erle.load(0), look, rgb);             // false: Blatt fehlt

// JOGL:
TreeGl gl = new TreeGl(erle);
gl.add(variante, x, y, z, drehung, maßstab);
gl.init(gl4);
// je Bild:
gl.setSeason(tag, schnee);
gl.setWind(wind);
gl.update(gl4, auge);                                 // Stufen wählen, fertig gebackene Netze hochladen
gl.draw(gl4, view, light, t);
```

## Aufbau

| Klasse | Aufgabe |
|---|---|
| `TreeAsset` | Daten aus `.dtree`: Skelett, Stamm, Zweig-Prototypen, Instanzen |
| `TreeSkeleton` | Äste, Ordnung, Hauptast, Weg und Reichweite je Gelenk |
| `TreeLod` | Einstellungen einer Stufe, `of(0..3)` |
| `TreeBaker` | Netz backen: Original oder Röhren, Zweigholz, Blätter, Hülle, Verdeckung; `chains` fasst gerade Ketten zusammen |
| `TreeMesh` | Ergebnis, 13 Werte je Punkt; `color` für Art und Jahreszeit |
| `TreeWind`, `Wind` | Wind, Böen, Formel je Punkt (`apply`), Wind von außen |
| `TreeSeason` | Austrieb, Verfärbung, Laubfall |
| `TreeSpecies` | Art: Varianten als Ressourcen, Farben, Biegsamkeit, Jahreszeit; lädt einmal und backt |
| `gl/TreeGl`, `gl/tree.vert`, `gl/tree.frag` | Bestand mit JOGL: Netze im Hintergrund backen, Eimer je Variante und Stufe, ein Aufruf je Eimer |
| `demo/SoftRenderer` | Renderer auf dem Prozessor für Vorschau und Bilder |
| `demo/TreeDemo` | Vorschau einer Art: Einzelbaum, Hain, Stufen nebeneinander; `--bild` ohne Fenster |
| `demo/TreeGlDemo` | Bestand mit JOGL |
| `demo/TreeCheck` | Selbsttest einer Art |

## Neue Art hinzufügen

1. In Unreal die Varianten als USD exportieren, dazu die Ast-Bibliothek und die DynamicWind-JSON.
2. `usd2dtree.py` je Variante aufrufen und die Dateien nach `src/com/dan/tree/<art>/` legen.
3. `<Art>.java` mit `species()` anlegen (Vorlage: `blackalder/BlackAlder.java`): Ressourcen, Namen, Rinde, Biegsamkeit, Jahreszeit.
4. In `<art>/demo/` drei Einzeiler anlegen: `…Demo` (`TreeDemo.run`), `…GlDemo` (`TreeGlDemo.run`) und `…Check` (`TreeCheck.run` mit der erwarteten Höhe).

## Grenzen

- Der Wind ist ein Modell für kleine Winkel. Punkte werden verschoben, nicht um Gelenke gedreht. Bei Sturm dehnen sich deshalb einzelne Kanten (unter 0,2 %, geprüft).
- Unreal liefert keine Texturen mit (nur Grundfarben). Rinde und Laub sind einfarbig mit Zufall je Blatt und Verdeckung.
- Die Hülle in Stufe 3 ist nur für große Entfernung gedacht. Aus der Nähe wirkt sie wie ein glatter Körper.
- In `gl/` gibt es noch keine Schattenkarte. Der Prozessor-Renderer zeichnet unter jeder Krone einen weichen Fleck.
- Stufe 0 der großen Varianten (A 2,8 Mio., D 1,7 Mio. Dreiecke) ist für Nahansichten gedacht und braucht etwa 3 s zum Backen. `TreeGl` backt im Hintergrund und zeigt bis dahin die nächstgröbere fertige Stufe.
- Der Konverter liest Y-oben-USD mit einem PointInstancer für die Zweige, so wie ihn der Procedural Vegetation Editor schreibt. FBX (etwa `trees_60_fbx.zip`) wird noch nicht gelesen.
