package com.dan.tree.hptree.demo;

import com.dan.tree.hptree.HpTree;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Bestand mit JOGL (OpenGL 4.3), Stufe nach Entfernung.
 *
 * <pre>
 * Fenster:  HpTreeGlDemo [bäume, 150] [fläche m, 220] [tag]
 * Bild:     HpTreeGlDemo --bild datei.png [bäume] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class HpTreeGlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run(HpTree.species(), args);
    }
}
