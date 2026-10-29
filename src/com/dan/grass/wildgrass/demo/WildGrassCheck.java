package com.dan.grass.wildgrass.demo;

import com.dan.grass.demo.GrassCheck;
import com.dan.grass.wildgrass.WildGrass;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class WildGrassCheck {

    public static void main(String[] args) {
        GrassCheck.run(WildGrass.species(), WildGrass.field(), 0.05f, 0.6f);
    }
}
