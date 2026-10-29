package com.dan.tree;

/**
 * Ein gebackener Baum in einer Stufe: Punkte mit allen Werten in einem Feld ({@link #STRIDE} je Punkt)
 * und Dreiecke. Im Baumraum: Meter, y oben, Fuß bei (0, 0, 0). Die Bewegung im Wind rechnet
 * {@link TreeWind} (Prozessor) bzw. der Vertex-Shader in {@code gl/} aus denselben Werten.
 */
public final class TreeMesh {

    /** Werte je Punkt */
    public static final int STRIDE = 13;
    public static final int POS = 0, NRM = 3;
    /** Weg im Hauptast² / Reichweite (m): wie stark der Punkt mit dem Hauptast schwingt */
    public static final int LIMB = 6;
    /** dasselbe für den Zweig */
    public static final int TWIG = 7;
    /** 0 Holz, 0..1 Blatt (Spitze 1) */
    public static final int FLUTTER = 8;
    /** Phase des Hauptasts und des Zweigs, 0..1 */
    public static final int PHASE_LIMB = 9, PHASE_TWIG = 10;
    /** 0 Holz, 1 Laub; Zufall je Blatt 0..1 in {@link #RAND} geht über die Jahreszeit ({@link TreeSeason.Look}) */
    public static final int KIND = 11;
    /** Zufall je Blatt (Holz: Schattierung), Ganzzahl-Teil: Verdeckung in Hundertsteln (siehe {@link #ao}) */
    public static final int RAND = 12;

    public final float[] v;
    public final int[] tri;
    public final int vertices, triangles;
    /** Dreiecke 0..barkTriangles-1 sind Holz, danach Laub */
    public final int barkTriangles;
    public final int leaves;
    public final float height, radius;
    public final TreeLod lod;

    TreeMesh(float[] v, int vertices, int[] tri, int triangles, int barkTriangles, int leaves,
             float height, float radius, TreeLod lod) {
        this.v = v;
        this.vertices = vertices;
        this.tri = tri;
        this.triangles = triangles;
        this.barkTriangles = barkTriangles;
        this.leaves = leaves;
        this.height = height;
        this.radius = radius;
        this.lod = lod;
    }

    /** Zufall je Blatt 0..1 des Punkts i. */
    public float rand(int i) {
        float r = v[i * STRIDE + RAND];
        return r - (float) Math.floor(r);
    }

    /** Verdeckung 0 (innen, dunkel) .. 1 (außen) des Punkts i. */
    public float ao(int i) {
        return (float) Math.floor(v[i * STRIDE + RAND]) / 100f;
    }

    public boolean leaf(int i) {
        return v[i * STRIDE + KIND] > 0.5f;
    }

    /**
     * Farbe des Punkts i, linear, für Art und Jahreszeit; false, wenn das Blatt nicht da ist.
     * Der Shader in {@code gl/tree.vert} rechnet dasselbe.
     */
    public boolean color(int i, TreeSpecies sp, TreeAsset a, TreeSeason.Look look, float[] out) {
        int b = i * STRIDE;
        float rand = rand(i), ao = ao(i);
        float ny = v[b + NRM + 1];
        if (leaf(i)) {
            if (!look.visible(rand)) return false;
            look.color(rand, out);
            float jitter = 0.85f + 0.3f * fract(rand * 7.31f);
            for (int k = 0; k < 3; k++) out[k] *= jitter * ao;
        } else {
            float[] bark = sp != null && sp.bark != null ? sp.bark : a.barkColor;
            float jitter = 0.85f + 0.3f * rand;
            for (int k = 0; k < 3; k++) out[k] = bark[k] * jitter * (0.55f + 0.45f * ao);
        }
        if (look.snow > 0) {
            float s = look.snow * smooth(0.25f, 0.8f, ny) * (leaf(i) ? 0.6f : 1f);
            for (int k = 0; k < 3; k++) out[k] = out[k] + (0.85f - out[k]) * s;
        }
        return true;
    }

    static float fract(float x) {
        return x - (float) Math.floor(x);
    }

    static float smooth(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    @Override
    public String toString() {
        return lod + ": " + vertices + " Punkte, " + triangles + " Dreiecke (" + barkTriangles + " Holz), " + leaves + " Blätter";
    }
}
