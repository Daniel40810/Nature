package com.dan.ground.dirtrough.demo;

import com.dan.ground.demo.GroundCheck;
import com.dan.ground.dirtrough.DirtRough;

/** Selbsttest ohne Fenster: Material, Netze, Stufen, steht still; endet mit „Alles in Ordnung.“ */
public final class DirtRoughCheck {

    public static void main(String[] args) {
        GroundCheck.run(DirtRough.species(), DirtRough.material(), 0.5f, 6);
    }
}
