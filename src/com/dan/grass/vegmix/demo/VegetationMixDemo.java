package com.dan.grass.vegmix.demo;

import com.dan.grass.demo.GrassDemo;
import com.dan.grass.vegmix.VegetationMix;

/**
 * Vorschau auf dem Prozessor: Rasenstück, Wiese aus Kacheln, Stufen.
 *
 * <pre>
 * Fenster:  VegetationMixDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     VegetationMixDemo --bild datei.png [einzel|wiese|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class VegetationMixDemo {

    public static void main(String[] args) throws Exception {
        GrassDemo.run(VegetationMix.species(), VegetationMix.field(), args);
    }
}
