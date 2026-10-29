package com.dan.grass.drygrass.demo;

import com.dan.grass.demo.GrassCheck;
import com.dan.grass.drygrass.DryGrass;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class DryGrassCheck {

    public static void main(String[] args) {
        GrassCheck.run(DryGrass.species(), DryGrass.field(), 0.0f, 1.0f);
    }
}
