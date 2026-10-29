package com.dan.grass.wildgrass.demo;

import com.dan.grass.demo.GrassDemo;
import com.dan.grass.wildgrass.WildGrass;

/**
 * Vorschau auf dem Prozessor: Büschel, Wiese, Stufen.
 *
 * <pre>
 * Fenster:  WildGrassDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     WildGrassDemo --bild datei.png [einzel|wiese|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class WildGrassDemo {

    public static void main(String[] args) throws Exception {
        GrassDemo.run(WildGrass.species(), WildGrass.field(), args);
    }
}
