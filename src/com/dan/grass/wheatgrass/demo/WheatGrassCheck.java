package com.dan.grass.wheatgrass.demo;

import com.dan.grass.demo.GrassCheck;
import com.dan.grass.wheatgrass.WheatGrass;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class WheatGrassCheck {

    public static void main(String[] args) {
        GrassCheck.run(WheatGrass.species(), WheatGrass.field(), 0.0f, 1.6f);
    }
}
