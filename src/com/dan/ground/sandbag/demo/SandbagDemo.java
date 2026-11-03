package com.dan.ground.sandbag.demo;

import com.dan.ground.demo.GroundDemo;
import com.dan.ground.sandbag.Sandbag;
import com.dan.ground.dirtrough.DirtRough;

/**
 * Vorschau auf dem Prozessor: einzeln, als Gruppe, Stufen nebeneinander; auf dem Boden DirtRough.
 *
 * <pre>
 * Fenster:  SandbagDemo [ansicht 0..2] [variante] [stufe 0..3|auto]
 * Bild:     SandbagDemo --bild datei.png [einzel|gruppe|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class SandbagDemo {

    public static void main(String[] args) throws Exception {
        GroundDemo.run(Sandbag.species(), DirtRough.material(), args);
    }
}
