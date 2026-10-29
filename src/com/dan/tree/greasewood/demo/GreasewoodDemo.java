package com.dan.tree.greasewood.demo;

import com.dan.tree.greasewood.Greasewood;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau auf dem Prozessor (ohne JOGL).
 *
 * <pre>
 * Fenster:  GreasewoodDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     GreasewoodDemo --bild datei.png [einzel|hain|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class GreasewoodDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(Greasewood.species(), args);
    }
}
