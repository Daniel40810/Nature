package com.dan.ground.dirtcoarse;

import com.dan.grass.GrassField;
import com.dan.grass.ground.GrassGround;
import com.dan.ground.GroundBlend;
import com.dan.ground.GroundMaterial;
import com.dan.ground.GroundSpecies;
import com.dan.tree.TreeSpecies;

/**
 * Erde grob („Military Trenches Ground Dirt Coarse 01“, Megascans): festgetretene Erde mit Kalk, Moos und
 * Steinchen, Kachel 2 × 2 m, Relief 3 cm. Varianten: Kachel 1 m, Fläche 6 m, Mulde 8 m und Übergang 6 m zum
 * Rasen aus {@code com.dan.grass.ground} (Erde auf −z, Rasen auf +z).
 */
public final class DirtCoarse {

    private DirtCoarse() {}

    private static GroundMaterial material;

    public static synchronized GroundMaterial material() {
        if (material == null) material = GroundMaterial.load(DirtCoarse.class, "dirtcoarse");
        return material;
    }

    public static TreeSpecies species() {
        GroundBlend.Layer lawn = GroundBlend.Layer.of(GrassGround.texture(), GrassGround.TILE_X, GrassGround.TILE_Z);
        return GroundSpecies.surface("Erde grob", "Erde mit Moos und Steinchen", DirtCoarse.class, material(), lawn);
    }

    /** Relief als Boden. */
    public static GrassField.Ground ground() {
        return material().ground(1);
    }
}
