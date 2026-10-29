package com.dan.tree.blackalder.demo;

import com.dan.tree.blackalder.BlackAlder;
import com.dan.tree.demo.TreeGlDemo;

/**
 * Bestand aus Schwarzerlen mit JOGL (OpenGL 4.3), Stufe nach Entfernung.
 *
 * <pre>
 * BlackAlderGlDemo [bäume, 150] [fläche m, 220] [tag]
 * </pre>
 */
public final class BlackAlderGlDemo {

    public static void main(String[] args) {
        TreeGlDemo.run(BlackAlder.species(), args);
    }
}
