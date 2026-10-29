package com.dan.grass.demo;

import com.dan.grass.GrassField;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeCheck;

import java.util.List;

/**
 * Selbsttest einer Grasart: alles aus {@link TreeCheck} (Format, Stufen, Wind, Jahreszeit) und dazu die Wiese
 * ({@link GrassField}: Dichte, Reproduzierbarkeit, Ausschnitt gleich Ganzes, Maske). Endet mit „Alles in Ordnung.“
 */
public final class GrassCheck {

    private GrassCheck() {}

    public static void run(TreeSpecies sp, GrassField field, float minH, float maxH) {
        if (field != null) field(sp, field);
        TreeCheck.run(sp, minH, maxH);
    }

    static void field(TreeSpecies sp, GrassField f) {
        System.out.println("Wiese");
        int n = sp.variantCount();
        List<GrassField.Clump> a = f.place(n, -10, -10, 10, 10), b = f.place(n, -10, -10, 10, 10);
        boolean same = a.equals(b);
        double perM2 = a.size() / 400.0;
        List<GrassField.Clump> part = f.place(n, 0, 0, 10, 10);
        boolean sub = a.containsAll(part);
        int[] count = new int[n];
        for (GrassField.Clump c : a) count[c.variant()]++;
        boolean allVariants = true;
        for (int c : count) allVariants &= c > 0 || a.size() < n * 5;
        GrassField g = new GrassField(f.density, f.seed);
        g.mask = (x, z) -> Math.abs(x) < 2 ? 0 : 1;
        boolean masked = g.place(n, -10, -10, 10, 10).stream().noneMatch(c -> Math.abs(c.x()) < 2);
        System.out.printf("  %s gleiche Wiese bei gleichem Startwert%n", same ? "ok    " : "FEHLER");
        System.out.printf("  %s Dichte %.2f je m² (verlangt %.2f)%n", Math.abs(perM2 - f.density) < f.density * 0.15 ? "ok    " : "FEHLER", perM2, f.density);
        System.out.printf("  %s Ausschnitt gehört zur ganzen Wiese%n", sub ? "ok    " : "FEHLER");
        System.out.printf("  %s alle Varianten kommen vor%n", allVariants ? "ok    " : "FEHLER");
        System.out.printf("  %s Maske hält einen Weg frei%n", masked ? "ok    " : "FEHLER");
        if (!(same && sub && allVariants && masked && Math.abs(perM2 - f.density) < f.density * 0.15)) {
            System.out.println("Wiese fehlerhaft.");
            System.exit(1);
        }
    }
}
