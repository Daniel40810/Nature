package com.dan.grass.kikuyu.demo;

import com.dan.grass.demo.GrassDemo;
import com.dan.grass.kikuyu.Kikuyu;

/**
 * Vorschau auf dem Prozessor: Büschel, Wiese, Stufen.
 *
 * <pre>
 * Fenster:  KikuyuDemo [ansicht 0..2] [variante] [stufe 0..3|auto] [tag]
 * Bild:     KikuyuDemo --bild datei.png [einzel|wiese|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe] [zeit s]
 * </pre>
 */
public final class KikuyuDemo {

    public static void main(String[] args) throws Exception {
        GrassDemo.run(Kikuyu.species(), Kikuyu.field(), args);
    }
}
