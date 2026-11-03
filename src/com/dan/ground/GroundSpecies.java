package com.dan.ground;

import com.dan.grass.GrassField;
import com.dan.tree.TreeAsset;
import com.dan.tree.TreeLod;
import com.dan.tree.TreeSpecies;

/**
 * Böden und Gegenstände als {@link TreeSpecies}: Die Varianten entstehen im Code ({@link Generator}), je Variante
 * vier eigene Stufen (fein bis grob). Statisch: kein Wind, keine Jahreszeit (Schnee bleibt). So zeichnen die
 * Renderer und Demos aus {@code com.dan.tree} auch Erde, Sandsäcke, Wellblech und Felsen.
 */
public final class GroundSpecies {

    private GroundSpecies() {}

    /** Netz einer Variante in einer Stufe (0 fein … 3 grob). */
    @FunctionalInterface
    public interface Generator {
        TreeAsset make(int variant, int level);
    }

    /**
     * Neue Art.
     *
     * @param form     {@code GROUND} (Gelände, Stufen nach halbem Durchmesser) oder {@code OBJECT} (steht auf dem Boden)
     * @param material Farbe und Normal-Map, die die Netze als Textur tragen (wird mit {@link GroundMaterial#addTo} bereitgestellt)
     */
    public static TreeSpecies create(String name, String latin, Class<?> owner, TreeSpecies.Form form,
                                     GroundMaterial material, String[] variantNames, Generator g) {
        int n = variantNames.length;
        String[][] lods = new String[n][TreeLod.LEVELS];
        String[] first = new String[n];
        for (int v = 0; v < n; v++) {
            for (int l = 0; l < TreeLod.LEVELS; l++) lods[v][l] = "v" + v + "_" + l;
            first[v] = lods[v][0];
        }
        TreeSpecies s = new TreeSpecies(name, latin, owner, first);
        s.variantNames = variantNames.clone();
        s.lods = lods;
        s.form = form;
        s.generator = res -> {
            if (!res.startsWith("v")) return null;
            int u = res.indexOf('_');
            return g.make(Integer.parseInt(res.substring(1, u)), Integer.parseInt(res.substring(u + 1)));
        };
        s.flexTrunk = 0;
        s.flexBranch = 0;
        s.flutter = 0;
        s.season.evergreen = true;
        // Stufen: Gelände in halben Durchmessern (wie der Fußweg), Gegenstände in ihrer Größe
        s.lodDistance = form == TreeSpecies.Form.GROUND ? new float[]{1.5f, 4, 10} : new float[]{4, 12, 35};
        if (material != null) material.addTo(s);
        return s;
    }

    /** Stücke eines Bodens: Kachel 1 m (Relief fein), Fläche 6 m (sanfte Hügel), Mulde 8 m; mit Nachbar dazu der Übergang. */
    public static final String[] SURFACE_VARIANTS = {"Kachel 1 m", "Fläche 6 m", "Mulde 8 m", "Übergang 6 m"};

    /** Form des Geländes je Variante von {@link #surface} (ohne Relief). */
    public static GrassField.Ground surfaceShape(int variant) {
        return switch (variant) {
            case 1 -> GroundPatch.hills(5, 0.18f, 3.5f);
            case 2 -> GroundPatch.sum(GroundPatch.bowl(0, 0, 0.6f, 2.2f), GroundPatch.hills(9, 0.12f, 3));
            case 3 -> GroundPatch.hills(13, 0.12f, 3);
            default -> GrassField.Ground.FLAT;
        };
    }

    /**
     * Ein Boden aus einem Material als Gelände-Art: Kachel 1 m (Zellen 5 mm), Fläche 6 m (2,5 cm), Mulde 8 m
     * (4 cm); jede Stufe hat doppelt so große Zellen. Mit {@code neighbour} (etwa Rasen) kommt Variante D dazu:
     * 6 m, auf der Seite +z liegt der Nachbar, die Grenze ist geschwungen und folgt den Steinen
     * ({@link GroundBlend}, gebacken in 2048 × 2048).
     */
    public static TreeSpecies surface(String name, String latin, Class<?> owner, GroundMaterial m, GroundBlend.Layer neighbour) {
        int n = neighbour == null ? 3 : 4;
        String[] names = java.util.Arrays.copyOf(SURFACE_VARIANTS, n);
        String blendName = m.prefix + "_blend.jpg", blendNormal = m.prefix + "_blend_normal.jpg";
        TreeSpecies[] self = new TreeSpecies[1];
        boolean[] baked = new boolean[1];
        TreeSpecies s = create(name, latin, owner, TreeSpecies.Form.GROUND, m, names, (v, level) -> {
            GroundPatch p = patch(m, v);
            String tex = null;
            if (v == 3) {
                synchronized (baked) {
                    if (!baked[0]) {
                        GroundBlend.Baked b = GroundBlend.bake(GroundBlend.Layer.of(m), neighbour, transition(),
                                -p.sizeX / 2, -p.sizeZ / 2, p.sizeX, p.sizeZ, 2048, 2048);
                        self[0].putTexture(blendName, b.albedo());
                        self[0].putTexture(blendNormal, b.normal());
                        self[0].barkNormal.put(blendName, blendNormal);
                        baked[0] = true;
                    }
                }
                tex = blendName;
            }
            return p.asset(name + " " + names[v] + " Stufe " + level, tex, level);
        });
        self[0] = s;
        return s;
    }

    /** Maske des Übergangs (Variante D): Nachbar auf der Seite +z, Grenze bei z ≈ 0, geschwungen. */
    public static GroundBlend.Mask transition() {
        return (x, z) -> {
            float edge = 0.5f * GroundPatch.noise(21, x / 1.6f, 0) + 0.15f * GroundPatch.noise(22, x / 0.4f, z / 0.4f);
            float t = (z - edge) / 0.35f;
            return Math.max(0, Math.min(1, 0.5f + t));
        };
    }

    /** Das Geländestück der Variante (ohne Netz): Größe, Zellen, Form; Kachel-UVs außer beim Übergang. */
    public static GroundPatch patch(GroundMaterial m, int variant) {
        GroundPatch p = switch (variant) {
            case 0 -> new GroundPatch(m, 1, 1, 0.005f);
            case 1, 3 -> new GroundPatch(m, 6, 6, 0.025f);
            default -> new GroundPatch(m, 8, 8, 0.04f);
        };
        p.shape = surfaceShape(variant);
        p.tiled = variant != 3;
        return p;
    }
}
