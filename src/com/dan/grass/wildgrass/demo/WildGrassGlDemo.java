package com.dan.grass.wildgrass.demo;

import com.dan.grass.demo.GrassGlDemo;
import com.dan.grass.wildgrass.WildGrass;

/**
 * Wiese mit JOGL (OpenGL 4.3).
 *
 * <pre>
 * Fenster:  WildGrassGlDemo [dichte je m²] [fläche m] [tag]
 * Bild:     WildGrassGlDemo --bild datei.png [dichte je m²] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class WildGrassGlDemo {

    public static void main(String[] args) throws Exception {
        GrassGlDemo.run(WildGrass.species(), WildGrass.field(), args);
    }
}
