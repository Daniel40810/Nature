package com.dan.ground.corrugated.demo;

import com.dan.ground.demo.GroundGlDemo;
import com.dan.ground.corrugated.Corrugated;
import com.dan.ground.dirtcoarse.DirtCoarse;

/**
 * Viele Stücke mit JOGL (OpenGL 4.3) und Normal-Maps, zufällig verteilt auf dem Boden DirtCoarse.
 *
 * <pre>
 * Fenster:  CorrugatedGlDemo [anzahl] [fläche m] [tag]
 * Bild:     CorrugatedGlDemo --bild datei.png [anzahl] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class CorrugatedGlDemo {

    public static void main(String[] args) throws Exception {
        GroundGlDemo.run(Corrugated.species(), DirtCoarse.material(), args, 30);
    }
}
