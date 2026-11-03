package com.dan.ground.rock.demo;

import com.dan.ground.demo.GroundDemo;
import com.dan.ground.dirtcoarse.DirtCoarse;
import com.dan.ground.rock.Rock;

/**
 * Vorschau auf dem Prozessor: einzeln, als Gruppe, Stufen nebeneinander; auf dem Boden „Erde grob“.
 *
 * <pre>
 * Fenster:  RockDemo [ansicht 0..2] [variante] [stufe 0..3|auto]
 * Bild:     RockDemo --bild datei.png [einzel|gruppe|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class RockDemo {

    public static void main(String[] args) throws Exception {
        GroundDemo.run(Rock.species(), DirtCoarse.material(), args);
    }
}
