package com.dan.ground.dirtrough.demo;

import com.dan.ground.demo.GroundDemo;
import com.dan.ground.dirtrough.DirtRough;

/**
 * Vorschau auf dem Prozessor: Gelände schräg, von oben, Stufen nebeneinander.
 *
 * <pre>
 * Fenster:  DirtRoughDemo [ansicht 0..2] [variante] [stufe 0..3|auto]
 * Bild:     DirtRoughDemo --bild datei.png [einzel|oben|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class DirtRoughDemo {

    public static void main(String[] args) throws Exception {
        GroundDemo.run(DirtRough.species(), null, args);
    }
}
