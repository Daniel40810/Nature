package com.dan.ground.corrugated.demo;

import com.dan.ground.demo.GroundCheck;
import com.dan.ground.corrugated.Corrugated;

/** Selbsttest ohne Fenster: Material, Netze, Größe, Stufen, steht still; endet mit „Alles in Ordnung.“ */
public final class CorrugatedCheck {

    public static void main(String[] args) {
        GroundCheck.run(Corrugated.species(), Corrugated.material(), 0.5f, 2.5f);
    }
}
