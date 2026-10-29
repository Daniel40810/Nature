package com.dan.grass.vegmix.demo;

import com.dan.grass.demo.GrassGlDemo;
import com.dan.grass.vegmix.VegetationMix;

/**
 * Wiese aus Kacheln mit JOGL (OpenGL 4.3).
 *
 * <pre>
 * Fenster:  VegetationMixGlDemo [dichte je m²] [fläche m] [tag]
 * Bild:     VegetationMixGlDemo --bild datei.png [dichte je m²] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class VegetationMixGlDemo {

    public static void main(String[] args) throws Exception {
        GrassGlDemo.run(VegetationMix.species(), VegetationMix.field(), args);
    }
}
