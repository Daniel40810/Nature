package com.dan.ground.sandbag;

import com.dan.ground.GroundMaterial;
import com.dan.ground.GroundPatch;
import com.dan.ground.GroundSpecies;
import com.dan.ground.MeshBuilder;
import com.dan.tree.TreeBaker;
import com.dan.tree.TreeSpecies;

/**
 * Sandsäcke mit dem Leinen „Military Trenches Detail Sandbag Canvas“ (Megascans, Kachel 1 × 1 m). Die Form entsteht
 * im Code: ein gefüllter Sack ist ein Superquader (kastenförmig im Grundriss, gerundet im Schnitt), unten flach
 * aufgesetzt, oben zu den Enden hin eingesunken und leicht beulig. Jeder Sack ist etwas anders (Größe, Beulen,
 * Drehung), aus einem Hash, also reproduzierbar.
 *
 * <p>Varianten: A ein Sack (60 × 32 × 13 cm), B Reihe 2,4 m (vier Säcke längs), C Mauer 2,4 m (vier Lagen,
 * unten zwei Reihen tief, versetzt, etwa 0,45 m hoch; Brustwehr am Graben), D Stellung (Ring mit 1,4 m Halbmesser,
 * zwei Lagen, vorne offen). Der Fuß liegt bei y = 0, die Mauer läuft entlang x.
 */
public final class Sandbag {

    private Sandbag() {}

    public static final float LENGTH = 0.60f, WIDTH = 0.32f, HEIGHT = 0.13f;
    /** Ein Sack trägt den nächsten: so viel seiner Höhe zählt beim Stapeln (die Säcke drücken sich ineinander) */
    public static final float STACK = 0.85f;

    public static final String[] VARIANTS = {"Sack", "Reihe 2,4 m", "Mauer 2,4 m", "Stellung"};

    private static GroundMaterial material;

    public static synchronized GroundMaterial material() {
        if (material == null) material = GroundMaterial.load(Sandbag.class, "sandbag");
        return material;
    }

    public static TreeSpecies species() {
        GroundMaterial m = material();
        return GroundSpecies.create("Sandsack", "Leinen, mit Erde gefüllt", Sandbag.class, TreeSpecies.Form.OBJECT, m, VARIANTS,
                (v, level) -> build(v, level).toAsset("Sandsack " + VARIANTS[v] + " Stufe " + level,
                        "Sandbag (prozedural)", m.albedoName, m.color));
    }

    /** Netz einer Variante in einer Stufe. */
    public static MeshBuilder build(int variant, int level) {
        MeshBuilder mb = new MeshBuilder();
        int id = 0;
        switch (variant) {
            case 1 -> {
                for (int i = 0; i < 4; i++) place(mb, id++, level, (i - 1.5f) * LENGTH * 0.98f, 0, 0, 0);
            }
            case 2 -> {
                for (int course = 0; course < 4; course++) {
                    float y = course * HEIGHT * STACK;
                    float shift = (course & 1) == 1 ? LENGTH * 0.5f : 0;
                    float[] rows = course < 2 ? new float[]{-WIDTH * 0.5f, WIDTH * 0.5f} : new float[]{0};
                    for (float z : rows) {
                        int n = shift > 0 ? 3 : 4;
                        for (int i = 0; i < n; i++) place(mb, id++, level, (i - (n - 1) / 2f) * LENGTH * 0.98f, y, z, 0);
                    }
                }
            }
            case 3 -> {
                float r = 1.4f;
                for (int course = 0; course < 2; course++) {
                    float y = course * HEIGHT * STACK;
                    float step = LENGTH * 0.97f / r;
                    float start = (float) Math.toRadians(35) + (course == 1 ? step / 2 : 0);
                    for (float a = start; a < 2 * Math.PI - Math.toRadians(35) - step * 0.5f; a += step) {
                        // Sack tangential: Längsachse quer zum Halbmesser
                        float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
                        place(mb, id++, level, x, y, z, (float) (a + Math.PI / 2));
                    }
                }
            }
            default -> bag(mb, 0, level, 0, 0, 0, 0);
        }
        return mb;
    }

