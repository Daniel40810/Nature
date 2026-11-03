package com.dan.ground.corrugated;

import com.dan.ground.GroundMaterial;
import com.dan.ground.GroundSpecies;
import com.dan.ground.MeshBuilder;
import com.dan.tree.TreeBaker;
import com.dan.tree.TreeSpecies;

/**
 * Wellblech mit „Military Trenches Metal Corrugated 02“ (Megascans, rostig, Kachel 2 × 2 m, 16 Wellen je Kachel).
 * Die Wellen sind echte Geometrie: Ihr Profil wird aus der Höhenkarte gelesen (Mittel über alle Zeilen), so liegen
 * Rippen der Textur und Rippen des Netzes übereinander. Tiefe {@link #DEPTH} statt der 12 cm aus dem Scan, die für
 * Blech viel zu tief wären. Die Bleche sind beidseitig (zwei Lagen, {@link #THICKNESS} auseinander).
 *
 * <p>Varianten: A Platte (0,75 × 2,4 m, liegt leicht verzogen am Boden), B Wandstück (drei Platten nebeneinander,
 * 2,2 m breit, 1,8 m hoch, 10° nach hinten geneigt, Vorderseite nach +z: Verschalung einer Grabenwand),
 * C Bogen (Unterstand aus gebogenen Platten, Halbmesser 1 m, 2,2 m lang, entlang z). Fuß bei y = 0.
 */
public final class Corrugated {

    private Corrugated() {}

    /** Breite einer Platte quer zu den Rippen (6 Wellen) und Länge entlang der Rippen (m) */
    public static final float SHEET_WIDTH = 0.75f, SHEET_LENGTH = 2.4f;
    /** Tiefe der Wellen von Tal zu Kamm (m) und Abstand der beiden Lagen */
    public static final float DEPTH = 0.03f, THICKNESS = 0.004f;
    /** Neigung des Wandstücks nach hinten (Grad) und seine Höhe entlang der Platten */
    public static final float WALL_LEAN = 10, WALL_HEIGHT = 1.8f;
    public static final float ARCH_RADIUS = 1.0f;

    public static final String[] VARIANTS = {"Platte", "Wandstück", "Bogen"};

    private static GroundMaterial material;
    private static float[] profile;

    public static synchronized GroundMaterial material() {
        if (material == null) material = GroundMaterial.load(Corrugated.class, "corrugated");
        return material;
    }

    public static TreeSpecies species() {
        GroundMaterial m = material();
        return GroundSpecies.create("Wellblech", "Stahlblech, verzinkt, verrostet", Corrugated.class, TreeSpecies.Form.OBJECT, m, VARIANTS,
                (v, level) -> build(v, level).toAsset("Wellblech " + VARIANTS[v] + " Stufe " + level,
                        "Corrugated (prozedural)", m.albedoName, m.color));
    }

