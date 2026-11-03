package com.dan.ground.dirtrough;

import com.dan.grass.GrassField;
import com.dan.ground.GroundMaterial;
import com.dan.ground.GroundSpecies;
import com.dan.tree.TreeSpecies;

/**
 * Erde rau („Military Trenches Ground Dirt Rough 02“, Megascans): trockene Erde mit Kalk, Kies und kantigen
 * Steinen, Kachel 1 × 1 m, Relief 8 cm. Varianten: Kachel 1 m, Fläche 6 m mit sanften Hügeln, Mulde 8 m.
 */
public final class DirtRough {

    private DirtRough() {}

    private static GroundMaterial material;

    public static synchronized GroundMaterial material() {
        if (material == null) material = GroundMaterial.load(DirtRough.class, "dirtrough");
        return material;
    }

    public static TreeSpecies species() {
        return GroundSpecies.surface("Erde rau", "Erde mit Kalk und Kies", DirtRough.class, material(), null);
    }

    /** Relief als Boden (etwa um Steine oder Gras auf eine ebene Fläche aus diesem Material zu setzen). */
    public static GrassField.Ground ground() {
        return material().ground(1);
    }
}
