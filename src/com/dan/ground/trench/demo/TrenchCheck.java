package com.dan.ground.trench.demo;

import com.dan.grass.GrassField;
import com.dan.ground.trench.Trench;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeCheck;

import java.util.List;
import java.util.Map;

/**
 * Selbsttest ohne Fenster: Form des Grabens (Sohle, Kante, außen eben), Szene (Wellblech an den Wänden auf der
 * Sohle, Sandsäcke auf dem Boden außerhalb, kein Gras und kein Fels im Graben, reproduzierbar), dann das Gelände
 * wie jede Art ({@link TreeCheck}). Endet mit „Alles in Ordnung.“
 */
public final class TrenchCheck {

    static int failed;

    static void check(boolean ok, String what) {
        if (!ok) failed++;
        System.out.println((ok ? "  ok    " : "  FEHLER ") + what);
    }

    public static void main(String[] args) {
        Trench t = Trench.standard();
        float ft = t.halfTop();
        System.out.println("Form");
        float[] p = t.path;
        float floor = 0, rim = 0, far = 0;
        int n = 0;
        for (int i = 0; i + 3 < p.length; i += 2) {
            float mx = (p[i] + p[i + 2]) / 2, mz = (p[i + 1] + p[i + 3]) / 2;
            floor = Math.max(floor, Math.abs(t.shape(mx, mz) + t.depth));
            n++;
        }
        for (float x = -15; x <= 15; x += 0.5f)
            for (float z = -9; z <= 9; z += 0.5f) {
                float d = t.distance(x, z);
                if (d >= ft && d - ft < 0.05f) rim = Math.max(rim, Math.abs(t.shape(x, z)));
                if (d > ft + 3) far = Math.max(far, Math.abs(t.shape(x, z)));
            }
        check(floor < 1e-4f, String.format("Sohle in der Mitte aller %d Abschnitte bei −%.1f m", n, t.depth));
        check(rim < 0.2f, String.format("Kante auf Geländehöhe (höchstens %.2f m daneben)", rim));
        check(far < 0.2f, String.format("außen fast eben (höchstens %.2f m)", far));

        System.out.println("Szene");
        Map<TreeSpecies, List<GrassField.Clump>> s = t.scene();
        check(s.equals(t.scene()) || same(s, t.scene()), "gleiche Szene bei gleichen Einstellungen");
        List<GrassField.Clump> walls = s.get(t.sheets()), bags = s.get(t.sandbags()), rocks = s.get(t.stones()), grass = s.get(t.wildgrass());
        check(walls != null && walls.size() >= 10, "Wellblech an den Wänden (" + (walls == null ? 0 : walls.size()) + " Wandstücke)");
        check(bags != null && bags.size() >= 10, "Sandsack-Mauern (" + (bags == null ? 0 : bags.size()) + ")");
        check(grass != null && grass.size() > 500, "Gras (" + (grass == null ? 0 : grass.size()) + " Büschel)");
        boolean wallsOk = true, bagsOk = true, rocksOk = true, grassOk = true;
        for (GrassField.Clump c : walls) wallsOk &= Math.abs(c.y() + t.depth) < 1e-4f && t.distance(c.x(), c.z()) < t.floorWidth / 2 + 0.01f;
        for (GrassField.Clump c : bags) bagsOk &= t.distance(c.x(), c.z()) > ft + 0.3f && Math.abs(c.y() + 0.03f - t.height(c.x(), c.z())) < 1e-3f;
        if (rocks != null) for (GrassField.Clump c : rocks) rocksOk &= t.distance(c.x(), c.z()) > ft + 1.5f;
        for (GrassField.Clump c : grass) grassOk &= t.distance(c.x(), c.z()) > ft + 1.5f && Math.abs(c.y() - t.height(c.x(), c.z())) < 1e-3f;
        check(wallsOk, "Wellblech steht auf der Sohle an der Wand");
        check(bagsOk, "Sandsäcke liegen außerhalb des Grabens auf dem Boden");
        check(rocksOk, "kein Fels im Graben oder an der Kante");
        check(grassOk, "kein Gras im Graben; Gras steht auf dem Boden");
        if (failed > 0) {
            System.out.println("Graben fehlerhaft.");
            System.exit(1);
        }
        TreeCheck.run(t.species(), 15, 25);
    }

    static boolean same(Map<TreeSpecies, List<GrassField.Clump>> a, Map<TreeSpecies, List<GrassField.Clump>> b) {
        if (a.size() != b.size()) return false;
        var ia = a.values().iterator();
        var ib = b.values().iterator();
        while (ia.hasNext()) if (!ia.next().equals(ib.next())) return false;
        return true;
    }
}
