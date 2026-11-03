package com.dan.ground.sandbag.demo;

import com.dan.ground.demo.GroundCheck;
import com.dan.ground.sandbag.Sandbag;

/** Selbsttest ohne Fenster: Material, Netze, Größe, Stufen, steht still; endet mit „Alles in Ordnung.“ */
public final class SandbagCheck {

    public static void main(String[] args) {
        GroundCheck.run(Sandbag.species(), Sandbag.material(), 0.2f, 2f);
    }
}
