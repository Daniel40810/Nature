package com.dan.grass.footpath.demo;

import com.dan.grass.footpath.FootPath;
import com.dan.tree.demo.TreeDemo;

/**
 * Vorschau des Geländes auf dem Prozessor: schräg, von oben, Stufen nebeneinander.
 *
 * <pre>
 * Fenster:  FootPathDemo [ansicht 0..2] [variante] [stufe 0..3|auto]
 * Bild:     FootPathDemo --bild datei.png [einzel|oben|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class FootPathDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(FootPath.species(), args);
    }
}
