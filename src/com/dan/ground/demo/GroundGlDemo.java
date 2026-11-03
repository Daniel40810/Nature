package com.dan.ground.demo;

import com.dan.ground.GroundMaterial;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Boden oder Gegenstände mit JOGL (OpenGL 4.3, über {@link TreeGlDemo}), mit Normal-Maps. Gelände: ein Stück,
 * die erste Zahl wählt die Variante. Gegenstände: so viele Stücke wie die erste Zahl, zufällig verteilt auf dem
 * Boden {@code unter}.
 *
 * <pre>
 * Fenster:  …GlDemo [variante bzw. anzahl] [fläche m] [tag]
 * Bild:     …GlDemo --bild datei.png [variante bzw. anzahl] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class GroundGlDemo {

    private GroundGlDemo() {}

    public static void run(TreeSpecies sp, GroundMaterial under, String[] args, int defaultCount) throws Exception {
        TreeGlDemo.run((count, area) -> {
            TreeGlDemo d = new TreeGlDemo(sp, count, area, null);
            if (under != null) {
                d.groundTexture = under.albedo();
                d.groundTileX = under.tileX;
                d.groundTileZ = under.tileZ;
            }
            return d;
        }, args, defaultCount);
    }
}
