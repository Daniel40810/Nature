package com.dan.tree.greasewood.demo;

import com.dan.tree.greasewood.Greasewood;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Bestand mit JOGL (OpenGL 4.3), Stufe nach Entfernung.
 *
 * <pre>
 * Fenster:  GreasewoodGlDemo [sträucher, 150] [fläche m, 220] [tag]
 * Bild:     GreasewoodGlDemo --bild datei.png [sträucher] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class GreasewoodGlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run(Greasewood.species(), args);
    }
}
