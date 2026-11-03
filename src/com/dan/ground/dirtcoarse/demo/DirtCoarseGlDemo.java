package com.dan.ground.dirtcoarse.demo;

import com.dan.ground.demo.GroundGlDemo;
import com.dan.ground.dirtcoarse.DirtCoarse;

/**
 * Gelände mit JOGL (OpenGL 4.3) und Normal-Map; die erste Zahl wählt die Variante.
 *
 * <pre>
 * Fenster:  DirtCoarseGlDemo [variante]
 * Bild:     DirtCoarseGlDemo --bild datei.png [variante] [fläche, ohne Bedeutung] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class DirtCoarseGlDemo {

    public static void main(String[] args) throws Exception {
        GroundGlDemo.run(DirtCoarse.species(), null, args, 1);
    }
}
