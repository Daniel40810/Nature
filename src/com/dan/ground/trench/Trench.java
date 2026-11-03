package com.dan.ground.trench;

import com.dan.grass.GrassField;
import com.dan.grass.wildgrass.WildGrass;
import com.dan.ground.GroundBlend;
import com.dan.ground.GroundMaterial;
import com.dan.ground.GroundPatch;
import com.dan.ground.GroundSpecies;
import com.dan.ground.corrugated.Corrugated;
import com.dan.ground.dirtcoarse.DirtCoarse;
import com.dan.ground.dirtrough.DirtRough;
import com.dan.ground.rock.Rock;
import com.dan.ground.sandbag.Sandbag;
import com.dan.tree.TreeBaker;
import com.dan.tree.TreeSpecies;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schützengraben als Bausatz aus den Paketen von {@code com.dan.ground}: ein Graben, der ins Gelände geschnitten
 * ist (Mittellinie {@link #path}, Sohle {@link #floorWidth}, Tiefe {@link #depth}, Wände {@link Corrugated#WALL_LEAN}°
 * geneigt), innen Erde rau, außen Erde grob, an der Kante geschwungen gemischt ({@link GroundBlend}). Dazu
 * {@link #scene()}: Wellblech an den Wänden, Sandsack-Mauern als Brustwehr auf beiden Kanten, eine Stellung aus
 * Sandsäcken hinter dem Graben, Felsen und Wildgras, das erst in einigem Abstand zum Graben wächst.
 *
 * <p>{@link #standard()} ist ein Graben mit Schulterwehren (Zickzack aus 7,2 m langen Abschnitten), 26 m lang.
 * Eigene Gräben: {@code new Trench(punkte)} mit den Punkten der Mittellinie (x, z, x, z, …); Abschnitte mit
 * Längen in Vielfachen von 2,4 m passen genau zu den Wellblechwänden und Sandsack-Mauern.
 */
public final class Trench {

    /** Mittellinie: x, z, x, z, … (m) */
    public final float[] path;
    public float depth = 1.9f, floorWidth = 1.0f;
    /** Größe des Geländestücks (m) */
    public float sizeX = 34, sizeZ = 20;
    /** Wellblech, Sandsäcke, Felsen, Gras dazu */
    public boolean revetment = true, parapet = true, emplacement = true, rocks = true, grass = true;
    /** Dichte des Grases weit vom Graben (Büschel je m²) */
    public float grassDensity = 10;

    private TreeSpecies species, sandbags, sheets, stones, wildgrass;

    public Trench(float... path) {
        if (path.length < 4 || path.length % 2 != 0) throw new IllegalArgumentException("mindestens zwei Punkte (x, z)");
        this.path = path.clone();
    }

    /** Graben mit Schulterwehren: Abschnitte 7,2 m im Wechsel bei z = 0 und z = 2,4, dazwischen Querstücke 2,4 m. */
    public static Trench standard() {
        return new Trench(-13.2f, -1.2f, -6, -1.2f, -6, 1.2f, 1.2f, 1.2f, 1.2f, -1.2f, 8.4f, -1.2f, 8.4f, 1.2f, 13.2f, 1.2f);
    }

    /** Halbe Breite oben (an der Kante), aus Sohle, Tiefe und Neigung der Wände. */
    public float halfTop() {
        return floorWidth / 2 + depth * (float) Math.tan(Math.toRadians(Corrugated.WALL_LEAN));
    }

    /** Abstand zur Mittellinie (m). */
    public float distance(float x, float z) {
        float best = Float.MAX_VALUE;
        for (int i = 0; i + 3 < path.length; i += 2) {
            float ax = path[i], az = path[i + 1], bx = path[i + 2], bz = path[i + 3];
            float dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
            float t = l2 > 0 ? Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / l2)) : 0;
            float ex = ax + t * dx - x, ez = az + t * dz - z;
            best = Math.min(best, ex * ex + ez * ez);
        }
        return (float) Math.sqrt(best);
    }

    /** Form ohne Relief: Sohle bei −depth, Wände gerade geneigt, außen sanfte Hügel. */
    public float shape(float x, float z) {
        float d = distance(x, z), fb = floorWidth / 2, ft = halfTop();
        float outside = GroundPatch.hills(41, 0.12f, 6).height(x, z) * Math.min(1, Math.max(0, (d - ft) / 2));
        if (d <= fb) return -depth;
        if (d < ft) return -depth * (1 - (d - fb) / (ft - fb));
        return outside;
    }

    /** Das Geländestück: Form des Grabens, Relief aus „Erde grob“, Textur einmal über die ganze Fläche. */
    public GroundPatch patch() {
        GroundPatch p = new GroundPatch(DirtCoarse.material(), sizeX, sizeZ, 0.1f);
        p.shape = this::shape;
        p.tiled = false;
        return p;
    }

    /** Höhe des Bodens am Ort, mit Relief: hierauf stehen Sandsäcke, Felsen und Gras. */
    public float height(float x, float z) {
        return patch().height(x, z);
    }

    /** Anteil „Erde grob“ am Ort: im Graben und an der Kante Erde rau, außen Erde grob, Grenze geschwungen. */
    public GroundBlend.Mask mask() {
        float ft = halfTop();
        return (x, z) -> {
            float d = distance(x, z) - ft - 0.25f + 0.35f * GroundPatch.noise(43, x / 0.9f, z / 0.9f);
            return Math.max(0, Math.min(1, 0.5f + d / 0.5f));
        };
    }

    /** Der Graben als Gelände-Art (eine Variante, Stufen mit Zellen 0,1 / 0,2 / 0,4 / 0,8 m, Textur 4096 × 2048 gebacken). */
    public synchronized TreeSpecies species() {
        if (species != null) return species;
        GroundMaterial rough = DirtRough.material(), coarse = DirtCoarse.material();
        String tex = "trench.jpg", nrm = "trench_normal.jpg";
        boolean[] baked = new boolean[1];
        TreeSpecies[] self = new TreeSpecies[1];
        TreeSpecies s = GroundSpecies.create("Schützengraben", "Erde, gegraben", Trench.class, TreeSpecies.Form.GROUND, null,
                new String[]{"Graben"}, (v, level) -> {
                    GroundPatch p = patch();
                    synchronized (baked) {
                        if (!baked[0]) {
                            int px = 4096, pz = Math.max(256, Math.round(px * sizeZ / sizeX));
                            GroundBlend.Baked b = GroundBlend.bake(GroundBlend.Layer.of(rough), GroundBlend.Layer.of(coarse), mask(),
                                    -sizeX / 2, -sizeZ / 2, sizeX, sizeZ, px, pz);
                            self[0].putTexture(tex, b.albedo());
                            self[0].putTexture(nrm, b.normal());
                            self[0].barkNormal.put(tex, nrm);
                            baked[0] = true;
                        }
                    }
                    return p.asset("Schützengraben Stufe " + level, tex, level);
                });
        self[0] = s;
        species = s;
        return s;
    }

    // ------------------------------------------------------------------ Szene

    public synchronized TreeSpecies sandbags() {
        if (sandbags == null) sandbags = Sandbag.species();
        return sandbags;
    }

    public synchronized TreeSpecies sheets() {
        if (sheets == null) sheets = Corrugated.species();
        return sheets;
    }

    public synchronized TreeSpecies stones() {
        if (stones == null) stones = Rock.species();
        return stones;
    }

    public synchronized TreeSpecies wildgrass() {
        if (wildgrass == null) wildgrass = WildGrass.species();
        return wildgrass;
    }

    /** Variante der Sandsäcke: Mauer 2,4 m bzw. Stellung; des Wellblechs: Wandstück */
    static final int WALL = 2, RING = 3, SHEET_WALL = 1;

    /**
     * Was am und um den Graben steht, je Art die Stücke (Variante, Ort, Drehung, Maßstab), in Zeichenreihenfolge.
     * Alles steht auf {@link #height}; gleiche Einstellungen, gleiche Szene.
     */
    public Map<TreeSpecies, List<GrassField.Clump>> scene() {
        Map<TreeSpecies, List<GrassField.Clump>> out = new LinkedHashMap<>();
        GroundPatch ground = patch();
        float fb = floorWidth / 2, ft = halfTop();
        List<GrassField.Clump> bags = new ArrayList<>(), walls = new ArrayList<>();
        for (int i = 0; i + 3 < path.length; i += 2) {
            float ax = path[i], az = path[i + 1], bx = path[i + 2], bz = path[i + 3];
            float len = (float) Math.hypot(bx - ax, bz - az);
            if (len < 0.5f) continue;
            float tx = (bx - ax) / len, tz = (bz - az) / len, nx = -tz, nz = tx;
            int n = Math.max(1, Math.round(len / 2.4f));
            float step = len / n;
            for (int k = 0; k < n; k++) {
                float cx = ax + tx * step * (k + 0.5f), cz = az + tz * step * (k + 0.5f);
                for (int side = -1; side <= 1; side += 2) {
                    float sx = nx * side, sz = nz * side;      // vom Graben weg
                    if (revetment) {
                        // Vorderseite zur Mitte (−s), Fuß an der Sohle, 4 cm vor der Wand
                        float r = fb - 0.04f, px = cx + sx * r, pz = cz + sz * r;
                        // nicht, wo ein Ende im Gang eines anderen Abschnitts stünde (innere Ecken)
                        if (distance(px + tx * 1.05f, pz + tz * 1.05f) > fb - 0.1f && distance(px - tx * 1.05f, pz - tz * 1.05f) > fb - 0.1f)
                            walls.add(new GrassField.Clump(SHEET_WALL, px, -depth, pz, yawFacing(-sx, -sz), 1));
                    }
                    if (parapet) {
                        float r = ft + 0.45f, px = cx + sx * r, pz = cz + sz * r;
                        // nur wo dort kein anderer Abschnitt des Grabens ist (Ecken der Schulterwehren)
                        if (distance(px, pz) > ft + 0.3f && distance(px + tx * 1.2f, pz + tz * 1.2f) > ft + 0.1f
                                && distance(px - tx * 1.2f, pz - tz * 1.2f) > ft + 0.1f)
                            bags.add(new GrassField.Clump(WALL, px, ground.height(px, pz) - 0.03f, pz, yawFacing(sx, sz),
                                    0.97f + 0.06f * TreeBaker.hash(i * 31 + k * 2 + side, 81)));
                    }
                }
            }
        }
        if (emplacement) {
            // Stellung hinter dem Graben (Seite −z), Öffnung zum Graben hin
            float px = 0, pz = -6.5f;
            bags.add(new GrassField.Clump(RING, px, ground.height(px, pz) - 0.03f, pz, (float) (Math.PI / 2), 1));
        }
        if (!walls.isEmpty()) out.put(sheets(), walls);
        if (!bags.isEmpty()) out.put(sandbags(), bags);
        GrassField.Mask free = (x, z) -> {
            float d = distance(x, z);
            if (Math.hypot(x, z + 6.5f) < 2.2) return 0;           // Stellung
            return Math.max(0, Math.min(1, (d - ft - 1.6f) / 2.5f));
        };
        float hx = sizeX / 2 - 0.5f, hz = sizeZ / 2 - 0.5f;
        if (rocks) {
            GrassField f = Rock.field();
            f.ground = ground.ground();
            f.mask = free;
            List<GrassField.Clump> r = f.place(stones().variantCount(), -hx, -hz, hx, hz);
            if (!r.isEmpty()) out.put(stones(), r);
        }
        if (grass) {
            GrassField f = new GrassField(grassDensity, 7);
            f.ground = ground.ground();
            // Gras in Flecken, dichter je weiter vom Graben
            f.mask = (x, z) -> free.at(x, z) * Math.max(0, Math.min(1, 0.65f + 0.6f * GroundPatch.noise(47, x / 2.5f, z / 2.5f)));
            List<GrassField.Clump> g = f.place(wildgrass().variantCount(), -hx, -hz, hx, hz);
            if (!g.isEmpty()) out.put(wildgrass(), g);
        }
        return out;
    }

    /** Drehung, so dass die Vorderseite eines Stücks (örtlich +z) in Richtung (fx, fz) zeigt. */
    static float yawFacing(float fx, float fz) {
        return (float) Math.atan2(-fx, fz);
    }
}
