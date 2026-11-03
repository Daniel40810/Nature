package com.dan.ground.dirtrough.demo;

import com.dan.ground.demo.GroundGlDemo;
import com.dan.ground.dirtrough.DirtRough;

/**
 * Gelände mit JOGL (OpenGL 4.3) und Normal-Map; die erste Zahl wählt die Variante.
 *
 * <pre>
 * Fenster:  DirtRoughGlDemo [variante]
 * Bild:     DirtRoughGlDemo --bild datei.png [variante] [fläche, ohne Bedeutung] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class DirtRoughGlDemo {

    public static void main(String[] args) throws Exception {
        GroundGlDemo.run(DirtRough.species(), null, args, 1);
    }
}
