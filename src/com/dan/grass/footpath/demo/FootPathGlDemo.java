package com.dan.grass.footpath.demo;

import com.dan.grass.footpath.FootPath;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Gelände mit JOGL (OpenGL 4.3).
 *
 * <pre>
 * Fenster:  FootPathGlDemo
 * Bild:     FootPathGlDemo --bild datei.png [anzahl, ohne Bedeutung] [fläche, ohne Bedeutung] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class FootPathGlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run(FootPath.species(), args);
    }
}
