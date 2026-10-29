package com.dan.grass.footpath.demo;

import com.dan.grass.GrassField;
import com.dan.grass.footpath.FootPath;
import com.dan.tree.demo.TreeCheck;

/** Selbsttest ohne Fenster: Gelände (Netz, Textur, Stufen, steht still) und Höhe am Ort; endet mit „Alles in Ordnung.“ */
public final class FootPathCheck {

    public static void main(String[] args) {
        GrassField.Ground g = FootPath.ground();
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (float x = -15; x <= 15; x += 1)
            for (float z = -20; z <= 20; z += 1) {
                float y = g.height(x, z);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        boolean ok = hi - lo > 0.5f && hi - lo < 5 && Float.isFinite(lo);
        System.out.printf("Höhe am Ort%n  %s Höhen von %.2f bis %.2f m%n", ok ? "ok    " : "FEHLER", lo, hi);
        if (!ok) System.exit(1);
        TreeCheck.run(FootPath.species(), 15, 30);
    }
}
