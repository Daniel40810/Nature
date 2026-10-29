package com.dan.tree.mobiletree.demo;

import com.dan.tree.mobiletree.MobileTree;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Bestand mit JOGL (OpenGL 4.3), Stufe nach Entfernung.
 *
 * <pre>
 * Fenster:  MobileTreeGlDemo [bäume, 150] [fläche m, 220] [tag]
 * Bild:     MobileTreeGlDemo --bild datei.png [bäume] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class MobileTreeGlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run(MobileTree.species(), args);
    }
}
