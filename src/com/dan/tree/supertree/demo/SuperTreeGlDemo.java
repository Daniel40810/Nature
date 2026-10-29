package com.dan.tree.supertree.demo;

import com.dan.tree.supertree.SuperTree;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Bestand mit JOGL (OpenGL 4.3), Stufe nach Entfernung.
 *
 * <pre>
 * Fenster:  SuperTreeGlDemo [bäume, 150] [fläche m, 220] [tag]
 * Bild:     SuperTreeGlDemo --bild datei.png [bäume] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class SuperTreeGlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run(SuperTree.species(), args);
    }
}
