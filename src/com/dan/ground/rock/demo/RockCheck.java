package com.dan.ground.rock.demo;

import com.dan.grass.GrassField;
import com.dan.ground.demo.GroundCheck;
import com.dan.ground.rock.Rock;

import java.util.List;

/** Selbsttest ohne Fenster: Material, Netze, Größe, Stufen, steht still, Streuung; endet mit „Alles in Ordnung.“ */
public final class RockCheck {

    public static void main(String[] args) {
        GrassField f = Rock.field();
        List<GrassField.Clump> a = f.place(4, -50, -50, 50, 50);
        double perM2 = a.size() / 10000.0;
        boolean ok = a.equals(f.place(4, -50, -50, 50, 50)) && Math.abs(perM2 - f.density) < f.density * 0.25;
        System.out.printf("Streuung%n  %s %d Felsen auf 100 × 100 m (%.3f je m²), reproduzierbar%n", ok ? "ok    " : "FEHLER", a.size(), perM2);
        if (!ok) System.exit(1);
        GroundCheck.run(Rock.species(), Rock.material(), 0.15f, 1.5f);
    }
}
