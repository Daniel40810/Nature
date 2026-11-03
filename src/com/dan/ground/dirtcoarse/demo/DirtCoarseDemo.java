package com.dan.ground.dirtcoarse.demo;

import com.dan.ground.demo.GroundDemo;
import com.dan.ground.dirtcoarse.DirtCoarse;

/**
 * Vorschau auf dem Prozessor: Gelände schräg, von oben, Stufen nebeneinander.
 *
 * <pre>
 * Fenster:  DirtCoarseDemo [ansicht 0..2] [variante] [stufe 0..3|auto]
 * Bild:     DirtCoarseDemo --bild datei.png [einzel|oben|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class DirtCoarseDemo {

    public static void main(String[] args) throws Exception {
        GroundDemo.run(DirtCoarse.species(), null, args);
    }
}
