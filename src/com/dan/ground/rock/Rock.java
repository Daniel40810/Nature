package com.dan.ground.rock;

import com.dan.grass.GrassField;
import com.dan.ground.GroundMaterial;
import com.dan.ground.GroundPatch;
import com.dan.ground.GroundSpecies;
import com.dan.ground.MeshBuilder;
import com.dan.tree.TreeAsset;
import com.dan.tree.TreeSpecies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Felsen aus dem „3D Lowpoly Rock“ (glTF, 428 Dreiecke, ohne brauchbare Textur). Das Netz wird verschweißt, je
 * Variante gedreht, gestreckt und mit Rauschen verformt; die feinen Stufen sind unterteilt (Stufe 0: zweimal,
 * Stufe 1: einmal, Stufe 2: das Original, Stufe 3: vereinfacht). Flächen bleiben facettiert wie im Original.
 * Als Oberfläche dient Gestein aus „Erde rau“ (entsättigt, heller), je Fläche von der Seite projiziert, aus der
 * sie am meisten zu sehen ist.
 *
 * <p>Varianten: A Brocken (1,2 m, liegend), B Platte (1,6 m, flach), C Spitze (1,3 m hoch, stehend),
 * D Stein (0,4 m). Fuß bei y = 0, etwa ein Zehntel der Höhe steckt im Boden.
 */
public final class Rock {

    private Rock() {}

    public static final String[] VARIANTS = {"Brocken", "Platte", "Spitze", "Stein"};

    private static GroundMaterial material;
    private static float[] basePos;
    private static int[] baseTri;

    public static synchronized GroundMaterial material() {
        if (material == null) material = GroundMaterial.load(Rock.class, "rock");
        return material;
    }

    public static TreeSpecies species() {
        GroundMaterial m = material();
        TreeSpecies s = GroundSpecies.create("Fels", "Kalkstein", Rock.class, TreeSpecies.Form.OBJECT, m, VARIANTS,
                (v, level) -> build(v, level).toAsset("Fels " + VARIANTS[v] + " Stufe " + level, "rock_lowpoly.dtree",
                        m.albedoName, m.color));
        return s;
    }

    /** Felsen gestreut: etwa einer je 25 m², kleine Steine häufiger als große Brocken. */
    public static GrassField field() {
        GrassField f = new GrassField(0.04f, 31);
        f.scaleMin = 0.6f;
        f.scaleMax = 1.5f;
        f.variantWeight = new float[]{1, 0.7f, 0.4f, 2.5f};
        return f;
    }

    // ------------------------------------------------------------------ Netz

