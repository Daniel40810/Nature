package com.dan.tree.demo;

import com.dan.tree.TreeAsset;
import com.dan.tree.TreeBaker;
import com.dan.tree.TreeLod;
import com.dan.tree.TreeMesh;
import com.dan.tree.TreeSeason;
import com.dan.tree.TreeSkeleton;
import com.dan.tree.TreeSpecies;
import com.dan.tree.TreeWind;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * Selbsttest ohne Fenster für eine {@link TreeSpecies}: Laden, Skelett, Stufen, Wind, Jahreszeit, Hülle,
 * Reproduzierbarkeit. Endet mit „Alles in Ordnung.“ oder mit Rückgabewert 1.
 * Die Pakete {@code com.dan.tree.<art>.demo} rufen nur {@link #run(TreeSpecies, float, float)} auf.
 */
public final class TreeCheck {

    private int failed, passed;

    /** Prüft die Art; minH/maxH: erwartete Höhe der Varianten in m. */
    public static void run(TreeSpecies sp, float minH, float maxH) {
        TreeCheck c = new TreeCheck();
        try {
            c.all(sp, minH, maxH);
        } catch (RuntimeException e) {
            e.printStackTrace();
            c.failed++;
        }
        System.out.println();
        System.out.println(c.passed + " Prüfungen bestanden, " + c.failed + " fehlgeschlagen.");
        if (c.failed > 0) System.exit(1);
        System.out.println("Alles in Ordnung.");
    }

    void check(boolean ok, String what) {
        if (ok) passed++;
        else failed++;
        System.out.println((ok ? "  ok    " : "  FEHLER ") + what);
    }

    void all(TreeSpecies sp, float minH, float maxH) {
        System.out.println(sp);
        format();
        chains();
        for (int v = 0; v < sp.variantCount(); v++) variant(sp, v, minH, maxH);
        season(sp);
    }

    // ------------------------------------------------------------------

    void format() {
        System.out.println("Format");
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (GZIPOutputStream z = new GZIPOutputStream(bo)) {
                z.write(new byte[]{0, 0, 0, 1, 0, 0, 0, 1});
            }
            TreeAsset.read(new ByteArrayInputStream(bo.toByteArray()));
            check(false, "falsche Kennung wird abgelehnt");
        } catch (IOException e) {
            check(e.getMessage().contains("dtree"), "falsche Kennung wird abgelehnt (" + e.getMessage() + ")");
        }
    }

    void chains() {
        System.out.println("Ketten");
        // gerade Linie 0-1-2-3-4, bei 2 eine Gabel nach 5
        int[] parent = {-1, 0, 1, 2, 3, 2};
        float[] pos = {0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 0, 4, 0, 1, 2.5f, 0};
        boolean[] use = new boolean[6];
        Arrays.fill(use, true);
        int[] anc = TreeBaker.chains(parent, pos, use, 10, 100);
        check(anc[1] == -1 && anc[2] == 0 && anc[3] == -1 && anc[4] == 2 && anc[5] == 2,
                "gerade Strecken werden zusammengefasst, Gabel und Spitzen bleiben " + Arrays.toString(anc));
    }

    void variant(TreeSpecies sp, int v, float minH, float maxH) {
        long t0 = System.nanoTime();
        TreeAsset a = sp.load(v);
        System.out.printf("Variante %s (%s, %.0f ms)%n", sp.variantNames[v], a.source, (System.nanoTime() - t0) / 1e6);

        // Skelett
        boolean order = true, radius = true;
        for (int j = 0; j < a.joints(); j++) {
            order &= a.jointParent[j] < j;
            radius &= a.jointRadius[j] > 0 && a.jointRadius[j] < 2;
        }
        check(order && a.jointParent[0] == -1, "Gelenke: Eltern vor Kindern, eine Wurzel (" + a.joints() + " Gelenke)");
        check(radius, "Radien der Gelenke zwischen 0 und 2 m");
        boolean bind = true;
        for (int k = 0; k < a.instances(); k++)
            for (int q = 0; q < 2; q++) bind &= a.instJoint[k * 2 + q] >= 0 && a.instJoint[k * 2 + q] < a.joints();
        check(bind && a.instances() > 0, "Zweige hängen an gültigen Gelenken (" + a.instances() + " Zweige)");
        int leaves = 0;
        for (TreeAsset.Twig t : a.twigs) leaves += t.leaves();
        check(leaves > 0, "Zweig-Prototypen haben Blätter (" + leaves + ")");
        float h = a.height();
        check(h >= minH && h <= maxH, String.format("Höhe %.1f m in %.0f … %.0f m", h, minH, maxH));
        TreeSkeleton sk = new TreeSkeleton(a);
        check(sk.trunkTop > h * 0.4f, String.format("Stamm reicht bis %.1f m", sk.trunkTop));

        // Stufen
        TreeMesh[] m = new TreeMesh[TreeLod.LEVELS];
        for (int l = 0; l < TreeLod.LEVELS; l++) {
            long b0 = System.nanoTime();
            m[l] = sp.bake(v, l);
            System.out.printf("        %s (%.0f ms)%n", m[l], (System.nanoTime() - b0) / 1e6);
            check(valid(m[l]), "Stufe " + l + ": Indizes gültig, keine NaN, Normalen Länge 1");
        }
        check(m[0].triangles > m[1].triangles && m[1].triangles > m[2].triangles && m[2].triangles > m[3].triangles,
                "Dreiecke nehmen mit der Stufe ab");
        check(m[3].triangles < 1000, "Stufe 3 unter 1 000 Dreiecken (" + m[3].triangles + ")");
        check(Math.abs(m[3].height - m[0].height) < m[0].height * 0.15f,
                String.format("Hülle so hoch wie der Baum (%.1f / %.1f m)", m[3].height, m[0].height));
        TreeMesh again = sp.bake(v, 1);
        check(Arrays.equals(again.v, m[1].v) && Arrays.equals(again.tri, m[1].tri), "Backen ist reproduzierbar");

        wind(sp, a, m[1]);
    }

    static boolean valid(TreeMesh m) {
        for (int i = 0; i < m.triangles * 3; i++) if (m.tri[i] < 0 || m.tri[i] >= m.vertices) return false;
        for (int i = 0; i < m.vertices; i++) {
            int b = i * TreeMesh.STRIDE;
            for (int k = 0; k < TreeMesh.STRIDE; k++) if (!Float.isFinite(m.v[b + k])) return false;
            float n = (float) Math.sqrt(m.v[b + 3] * m.v[b + 3] + m.v[b + 4] * m.v[b + 4] + m.v[b + 5] * m.v[b + 5]);
            if (Math.abs(n - 1) > 0.05f) return false;
        }
        return true;
    }

    void wind(TreeSpecies sp, TreeAsset a, TreeMesh m) {
        TreeWind w = new TreeWind();
        float[] out = new float[m.vertices * 3];
        w.speed = 0;
        w.displace(m, sp, a.gustAttenuation, 3.3f, 0, 0, 0, 0, out);
        float still = maxShift(m, out, -1e9f, 1e9f);
        check(still < 1e-5f, "ohne Wind steht der Baum still");

        w.speed = 15;
        w.gustiness = 0;
        w.fromDeg = 270;                      // aus West, weht nach +x
        w.displace(m, sp, a.gustAttenuation, 3.3f, 0, 0, 0, 0, out);
        float foot = maxShift(m, out, -1e9f, 0.3f);
        check(foot < 0.01f, String.format("Fuß bleibt stehen (%.4f m)", foot));
        float mid = meanShift(m, out, m.height * 0.3f, m.height * 0.5f);
        float top = meanShift(m, out, m.height * 0.8f, m.height * 1.1f);
        check(top > mid && top > 0.05f, String.format("oben mehr Ausschlag als in der Mitte (%.2f / %.2f m)", top, mid));
        double dx = 0;
        for (int i = 0; i < m.vertices; i++) dx += out[i * 3] - m.v[i * TreeMesh.STRIDE];
        check(dx > 0, "der Baum neigt sich mit dem Wind");
        float[] out2 = new float[out.length];
        w.displace(m, sp, a.gustAttenuation, 3.3f, 0, 0, 0, 0, out2);
        check(Arrays.equals(out, out2), "Wind ist reproduzierbar");

        // Drehung des Baums: Wind im Baumraum gegen die Drehung
        w.displace(m, sp, a.gustAttenuation, 3.3f, 0, 0, 0, (float) (Math.PI / 2), out2);
        double dz = 0;
        for (int i = 0; i < m.vertices; i++) dz += out2[i * 3 + 2] - m.v[i * TreeMesh.STRIDE + 2];
        // Welt +x entspricht bei yaw 90° dem Baumraum −z
        check(dz < 0, "gedrehter Baum neigt sich in der Welt ebenfalls mit dem Wind");

        w.speed = 25;
        w.displace(m, sp, a.gustAttenuation, 3.3f, 0, 0, 0, 0, out);
        float top25 = meanShift(m, out, m.height * 0.8f, m.height * 1.1f);
        check(top25 > top, String.format("stärkerer Wind, größerer Ausschlag (%.2f > %.2f m)", top25, top));
        check(tears(m, out) < 0.002, String.format("keine Risse im Sturm (%.3f %% gedehnte Kanten)", tears(m, out) * 100));
    }

    static float maxShift(TreeMesh m, float[] out, float y0, float y1) {
        float mx = 0;
        for (int i = 0; i < m.vertices; i++) {
            int b = i * TreeMesh.STRIDE;
            if (m.v[b + 1] < y0 || m.v[b + 1] > y1) continue;
            float d = (float) Math.sqrt(sq(out[i * 3] - m.v[b]) + sq(out[i * 3 + 1] - m.v[b + 1]) + sq(out[i * 3 + 2] - m.v[b + 2]));
            mx = Math.max(mx, d);
        }
        return mx;
    }

    static float meanShift(TreeMesh m, float[] out, float y0, float y1) {
        double s = 0;
        int n = 0;
        for (int i = 0; i < m.vertices; i++) {
            int b = i * TreeMesh.STRIDE;
            if (m.v[b + 1] < y0 || m.v[b + 1] > y1) continue;
            s += Math.sqrt(sq(out[i * 3] - m.v[b]) + sq(out[i * 3 + 2] - m.v[b + 2]));
            n++;
        }
        return n == 0 ? 0 : (float) (s / n);
    }

    /** Anteil der Kanten, die sich mehr als 50 % und mehr als 5 cm dehnen. */
    static double tears(TreeMesh m, float[] out) {
        int bad = 0, all = 0;
        for (int t = 0; t < m.triangles; t++)
            for (int e = 0; e < 3; e++) {
                int i = m.tri[t * 3 + e], j = m.tri[t * 3 + (e + 1) % 3];
                int bi = i * TreeMesh.STRIDE, bj = j * TreeMesh.STRIDE;
                float r = (float) Math.sqrt(sq(m.v[bi] - m.v[bj]) + sq(m.v[bi + 1] - m.v[bj + 1]) + sq(m.v[bi + 2] - m.v[bj + 2]));
                float d = (float) Math.sqrt(sq(out[i * 3] - out[j * 3]) + sq(out[i * 3 + 1] - out[j * 3 + 1]) + sq(out[i * 3 + 2] - out[j * 3 + 2]));
                all++;
                if (d - r > 0.05f && d > r * 1.5f) bad++;
            }
        return bad / (double) Math.max(all, 1);
    }

    static float sq(float x) {
        return x * x;
    }

    void season(TreeSpecies sp) {
        System.out.println("Jahreszeit");
        TreeSeason s = sp.season;
        TreeSeason.Look winter = s.look(20, 0), spring = s.look(s.leafOut + s.flushDays / 2, 0),
                summer = s.look(200, 0), fall = s.look((s.colorFull + s.bare) / 2, 0);
        check(winter.bare(), "im Januar kahl");
        check(summer.lo == 0 && summer.hi == 1, "im Juli alles Laub");
        check(spring.hi > 0.1f && spring.hi < 0.9f, String.format("beim Austrieb ein Teil (%.2f)", spring.hi));
        check(fall.lo > 0.1f && fall.lo < 0.9f, String.format("beim Laubfall ein Teil (%.2f)", fall.lo));
        TreeSeason.Look autumn = s.look(s.colorFull, 0);
        float diff = 0;
        for (int k = 0; k < 3; k++) diff += Math.abs(autumn.colorLo[k] - summer.colorLo[k]);
        check(diff > 0.01f, "im Herbst andere Farbe als im Sommer");
        TreeMesh m = sp.bake(0, 2);
        float[] c = new float[3];
        int shown = 0;
        for (int i = 0; i < m.vertices; i++) if (m.leaf(i) && m.color(i, sp, sp.load(0), fall, c)) shown++;
        int leafVerts = 0;
        for (int i = 0; i < m.vertices; i++) if (m.leaf(i)) leafVerts++;
        check(shown > 0 && shown < leafVerts, "beim Laubfall sind nur manche Blätter da (" + shown + " von " + leafVerts + " Punkten)");
    }
}
