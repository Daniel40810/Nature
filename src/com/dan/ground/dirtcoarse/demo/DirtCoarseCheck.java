package com.dan.ground.dirtcoarse.demo;

import com.dan.ground.demo.GroundCheck;
import com.dan.ground.dirtcoarse.DirtCoarse;

/** Selbsttest ohne Fenster: Material, Netze, Stufen, steht still; endet mit „Alles in Ordnung.“ */
public final class DirtCoarseCheck {

    public static void main(String[] args) {
        GroundCheck.run(DirtCoarse.species(), DirtCoarse.material(), 0.5f, 6);
    }
}
