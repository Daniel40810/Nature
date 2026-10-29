package com.dan.grass.drygrass.demo;

import com.dan.grass.demo.GrassGlDemo;
import com.dan.grass.drygrass.DryGrass;

/**
 * Wiese mit JOGL (OpenGL 4.3).
 *
 * <pre>
 * Fenster:  DryGrassGlDemo [dichte je m²] [fläche m] [tag]
 * Bild:     DryGrassGlDemo --bild datei.png [dichte je m²] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class DryGrassGlDemo {

    public static void main(String[] args) throws Exception {
        GrassGlDemo.run(DryGrass.species(), DryGrass.field(), args);
    }
}
