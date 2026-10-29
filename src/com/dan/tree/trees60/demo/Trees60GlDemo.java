package com.dan.tree.trees60.demo;

import com.dan.tree.trees60.Trees60;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Bestand mit JOGL (OpenGL 4.3), Stufe nach Entfernung.
 *
 * <pre>
 * Fenster:  Trees60GlDemo [bäume, 150] [fläche m, 220] [tag]
 * Bild:     Trees60GlDemo --bild datei.png [bäume] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class Trees60GlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run(Trees60.species(), args);
    }
}
