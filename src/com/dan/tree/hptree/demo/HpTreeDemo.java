package com.dan.tree.hptree.demo;

import com.dan.tree.hptree.HpTree;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau auf dem Prozessor (ohne JOGL).
 *
 * <pre>
 * Fenster:  HpTreeDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     HpTreeDemo --bild datei.png [einzel|hain|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class HpTreeDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(HpTree.species(), args);
    }
}