    /** Profil der Wellen 0..1 über eine Kachel (512 Werte), aus der Höhenkarte. */
    public static synchronized float[] profile() {
        if (profile != null) return profile;
        GroundMaterial m = material();
        int n = 512, rows = 64;
        float[] p = new float[n];
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float s = 0;
            for (int r = 0; r < rows; r++) s += m.height01((i + 0.5f) / n, (r + 0.5f) / rows);
            p[i] = s / rows;
            lo = Math.min(lo, p[i]);
            hi = Math.max(hi, p[i]);
        }
        for (int i = 0; i < n; i++) p[i] = (p[i] - lo) / Math.max(hi - lo, 1e-6f);
        profile = p;
        return p;
    }

    /** Auslenkung der Welle (m, −DEPTH/2 … +DEPTH/2) an u (in Kacheln). */
    public static float wave(float u) {
        float[] p = profile();
        float f = u * p.length - 0.5f;
        int i = (int) Math.floor(f);
        float t = f - i;
        float a = p[Math.floorMod(i, p.length)], b = p[Math.floorMod(i + 1, p.length)];
        return (a + (b - a) * t - 0.5f) * DEPTH;
    }

    /** Punkte je Welle quer zu den Rippen (0: flach, ohne Wellen) */
    static int perWave(int level) {
        return switch (level) {
            case 0 -> 16;
            case 1 -> 8;
            case 2 -> 4;
            default -> 0;
        };
    }

    public static MeshBuilder build(int variant, int level) {
        MeshBuilder mb = new MeshBuilder();
        switch (variant) {
            case 1 -> {
                float lean = (float) Math.toRadians(WALL_LEAN), cl = (float) Math.cos(lean), sl = (float) Math.sin(lean);
                for (int sheetIndex = 0; sheetIndex < 3; sheetIndex++) {
                    int k = sheetIndex;
                    float x0 = (k - 1.5f) * (SHEET_WIDTH - 0.02f), off = k * 0.008f;
                    float u0 = TreeBaker.hash(k, 71) * 2;
                    // s quer (x), t die Platte hinauf, nach hinten (−z) geneigt; Normale nach vorn (+z) und etwas nach oben
                    sheet(mb, level, SHEET_WIDTH, WALL_HEIGHT, u0, Math.max(2, 8 >> level),
                            (s, t, o) -> {
                                o[0] = x0 + s;
                                o[1] = t * cl;
                                o[2] = -t * sl + off + 0.003f * (float) Math.sin(t * 2.1f + k);
                                o[3] = 0;
                                o[4] = sl;
                                o[5] = cl;
                            });
                }
            }
            case 2 -> {
                float r = ARCH_RADIUS, arc = (float) Math.PI * r;
                for (int k = 0; k < 3; k++) {
                    float z0 = (k - 1.5f) * (SHEET_WIDTH - 0.02f), off = k * 0.008f;
                    float u0 = TreeBaker.hash(k, 73) * 2;
                    // s entlang der Achse (z), t um den Bogen
                    sheet(mb, level, SHEET_WIDTH, arc, u0, Math.max(4, 32 >> level), (s, t, o) -> {
                        double th = t / r;
                        float c = (float) Math.cos(th), sn = (float) Math.sin(th);
                        float rr = r + off;
                        o[0] = c * rr;
                        o[1] = sn * rr;
                        o[2] = z0 + s;
                        o[3] = c;
                        o[4] = sn;
                        o[5] = 0;
                    });
                }
            }
            default -> sheet(mb, level, SHEET_WIDTH, SHEET_LENGTH, 0.3f, Math.max(2, 12 >> level), (s, t, o) -> {
                float x = s - SHEET_WIDTH / 2, z = t - SHEET_LENGTH / 2;
                // leicht verzogen: Mitte durchgebogen, eine Ecke angehoben
                o[0] = x;
                o[1] = DEPTH / 2 + THICKNESS + 0.025f * (float) Math.sin(Math.PI * t / SHEET_LENGTH)
                        + 0.04f * Math.max(0, x / SHEET_WIDTH) * Math.max(0, z / SHEET_LENGTH);
                o[2] = z;
                o[3] = 0;
                o[4] = 1;
                o[5] = 0;
            });
        }
        return mb;
    }

    /** Ort (o[0..2]) und Richtung der Welle (o[3..5], Normale der ungewellten Platte) bei s quer, t entlang. */
    @FunctionalInterface
    interface Surface {
        void at(float s, float t, float[] o);
    }

    /**
     * Eine Platte, beidseitig: s in [0, width] quer zu den Rippen (Welle), t in [0, length] entlang; UVs in
     * Kacheln, u = u0 + s / Kachel, v = t / Kachel. Jede Lage liegt um THICKNESS/2 von der Mitte zu ihrer Seite.
     */
    static void sheet(MeshBuilder mb, int level, float width, float length, float u0, int along, Surface f) {
        GroundMaterial m = material();
        int pw = perWave(level);
        int across = pw == 0 ? 2 : Math.max(2, Math.round(width / (m.tileX / 16f) * pw));
        float[] o = new float[6];
        for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
            int side = sideIndex;
            MeshBuilder layer = new MeshBuilder();
            // Zeilen/Spalten getauscht dreht die Fläche um: so zeigen die beiden Lagen in verschiedene Richtungen
            MeshBuilder.GridPoint gp = (c, r, out) -> {
                int ci = side == 0 ? c : r, ri = side == 0 ? r : c;
                float s = width * ci / across, t = length * ri / along;
                float u = u0 + s / m.tileX;
                f.at(s, t, o);
                float w = pw == 0 ? 0 : wave(u);
                out[0] = o[0] + o[3] * w;
                out[1] = o[1] + o[4] * w;
                out[2] = o[2] + o[5] * w;
                out[3] = u;
                out[4] = t / m.tileZ;
            };
            if (side == 0) layer.grid(across, along, gp);
            else layer.grid(along, across, gp);
            // Lage um THICKNESS/2 entlang ihrer eigenen Normalen schieben: die beiden Lagen gehen auseinander
            float[] p = new float[3], n = new float[3];
            for (int i = 0; i < layer.vertices(); i++) {
                layer.position(i, p);
                layer.normal(i, n);
                layer.move(i, p[0] + n[0] * THICKNESS / 2, p[1] + n[1] * THICKNESS / 2, p[2] + n[2] * THICKNESS / 2);
            }
            mb.append(layer, 0, 0, 0, 0);
        }
    }

}
