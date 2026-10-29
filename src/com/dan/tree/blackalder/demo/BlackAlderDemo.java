package com.dan.tree.blackalder.demo;

import com.dan.tree.blackalder.BlackAlder;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau der Schwarzerle auf dem Prozessor (ohne JOGL).
 *
 * <pre>
 * Fenster:  BlackAlderDemo [ansicht 0..2] [variante 0..3] [stufe 0..3|auto] [tag]
 * Bild:     BlackAlderDemo --bild datei.png [einzel|hain|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class BlackAlderDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(BlackAlder.species(), args);
    }
}
