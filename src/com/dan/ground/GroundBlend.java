package com.dan.ground;

import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

/**
 * Backt zwei Böden zu einem Bild (Farbe und Normal-Map) für eine Fläche, etwa Erde im Graben und Gras daneben.
 * Wo welcher Boden liegt, sagt die {@link Mask}; an der Grenze entscheidet die Höhe: Steine des einen Bodens
 * ragen in den anderen hinein, statt dass die Farben weich überblendet werden. Das Bild wird einmal über die
 * Fläche gespannt ({@link GroundPatch#tiled} = false).
 */
public final class GroundBlend {

    private GroundBlend() {}

    /** Anteil des zweiten Bodens am Ort, 0..1. */
    @FunctionalInterface
    public interface Mask {
        float at(float x, float z);
    }

    /** Ein Boden zum Mischen: Farbe, Normal-Map (oder null: flach), Höhe (oder null: Helligkeit der Farbe), Kachel in m. */
    public record Layer(BufferedImage albedo, BufferedImage normal, GroundMaterial height, float tileX, float tileZ) {
        public static Layer of(GroundMaterial m) {
            return new Layer(m.albedo(), m.normal(), m, m.tileX, m.tileZ);
        }

        /** Nur ein Farbbild (etwa Rasen ohne Höhe und Normalen). */
        public static Layer of(BufferedImage albedo, float tileX, float tileZ) {
            return new Layer(albedo, null, null, tileX, tileZ);
        }
    }

    /** Ergebnis: Farbe (sRGB) und Normal-Map (OpenGL-Format). */
    public record Baked(BufferedImage albedo, BufferedImage normal) {}

    /** Breite des Übergangs in Höheneinheiten (0..1): klein = harte Kanten an Steinen */
    public static float depth = 0.2f;

