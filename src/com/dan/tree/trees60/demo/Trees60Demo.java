package com.dan.tree.trees60.demo;

import com.dan.tree.trees60.Trees60;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau auf dem Prozessor (ohne JOGL).
 *
 * <pre>
 * Fenster:  Trees60Demo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     Trees60Demo --bild datei.png [einzel|hain|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class Trees60Demo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(Trees60.species(), args);
    }
}
