package com.dan.grass.vegmix.demo;

import com.dan.grass.demo.GrassCheck;
import com.dan.grass.vegmix.VegetationMix;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class VegetationMixCheck {

    public static void main(String[] args) {
        GrassCheck.run(VegetationMix.species(), VegetationMix.field(), 0.3f, 1.2f);
    }
}
