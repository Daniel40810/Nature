package com.dan.grass.kikuyu.demo;

import com.dan.grass.demo.GrassGlDemo;
import com.dan.grass.kikuyu.Kikuyu;

/**
 * Wiese mit JOGL (OpenGL 4.3).
 *
 * <pre>
 * Fenster:  KikuyuGlDemo [dichte je m²] [fläche m] [tag]
 * Bild:     KikuyuGlDemo --bild datei.png [dichte je m²] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class KikuyuGlDemo {

    public static void main(String[] args) throws Exception {
        GrassGlDemo.run(Kikuyu.species(), Kikuyu.field(), args);
    }
}
