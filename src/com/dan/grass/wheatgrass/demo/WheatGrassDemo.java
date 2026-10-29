package com.dan.grass.wheatgrass.demo;

import com.dan.grass.demo.GrassDemo;
import com.dan.grass.wheatgrass.WheatGrass;

/**
 * Vorschau auf dem Prozessor: Büschel, Wiese, Stufen.
 *
 * <pre>
 * Fenster:  WheatGrassDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     WheatGrassDemo --bild datei.png [einzel|wiese|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class WheatGrassDemo {

    public static void main(String[] args) throws Exception {
        GrassDemo.run(WheatGrass.species(), WheatGrass.field(), args);
    }
}
