package com.dan.ground.trench.demo;

import com.dan.grass.GrassField;
import com.dan.ground.trench.Trench;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeGlDemo;
import com.dan.tree.gl.TreeGl;

import java.util.List;
import java.util.Map;

/**
 * Der Schützengraben mit JOGL (OpenGL 4.3): Gelände, Wellblech, Sandsäcke, Felsen und Gras im Wind, mit Normal-Maps.
 * Die erste Zahl ist die Dichte des Grases (Büschel je m², 0 = ohne Gras).
 *
 * <pre>
 * Fenster:  TrenchGlDemo [gras je m²] [fläche, ohne Bedeutung] [tag]
 * Bild:     TrenchGlDemo --bild datei.png [gras je m²] [fläche] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class TrenchGlDemo {

    public static void main(String[] args) throws Exception {
        TreeGlDemo.run((count, area) -> {
            Trench t = Trench.standard();
            t.grass = count > 0;
            t.grassDensity = count;
            TreeGlDemo d = new TreeGlDemo(t.species(), 0, area, null);
            for (Map.Entry<TreeSpecies, List<GrassField.Clump>> e : t.scene().entrySet()) {
                TreeGl gl = new TreeGl(e.getKey());
                for (GrassField.Clump c : e.getValue()) gl.add(c.variant(), c.x(), c.y(), c.z(), c.yaw(), c.scale());
                d.extras.add(gl);
            }
            d.wind.speed = 5;
            d.wind.gustiness = 0.5f;
            d.dist = 9;
            d.pitch = 0.42;
            d.yaw = 1.05;
            return d;
        }, args, 10);
    }
}