    /** Ein Sack mit kleiner Abweichung in Lage und Drehung. */
    private static void place(MeshBuilder mb, int id, int level, float x, float y, float z, float yaw) {
        float dx = (TreeBaker.hash(id, 51) - 0.5f) * 0.03f, dz = (TreeBaker.hash(id, 52) - 0.5f) * 0.03f;
        float dyaw = (TreeBaker.hash(id, 53) - 0.5f) * 0.12f;
        bag(mb, id, level, x + dx, y, z + dz, yaw + dyaw);
    }

    /** Teilung je Stufe: Umfang (θ) und Schnitt (φ). */
    static int[] segments(int level) {
        return switch (level) {
            case 0 -> new int[]{48, 24};
            case 1 -> new int[]{24, 12};
            case 2 -> new int[]{12, 8};
            default -> new int[]{8, 4};
        };
    }

    /**
     * Ein Sack: Superquader mit Halbachsen a (x), b (y), c (z), Exponent e1 im Schnitt, e2 im Grundriss; unten
     * flach, oben zu den Enden eingesunken, beulig. UVs in Metern Leinen (Kachel 1 m).
     */
    static void bag(MeshBuilder out, int id, int level, float px, float py, float pz, float yaw) {
        float a = LENGTH / 2 * (0.94f + 0.12f * TreeBaker.hash(id, 61));
        float c = WIDTH / 2 * (0.94f + 0.12f * TreeBaker.hash(id, 62));
        float b = HEIGHT / 2 * 1.25f * (0.92f + 0.16f * TreeBaker.hash(id, 63));
        float e1 = 0.55f, e2 = 0.3f;
        int[] seg = segments(level);
        int cols = seg[0], rows = seg[1];
        float floor = -b * 0.75f;
        int seed = 1000 + id * 7;
        float lump = level >= 3 ? 0 : 0.006f;
        float uOff = TreeBaker.hash(id, 64), vOff = TreeBaker.hash(id, 65);
        float perimeter = 2 * (2 * a + 2 * c), section = 2 * b + 2 * c;
        MeshBuilder mb = new MeshBuilder();
        mb.grid(cols, rows, (col, row, o) -> {
            double th = -Math.PI + 2 * Math.PI * col / cols;
            double ph = -Math.PI / 2 + Math.PI * row / rows;
            float cp = (float) Math.cos(ph), sp = (float) Math.sin(ph), ct = (float) Math.cos(th), st = (float) Math.sin(th);
            float x = a * sgnPow(cp, e1) * sgnPow(ct, e2);
            float y = b * sgnPow(sp, e1);
            float z = c * sgnPow(cp, e1) * sgnPow(st, e2);
            // unten flach aufgesetzt, oben zu den Enden eingesunken
            float t = x / a;
            if (y > 0) y *= 1 - 0.35f * t * t * t * t;
            y = Math.max(y, floor);
            // Beulen (nicht am Boden, und gleich an der Naht θ = ±π)
            if (y > floor + 0.005f) {
                float n = GroundPatch.noise(seed, x * 9, z * 9 + y * 5);
                float f = lump * n;
                x += f * Math.signum(x) * Math.abs(ct);
                z += f * Math.signum(z) * Math.abs(st);
                y += f * 1.5f * Math.max(0, sp);
            }
            o[0] = x;
            o[1] = y - floor;
            o[2] = z;
            o[3] = uOff + (float) (col / (double) cols) * perimeter;
            o[4] = vOff + (float) (row / (double) rows) * section;
        });
        out.append(mb, yaw, px, py, pz);
    }

    static float sgnPow(float v, float e) {
        return Math.signum(v) * (float) Math.pow(Math.abs(v), e);
    }
}
