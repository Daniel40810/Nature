package com.dan.ground.sandbag.demo;

import com.dan.ground.demo.GroundGlDemo;
import com.dan.ground.sandbag.Sandbag;
import com.dan.ground.dirtrough.DirtRough;

/**
 * Viele Stücke mit JOGL (OpenGL 4.3) und Normal-Maps, zufällig verteilt auf dem Boden DirtRough.
 *
 * <pre>
 * Fenster:  SandbagGlDemo [anzahl] [fläche m] [tag]
 * Bild:     SandbagGlDemo --bild datei.png [anzahl] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class SandbagGlDemo {

    public static void main(String[] args) throws Exception {
        GroundGlDemo.run(Sandbag.species(), DirtRough.material(), args, 40);
    }
}