    /**
     * Mischt a und b über das Rechteck [x0, x0+sizeX] × [z0, z0+sizeZ] in ein Bild mit px × pz Punkten.
     * Zeile 0 liegt bei z0 (v läuft mit z, wie die UVs von {@link GroundPatch}).
     */
    public static Baked bake(Layer a, Layer b, Mask mask, float x0, float z0, float sizeX, float sizeZ, int px, int pz) {
        Sampler ca = new Sampler(a.albedo), cb = new Sampler(b.albedo);
        Sampler na = a.normal != null ? new Sampler(a.normal) : null, nb = b.normal != null ? new Sampler(b.normal) : null;
        int[] col = new int[px * pz], nrm = new int[px * pz];
        IntStream.range(0, pz).parallel().forEach(y -> {
            float[] s = new float[3], t = new float[3];
            for (int x = 0; x < px; x++) {
                float wx = x0 + (x + 0.5f) * sizeX / px, wz = z0 + (y + 0.5f) * sizeZ / pz;
                float m = Math.max(0, Math.min(1, mask.at(wx, wz)));
                float ua = wx / a.tileX, va = wz / a.tileZ, ub = wx / b.tileX, vb = wz / b.tileZ;
                float wa, wb;
                if (m <= 0) {
                    wa = 1;
                    wb = 0;
                } else if (m >= 1) {
                    wa = 0;
                    wb = 1;
                } else {
                    float ha = a.height != null ? a.height.height01(ua, va) : ca.luminance(ua, va);
                    float hb = b.height != null ? b.height.height01(ub, vb) : cb.luminance(ub, vb);
                    float ta = ha + 2 * (1 - m), tb = hb + 2 * m;
                    float top = Math.max(ta, tb) - depth;
                    wa = Math.max(ta - top, 0);
                    wb = Math.max(tb - top, 0);
                    float sum = wa + wb;
                    wa /= sum;
                    wb /= sum;
                }
                float[] c = new float[3];
                if (wa > 0) {
                    ca.linear(ua, va, s);
                    for (int k = 0; k < 3; k++) c[k] += s[k] * wa;
                }
                if (wb > 0) {
                    cb.linear(ub, vb, t);
                    for (int k = 0; k < 3; k++) c[k] += t[k] * wb;
                }
                col[y * px + x] = Sampler.packSrgb(c);
                float[] n = new float[3];
                if (wa > 0) {
                    vec(na, ua, va, s);
                    for (int k = 0; k < 3; k++) n[k] += s[k] * wa;
                }
                if (wb > 0) {
                    vec(nb, ub, vb, t);
                    for (int k = 0; k < 3; k++) n[k] += t[k] * wb;
                }
                float l = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) + 1e-9f;
                nrm[y * px + x] = Sampler.packUnit(n[0] / l, n[1] / l, n[2] / l);
            }
        });
        BufferedImage ai = new BufferedImage(px, pz, BufferedImage.TYPE_INT_RGB), ni = new BufferedImage(px, pz, BufferedImage.TYPE_INT_RGB);
        ai.setRGB(0, 0, px, pz, col, 0, px);
        ni.setRGB(0, 0, px, pz, nrm, 0, px);
        return new Baked(ai, ni);
    }

    private static void vec(Sampler s, float u, float v, float[] out) {
        if (s == null) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 1;
        } else {
            s.raw(u, v, out);
            for (int k = 0; k < 3; k++) out[k] = out[k] * 2 - 1;
        }
    }

    /** Bild bilinear und wiederholt abtasten. */
    static final class Sampler {
        static final float[] LINEAR = new float[256];

        static {
            for (int i = 0; i < 256; i++) {
                double c = i / 255.0;
                LINEAR[i] = (float) (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
            }
        }

        final int w, h;
        final int[] px;

        Sampler(BufferedImage img) {
            w = img.getWidth();
            h = img.getHeight();
            px = img.getRGB(0, 0, w, h, null, 0, w);
        }

        /** Werte 0..1 je Kanal (ohne Umrechnung). */
        void raw(float u, float v, float[] out) {
            sample(u, v, out, false);
        }

        /** Farbe linear. */
        void linear(float u, float v, float[] out) {
            sample(u, v, out, true);
        }

        float luminance(float u, float v) {
            float[] c = new float[3];
            sample(u, v, c, false);
            return 0.3f * c[0] + 0.6f * c[1] + 0.1f * c[2];
        }

        private void sample(float u, float v, float[] out, boolean lin) {
            float fx = u * w - 0.5f, fy = v * h - 0.5f;
            int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
            float tx = fx - x0, ty = fy - y0;
            int xa = Math.floorMod(x0, w), xb = Math.floorMod(x0 + 1, w), ya = Math.floorMod(y0, h), yb = Math.floorMod(y0 + 1, h);
            int[] q = {px[ya * w + xa], px[ya * w + xb], px[yb * w + xa], px[yb * w + xb]};
            float[] f = {(1 - tx) * (1 - ty), tx * (1 - ty), (1 - tx) * ty, tx * ty};
            out[0] = out[1] = out[2] = 0;
            for (int k = 0; k < 4; k++)
                for (int e = 0; e < 3; e++) {
                    int b = (q[k] >> (16 - 8 * e)) & 255;
                    out[e] += f[k] * (lin ? LINEAR[b] : b / 255f);
                }
        }

        static int packSrgb(float[] c) {
            int r = 0;
            for (int e = 0; e < 3; e++) {
                double x = Math.max(0, Math.min(1, c[e]));
                x = x <= 0.0031308 ? x * 12.92 : 1.055 * Math.pow(x, 1 / 2.4) - 0.055;
                r = r << 8 | (int) Math.round(x * 255);
            }
            return r;
        }

        static int packUnit(float x, float y, float z) {
            return Math.round((x + 1) / 2 * 255) << 16 | Math.round((y + 1) / 2 * 255) << 8 | Math.round((z + 1) / 2 * 255);
        }
    }
}
