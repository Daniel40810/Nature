package com.dan.ground.trench.demo;

import com.dan.grass.GrassField;
import com.dan.ground.trench.Trench;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeDemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Der Schützengraben auf dem Prozessor: Gelände mit Wellblech, Sandsäcken, Felsen und Gras ({@link Trench#scene}).
 * Mit Gras dauert ein Bild einige Sekunden.
 *
 * <pre>
 * Fenster:  TrenchDemo [ansicht 0..2] [variante, nur 0] [stufe 0..3|auto] [tag]
 * Bild:     TrenchDemo --bild datei.png [einzel|oben|stufen] [variante] [stufe|auto] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class TrenchDemo {

    public static void main(String[] args) throws Exception {
        TreeDemo.run(demo(Trench.standard()), args);
    }

    /** Vorschau eines Grabens mit seiner Szene. */
    public static TreeDemo demo(Trench t) {
        TreeDemo d = new TreeDemo(t.species());
        for (Map.Entry<TreeSpecies, List<GrassField.Clump>> e : t.scene().entrySet()) {
            List<TreeDemo.Placed> items = new ArrayList<>();
            for (GrassField.Clump c : e.getValue())
                items.add(new TreeDemo.Placed(c.variant(), c.x(), c.y(), c.z(), c.yaw(), c.scale(), -1));
            d.extras.add(new TreeDemo.Extra(e.getKey(), items));
        }
        d.wind.speed = 5;
        // nah am Graben, schräg auf einen Abschnitt mit Wellblech und Brustwehr
        d.dist = 9;
        d.pitch = 0.42;
        d.startYaw = 1.05;
        return d;
    }
}
