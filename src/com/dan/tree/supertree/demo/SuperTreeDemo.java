package com.dan.tree.supertree.demo;

import com.dan.tree.supertree.SuperTree;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau auf dem Prozessor (ohne JOGL).
 *
 * <pre>
 * Fenster:  SuperTreeDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     SuperTreeDemo --bild datei.png [einzel|hain|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class SuperTreeDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(SuperTree.species(), args);
    }
}
