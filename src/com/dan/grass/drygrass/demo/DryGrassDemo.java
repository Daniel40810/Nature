package com.dan.grass.drygrass.demo;

import com.dan.grass.demo.GrassDemo;
import com.dan.grass.drygrass.DryGrass;

/**
 * Vorschau auf dem Prozessor: Büschel, Wiese, Stufen.
 *
 * <pre>
 * Fenster:  DryGrassDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     DryGrassDemo --bild datei.png [einzel|wiese|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class DryGrassDemo {

    public static void main(String[] args) throws Exception {
        GrassDemo.run(DryGrass.species(), DryGrass.field(), args);
    }
}
