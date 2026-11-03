package com.dan.ground.corrugated.demo;

import com.dan.ground.demo.GroundDemo;
import com.dan.ground.corrugated.Corrugated;
import com.dan.ground.dirtcoarse.DirtCoarse;

/**
 * Vorschau auf dem Prozessor: einzeln, als Gruppe, Stufen nebeneinander; auf dem Boden DirtCoarse.
 *
 * <pre>
 * Fenster:  CorrugatedDemo [ansicht 0..2] [variante] [stufe 0..3|auto]
 * Bild:     CorrugatedDemo --bild datei.png [einzel|gruppe|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class CorrugatedDemo {

    public static void main(String[] args) throws Exception {
        GroundDemo.run(Corrugated.species(), DirtCoarse.material(), args);
    }
}
