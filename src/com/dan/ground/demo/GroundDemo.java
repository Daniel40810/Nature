package com.dan.ground.demo;

import com.dan.ground.GroundMaterial;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau eines Bodens oder Gegenstands aus {@code com.dan.ground} auf dem Prozessor (über {@link TreeDemo}):
 * Gelände schräg, von oben und in Stufen; Gegenstände einzeln, als Gruppe und in Stufen, auf dem Boden
 * {@code unter} (null: grüne Fläche). Die Pakete {@code com.dan.ground.<art>.demo} rufen nur {@link #run} auf.
 *
 * <pre>
 * Fenster:  …Demo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     …Demo --bild datei.png [einzel|oben|gruppe|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class GroundDemo {

    private GroundDemo() {}

    public static void run(TreeSpecies sp, GroundMaterial under, String[] args) throws Exception {
        TreeDemo.run(demo(sp, under), args);
    }

    /** Vorschau mit Bodentextur unter Gegenständen. */
    public static TreeDemo demo(TreeSpecies sp, GroundMaterial under) {
        TreeDemo d = new TreeDemo(sp);
        if (under != null) {
            d.groundTexture = under.albedo();
            d.groundTileX = under.tileX;
            d.groundTileZ = under.tileZ;
        }
        return d;
    }
}
