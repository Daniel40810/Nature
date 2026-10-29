package com.dan.tree.mobiletree.demo;

import com.dan.tree.mobiletree.MobileTree;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau auf dem Prozessor (ohne JOGL).
 *
 * <pre>
 * Fenster:  MobileTreeDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     MobileTreeDemo --bild datei.png [einzel|hain|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class MobileTreeDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(MobileTree.species(), args);
    }
}
