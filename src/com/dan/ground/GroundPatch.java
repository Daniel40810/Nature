package com.dan.ground;

import com.dan.grass.GrassField;
import com.dan.tree.TreeAsset;
import com.dan.tree.TreeBaker;

/**
 * Ein Stück Boden als Netz: Raster über {@link #sizeX} × {@link #sizeZ} m um ({@link #centerX}, {@link #centerZ}),
 * Höhe = Form des Geländes ({@link #shape}) + Relief des Materials ({@link GroundMaterial#displacement}).
 * Stufe 0 hat Zellen von {@link #cell} m, jede weitere Stufe doppelt so große; in Stufe 1 wird das Relief über die
 * Zelle gemittelt, damit es nicht flimmert. Die UVs kacheln das Material oder spannen ein gebackenes Bild einmal
 * über die ganze Fläche ({@link #tiled}).
 */
public final class GroundPatch {

    public float sizeX = 4, sizeZ = 4;
    public float centerX, centerZ;
    /** Zellgröße in Stufe 0 (m) */
    public float cell = 0.02f;
    /** Form des Geländes ohne Relief (Hügel, Mulde, Graben) */
    public GrassField.Ground shape = GrassField.Ground.FLAT;
    /** Relief und Kachelung; null = glatt */
    public GroundMaterial material;
    /** Relief mal (1 = wie gescannt) */
    public float reliefScale = 1;
    /** true: UVs kacheln das Material; false: 0..1 einmal über die Fläche (für {@link GroundBlend}) */
    public boolean tiled = true;

    public GroundPatch() {}

    public GroundPatch(GroundMaterial material, float sizeX, float sizeZ, float cell) {
        this.material = material;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.cell = cell;
    }

    /** Höhe des Bodens am Ort (Form und Relief), etwa um Gras oder Steine darauf zu setzen. */
    public float height(float x, float z) {
        float y = shape.height(x, z);
        if (material != null) y += material.displacement(x, z) * reliefScale;
        return y;
    }

    /** Der Boden als {@link GrassField.Ground}. */
    public GrassField.Ground ground() {
        return this::height;
    }

    /** Zellen in x und z für die Stufe. */
    public int[] cells(int level) {
        float c = cell * (1 << level);
        return new int[]{Math.max(2, Math.round(sizeX / c)), Math.max(2, Math.round(sizeZ / c))};
    }

    /** Netz der Stufe (0 fein … 3 grob). */
    public MeshBuilder build(int level) {
        int[] n = cells(level);
        int cols = n[0], rows = n[1];
        float x0 = centerX - sizeX / 2, z0 = centerZ - sizeZ / 2, dx = sizeX / cols, dz = sizeZ / rows;
        // Relief bis Stufe 1 über die Zelle mitteln (weniger Flimmern), danach am Gitterpunkt (Spitzen bleiben, die Höhe stimmt)
        int sub = level == 1 ? 2 : 1;
        MeshBuilder mb = new MeshBuilder();
        mb.grid(cols, rows, (c, r, out) -> {
            float x = x0 + c * dx, z = z0 + r * dz;
            float y = shape.height(x, z);
            if (material != null && reliefScale != 0) {
                float s = 0;
                for (int i = 0; i < sub; i++)
                    for (int k = 0; k < sub; k++)
                        s += material.displacement(x + ((i + 0.5f) / sub - 0.5f) * dx, z + ((k + 0.5f) / sub - 0.5f) * dz);
                y += s / (sub * sub) * reliefScale;
            }
            out[0] = x;
            out[1] = y;
            out[2] = z;
            if (tiled && material != null) {
                out[3] = x / material.tileX;
                out[4] = z / material.tileZ;
            } else {
                out[3] = (x - x0) / sizeX;
                out[4] = (z - z0) / sizeZ;
            }
        });
        return mb;
    }

    /** Netz der Stufe als Asset mit der Textur {@code texture} (null: Farbe des Materials). */
    public TreeAsset asset(String name, String texture, int level) {
        float[] color = material != null ? material.color : new float[]{0.2f, 0.18f, 0.14f};
        if (texture == null && material != null) texture = material.albedoName;
        return build(level).toAsset(name, "GroundPatch " + sizeX + " × " + sizeZ + " m", texture, color);
    }

    // ------------------------------------------------------------------ Formen

    /** Sanfte Hügel: Wertrauschen in zwei Lagen, Höhe ±amplitude, Wellenlänge in m; gleicher Startwert, gleiche Hügel. */
    public static GrassField.Ground hills(int seed, float amplitude, float wavelength) {
        return (x, z) -> amplitude * (0.7f * noise(seed, x / wavelength, z / wavelength)
                + 0.3f * noise(seed + 17, x * 2.3f / wavelength, z * 2.3f / wavelength));
    }

    /** Mulde: Tiefe in der Mitte, Radius bis zur halben Tiefe (Glocke). */
    public static GrassField.Ground bowl(float cx, float cz, float depth, float radius) {
        return (x, z) -> {
            float d2 = ((x - cx) * (x - cx) + (z - cz) * (z - cz)) / (radius * radius);
            return -depth * (float) Math.pow(0.5, d2);
        };
    }

    /** Summe zweier Formen. */
    public static GrassField.Ground sum(GrassField.Ground a, GrassField.Ground b) {
        return (x, z) -> a.height(x, z) + b.height(x, z);
    }

    /** Wertrauschen −1..1, weich (Smoothstep zwischen Gitterpunkten). */
    public static float noise(int seed, float x, float z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        float tx = x - x0, tz = z - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        float a = lattice(seed, x0, z0), b = lattice(seed, x0 + 1, z0), c = lattice(seed, x0, z0 + 1), d = lattice(seed, x0 + 1, z0 + 1);
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    private static float lattice(int seed, int x, int z) {
        return TreeBaker.hash(x * 73856093 ^ z * 19349663, seed) * 2 - 1;
    }
}