    /** Das Original verschweißt (gleiche Orte = ein Punkt), Mitte auf der Achse, größte Ausdehnung 1. */
    private static synchronized void base() {
        if (basePos != null) return;
        TreeAsset a = TreeAsset.load(Rock.class, "rock_lowpoly.dtree");
        float[] p = a.trunk.pos;
        Map<String, Integer> ids = new HashMap<>();
        int[] map = new int[p.length / 3];
        List<float[]> pts = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            String key = Math.round(p[i * 3] * 1e4) + "," + Math.round(p[i * 3 + 1] * 1e4) + "," + Math.round(p[i * 3 + 2] * 1e4);
            Integer id = ids.get(key);
            if (id == null) {
                ids.put(key, id = pts.size());
                pts.add(new float[]{p[i * 3], p[i * 3 + 1], p[i * 3 + 2]});
            }
            map[i] = id;
        }
        float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] q : pts)
            for (int k = 0; k < 3; k++) {
                lo[k] = Math.min(lo[k], q[k]);
                hi[k] = Math.max(hi[k], q[k]);
            }
        float ext = Math.max(hi[0] - lo[0], Math.max(hi[1] - lo[1], hi[2] - lo[2]));
        basePos = new float[pts.size() * 3];
        for (int i = 0; i < pts.size(); i++)
            for (int k = 0; k < 3; k++) basePos[i * 3 + k] = (pts.get(i)[k] - (lo[k] + hi[k]) / 2) / ext;
        int[] t = a.trunk.tri;
        List<Integer> tri = new ArrayList<>();
        for (int i = 0; i < t.length; i += 3) {
            int x = map[t[i]], y = map[t[i + 1]], z = map[t[i + 2]];
            if (x != y && y != z && x != z) {
                tri.add(x);
                tri.add(y);
                tri.add(z);
            }
        }
        baseTri = tri.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Form je Variante: Maßstab (x, y, z) nach dem Drehen, liegend (lange Achse waagerecht), Rauschen, Startwert. */
    private static float[] shape(int v) {
        return switch (v) {
            case 1 -> new float[]{1.6f, 0.42f, 1.15f, 1, 0.05f, 11};
            case 2 -> new float[]{0.75f, 1.3f, 0.7f, 0, 0.05f, 12};
            case 3 -> new float[]{0.4f, 0.28f, 0.34f, 1, 0.07f, 13};
            default -> new float[]{1.2f, 0.8f, 0.95f, 1, 0.045f, 10};
        };
    }

    public static MeshBuilder build(int variant, int level) {
        base();
        float[] sh = shape(variant);
        boolean lying = sh[3] > 0.5f;
        // drehen (liegend: lange Achse y nach x), strecken
        float[] pos = new float[basePos.length];
        for (int i = 0; i < basePos.length; i += 3) {
            float x = basePos[i], y = basePos[i + 1], z = basePos[i + 2];
            if (lying) {
                float t = x;
                x = y;
                y = -t;
            }
            pos[i] = x;
            pos[i + 1] = y;
            pos[i + 2] = z;
        }
        float[] lo = new float[3], hi = new float[3];
        bounds(pos, lo, hi);
        for (int i = 0; i < pos.length; i += 3)
            for (int k = 0; k < 3; k++) pos[i + k] = pos[i + k] / Math.max(hi[k] - lo[k], 1e-6f) * sh[k];
        int[] tri = baseTri.clone();
        // Unterteilen (Stufe 0 zweimal, Stufe 1 einmal)
        float[][] pr = {pos};
        int[][] tr = {tri};
        for (int s = 0; s < 2 - Math.min(level, 2); s++) subdivide(pr, tr);
        pos = pr[0];
        tri = tr[0];
        // Rauschen nach außen, auf allen Stufen gleich (Funktion des Orts)
        float size = Math.max(sh[0], Math.max(sh[1], sh[2]));
        int seed = (int) sh[5];
        float amp = sh[4] * size, freq = 3.5f / size;
        for (int i = 0; i < pos.length; i += 3) {
            float x = pos[i], y = pos[i + 1], z = pos[i + 2];
            float l = (float) Math.sqrt(x * x + y * y + z * z) + 1e-6f;
            float n = noise3(seed, x * freq, y * freq, z * freq) + 0.4f * noise3(seed + 5, x * freq * 2.7f, y * freq * 2.7f, z * freq * 2.7f);
            pos[i] += x / l * n * amp;
            pos[i + 1] += y / l * n * amp;
            pos[i + 2] += z / l * n * amp;
        }
        if (level >= 3) {
            int[][] t2 = {tri};
            pos = cluster(pos, t2, size / 5);
            tri = t2[0];
        }
        // Fuß: ein Zehntel der Höhe steckt im Boden
        bounds(pos, lo, hi);
        float sink = lo[1] + (hi[1] - lo[1]) * 0.1f;
        for (int i = 1; i < pos.length; i += 3) pos[i] -= sink;
        return facets(pos, tri, material());
    }

    /** Facettiert: je Dreieck eigene Punkte mit der Flächennormalen; UVs von der Seite, zu der die Fläche zeigt. */
    static MeshBuilder facets(float[] pos, int[] tri, GroundMaterial m) {
        MeshBuilder mb = new MeshBuilder();
        for (int t = 0; t < tri.length; t += 3) {
            int a = tri[t] * 3, b = tri[t + 1] * 3, c = tri[t + 2] * 3;
            float ux = pos[b] - pos[a], uy = pos[b + 1] - pos[a + 1], uz = pos[b + 2] - pos[a + 2];
            float vx = pos[c] - pos[a], vy = pos[c + 1] - pos[a + 1], vz = pos[c + 2] - pos[a + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-12f) continue;
            nx /= l;
            ny /= l;
            nz /= l;
            int axis = Math.abs(nx) > Math.abs(ny) ? (Math.abs(nx) > Math.abs(nz) ? 0 : 2) : (Math.abs(ny) > Math.abs(nz) ? 1 : 2);
            int[] ids = new int[3];
            int k = 0;
            for (int q : new int[]{a, b, c}) {
                float x = pos[q], y = pos[q + 1], z = pos[q + 2];
                float u, w;
                switch (axis) {
                    case 0 -> { u = z * Math.signum(nx); w = -y; }
                    case 1 -> { u = x; w = z * Math.signum(ny); }
                    default -> { u = -x * Math.signum(nz); w = -y; }
                }
                ids[k++] = mb.vertex(x, y, z, nx, ny, nz, u / m.tileX, w / m.tileZ);
            }
            mb.triangle(ids[0], ids[1], ids[2]);
        }
        return mb;
    }

    /** Jedes Dreieck in vier (Kantenmitten geteilt, gemeinsame Kanten gemeinsam). */
    static void subdivide(float[][] pr, int[][] tr) {
        float[] pos = pr[0];
        int[] tri = tr[0];
        Map<Long, Integer> mid = new HashMap<>();
        float[] np = Arrays.copyOf(pos, pos.length + tri.length * 3);
        int[] nt = new int[tri.length * 4];
        int nv = pos.length / 3, o = 0;
        for (int t = 0; t < tri.length; t += 3) {
            int[] c = {tri[t], tri[t + 1], tri[t + 2]};
            int[] m = new int[3];
            for (int e = 0; e < 3; e++) {
                int i = c[e], j = c[(e + 1) % 3];
                long key = (long) Math.min(i, j) << 32 | Math.max(i, j);
                Integer id = mid.get(key);
                if (id == null) {
                    id = nv++;
                    for (int k = 0; k < 3; k++) np[id * 3 + k] = (pos[i * 3 + k] + pos[j * 3 + k]) / 2;
                    mid.put(key, id);
                }
                m[e] = id;
            }
            int[] add = {c[0], m[0], m[2], m[0], c[1], m[1], m[2], m[1], c[2], m[0], m[1], m[2]};
            System.arraycopy(add, 0, nt, o, 12);
            o += 12;
        }
        pr[0] = Arrays.copyOf(np, nv * 3);
        tr[0] = nt;
    }

    /** Vereinfachen: Punkte in Würfeln der Kante cell zusammenfassen (der Punkt am weitesten außen bleibt). */
    static float[] cluster(float[] pos, int[][] tr, float cell) {
        Map<Long, Integer> ids = new HashMap<>();
        int n = pos.length / 3;
        int[] map = new int[n];
        List<Integer> keep = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long key = ((long) Math.floor(pos[i * 3] / cell) & 0xFFFFF) << 40 | ((long) Math.floor(pos[i * 3 + 1] / cell) & 0xFFFFF) << 20
                    | ((long) Math.floor(pos[i * 3 + 2] / cell) & 0xFFFFF);
            Integer id = ids.get(key);
            if (id == null) {
                ids.put(key, id = keep.size());
                keep.add(i);
            } else {
                int r = keep.get(id);
                if (len(pos, i) > len(pos, r)) keep.set(id, i);
            }
            map[i] = id;
        }
        float[] out = new float[keep.size() * 3];
        for (int c = 0; c < keep.size(); c++) System.arraycopy(pos, keep.get(c) * 3, out, c * 3, 3);
        int[] tri = tr[0];
        List<Integer> nt = new ArrayList<>();
        for (int t = 0; t < tri.length; t += 3) {
            int a = map[tri[t]], b = map[tri[t + 1]], c = map[tri[t + 2]];
            if (a != b && b != c && a != c) {
                nt.add(a);
                nt.add(b);
                nt.add(c);
            }
        }
        tr[0] = nt.stream().mapToInt(Integer::intValue).toArray();
        return out;
    }

    private static float len(float[] p, int i) {
        return p[i * 3] * p[i * 3] + p[i * 3 + 1] * p[i * 3 + 1] + p[i * 3 + 2] * p[i * 3 + 2];
    }

    private static void bounds(float[] p, float[] lo, float[] hi) {
        Arrays.fill(lo, Float.MAX_VALUE);
        Arrays.fill(hi, -Float.MAX_VALUE);
        for (int i = 0; i < p.length; i += 3)
            for (int k = 0; k < 3; k++) {
                lo[k] = Math.min(lo[k], p[i + k]);
                hi[k] = Math.max(hi[k], p[i + k]);
            }
    }

    /** Rauschen im Raum aus drei ebenen Rauschen, etwa −1..1. */
    static float noise3(int seed, float x, float y, float z) {
        return (GroundPatch.noise(seed, x, y) + GroundPatch.noise(seed + 1, y, z) + GroundPatch.noise(seed + 2, z, x)) / 1.8f;
    }
}
