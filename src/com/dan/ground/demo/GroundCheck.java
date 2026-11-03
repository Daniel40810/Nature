package com.dan.ground.demo;

import com.dan.ground.GroundMaterial;
import com.dan.tree.TreeSpecies;
import com.dan.tree.demo.TreeCheck;

import java.awt.image.BufferedImage;

/**
 * Selbsttest eines Bodens oder Gegenstands: zuerst das Material (Bilder da, Höhenkarte nahtlos,
 * Relief um die Mitte, Normalen zeigen nach oben), dann alles aus {@link TreeCheck} (Netz mit UVs, Größe, Stufen,
 * steht im Sturm still). Endet mit „Alles in Ordnung.“ oder mit Rückgabewert 1.
 */
public final class GroundCheck {

    private GroundCheck() {}

    private static int failed;

    /** minSize/maxSize: Gelände halber Durchmesser, Gegenstände größere von Höhe und halbem Durchmesser (m). */
    public static void run(TreeSpecies sp, GroundMaterial m, float minSize, float maxSize) {
        if (m != null) material(m);
        if (failed > 0) {
            System.out.println("Material fehlerhaft.");
            System.exit(1);
        }
        TreeCheck.run(sp, minSize, maxSize);
    }

    static void check(boolean ok, String what) {
        if (!ok) failed++;
        System.out.println((ok ? "  ok    " : "  FEHLER ") + what);
    }

    /** Prüft ein Material; liefert die Zahl der Fehler. */
    public static int material(GroundMaterial m) {
        int before = failed;
        System.out.println("Material " + m);
        BufferedImage a = m.albedo(), n = m.normal();
        check(a.getWidth() >= 256 && a.getWidth() == a.getHeight(), "Farbe " + a.getWidth() + " × " + a.getHeight());
        check(n.getWidth() >= 256, "Normal-Map " + n.getWidth() + " × " + n.getHeight());
        check(m.tileX > 0.1f && m.tileZ > 0.1f && m.relief > 0 && m.relief < 0.5f, "Kachel und Relief plausibel");
        // Höhe: nahtlos (Sprung über den Rand wie der mittlere Sprung zwischen Nachbarn)
        int w = m.heightMapSize();
        double edge = 0, inner = 0, mean = 0;
        int ni = 0;
        for (int i = 0; i < w; i++) {
            float v = (i + 0.5f) / w;
            edge += Math.abs(m.height01(0.5f / w, v) - m.height01(-0.5f / w, v)) + Math.abs(m.height01(v, 0.5f / w) - m.height01(v, -0.5f / w));
            if (i % 7 == 0)
                for (int k = 1; k < w; k++) {
                    float b = (float) k / w;
                    inner += Math.abs(m.height01(b + 0.5f / w, v) - m.height01(b - 0.5f / w, v)) + Math.abs(m.height01(v, b + 0.5f / w) - m.height01(v, b - 0.5f / w));
                    mean += m.height01(v, (k + 0.5f) / w);
                    ni++;
                }
        }
        mean /= ni;
        edge /= 2 * w;
        inner /= 2 * ni;
        check(edge < inner * 2 + 1e-3, String.format("Höhenkarte nahtlos (Sprung am Rand %.4f, sonst %.4f)", edge, inner));
        check(Math.abs(mean - m.heightMid) < 0.2, String.format("Relief um die Mitte (Mittel %.3f, Mitte %.3f)", mean, m.heightMid));
        double up = 0;
        for (int y = 0; y < n.getHeight(); y += 8) for (int x = 0; x < n.getWidth(); x += 8) up += (n.getRGB(x, y) & 255) / 255.0 * 2 - 1;
        up /= (double) ((n.getHeight() + 7) / 8) * ((n.getWidth() + 7) / 8);
        check(up > 0.6, String.format("Normalen zeigen im Mittel nach oben (%.2f)", up));
        return failed - before;
    }
}
