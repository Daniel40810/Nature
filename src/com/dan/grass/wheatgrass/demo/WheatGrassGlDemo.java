package com.dan.grass.wheatgrass.demo;

import com.dan.grass.demo.GrassGlDemo;
import com.dan.grass.wheatgrass.WheatGrass;

/**
 * Wiese mit JOGL (OpenGL 4.3).
 *
 * <pre>
 * Fenster:  WheatGrassGlDemo [dichte je m²] [fläche m] [tag]
 * Bild:     WheatGrassGlDemo --bild datei.png [dichte je m²] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class WheatGrassGlDemo {

    public static void main(String[] args) throws Exception {
        GrassGlDemo.run(WheatGrass.species(), WheatGrass.field(), args);
    }
}
