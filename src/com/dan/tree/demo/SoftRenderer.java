package com.dan.tree.demo;

import com.dan.tree.TreeAsset;
import com.dan.tree.TreeMesh;
import com.dan.tree.TreeSeason;
import com.dan.tree.TreeSpecies;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Einfacher Renderer auf dem Prozessor für die Demos und Selbsttests: Tiefenpuffer, Farbe je Punkt
 * (Sonne, Himmel, Durchscheinen der Blätter), Himmel als Verlauf, Boden mit weichem Schatten unter
 * den Kronen. Ohne JOGL, damit Bilder auch ohne Grafikkarte entstehen.
 */
public final class SoftRenderer {

    public int width, height;
    public BufferedImage image;
    private int[] rgb;
    private float[] depth;
    private float[] fr, fg, fb;

    // Kamera
    private final float[] vp = new float[16];
    private float ex, ey, ez;

    // Licht
    public float[] sunDir = norm(new float[]{-0.45f, 0.75f, 0.35f});   // zur Sonne
    public float[] sun = {2.6f, 2.45f, 2.2f};
    public float[] sky = {0.45f, 0.55f, 0.75f};
    public float[] groundBounce = {0.12f, 0.11f, 0.08f};
    public float exposure = 1.0f;

    /** Schattenflecken unter Bäumen: x, z, Radius, Höhe der Kronenmitte */
    private float[] blobs = new float[0];

    public void resize(int w, int h) {
        if (w == width && h == height && image != null) return;
        width = w;
        height = h;
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        depth = new float[w * h];
        fr = new float[w * h];
        fg = new float[w * h];
        fb = new float[w * h];
    }

    /** Kamera: Auge, Ziel, senkrechter Blickwinkel in Grad. */
    public void camera(float[] eye, float[] target, float fovDeg) {
        ex = eye[0];
        ey = eye[1];
        ez = eye[2];
        float fx = target[0] - eye[0], fy = target[1] - eye[1], fz = target[2] - eye[2];
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl;
        fy /= fl;
        fz /= fl;
        float sx = fy * 0 - fz * 1, sy = fz * 0 - fx * 0, sz = fx * 1 - fy * 0;     // f × up
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= sl;
        sy /= sl;
        sz /= sl;
        float ux = sy * fz - sz * fy, uy = sz * fx - sx * fz, uz = sx * fy - sy * fx;
        float[] view = {
                sx, ux, -fx, 0,
                sy, uy, -fy, 0,
                sz, uz, -fz, 0,
                -(sx * ex + sy * ey + sz * ez), -(ux * ex + uy * ey + uz * ez), fx * ex + fy * ey + fz * ez, 1};
        float f = (float) (1 / Math.tan(Math.toRadians(fovDeg) / 2));
        float aspect = width / (float) height;
        float near = 0.1f, far = 2000f;
        float[] proj = {
                f / aspect, 0, 0, 0,
                0, f, 0, 0,
                0, 0, (far + near) / (near - far), -1,
                0, 0, 2 * far * near / (near - far), 0};
        mul(proj, view, vp);
    }

    /** Spalten-Matrix a·b (wie OpenGL). */
    static void mul(float[] a, float[] b, float[] out) {
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++) {
                float s = 0;
                for (int k = 0; k < 4; k++) s += a[k * 4 + r] * b[c * 4 + k];
                out[c * 4 + r] = s;
            }
    }

    public void shadows(float[] xzrh) {
        blobs = xzrh;
    }

    /** Himmel und Tiefenpuffer leeren. */
    public void clear() {
        Arrays.fill(depth, Float.MAX_VALUE);
        IntStream.range(0, height).parallel().forEach(y -> {
            float t = y / (float) height;
            float r = 0.55f + 0.25f * t, g = 0.68f + 0.18f * t, b = 0.86f + 0.06f * t;
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                fr[i] = r;
                fg[i] = g;
                fb[i] = b;
            }
        });
    }

    /** Boden als Gitter um (cx, cz), Seitenlänge size, Farbe linear. */
    public void ground(float cx, float cz, float size, float[] color, float snow) {
        int n = 48;
        float[] pos = new float[(n + 1) * (n + 1) * 3];
        float[] col = new float[pos.length];
        for (int j = 0; j <= n; j++)
            for (int i = 0; i <= n; i++) {
                int k = (j * (n + 1) + i) * 3;
                float x = cx + (i / (float) n - 0.5f) * size, z = cz + (j / (float) n - 0.5f) * size;
                pos[k] = x;
                pos[k + 1] = 0;
                pos[k + 2] = z;
                float shade = 1;
                for (int b = 0; b < blobs.length; b += 4) {
                    float h = blobs[b + 3] / Math.max(sunDir[1], 0.2f);
                    float d = (float) Math.hypot(x - blobs[b] + sunDir[0] * h, z - blobs[b + 1] + sunDir[2] * h);
                    float r = blobs[b + 2];
                    shade *= 1 - 0.55f * smooth(r * 1.15f, r * 0.4f, d);
                }
                float light = sunDir[1] * shade;
                float var = 0.92f + 0.08f * (float) (Math.sin(x * 0.37 + Math.sin(z * 0.23) * 2) * Math.sin(z * 0.31 + x * 0.11));
                for (int e = 0; e < 3; e++) {
                    float c = color[e] * var;
                    c = c + (0.8f - c) * snow;
                    col[k + e] = c * (sun[e] * light + sky[e] * 0.9f);
                }
            }
        int[] tri = new int[n * n * 6];
        int t = 0;
        for (int j = 0; j < n; j++)
            for (int i = 0; i < n; i++) {
                int a = j * (n + 1) + i, b = a + 1, c = a + n + 1, d = c + 1;
                tri[t++] = a; tri[t++] = c; tri[t++] = b;
                tri[t++] = b; tri[t++] = c; tri[t++] = d;
            }
        raster(project(pos, pos.length / 3), col, tri, tri.length / 3);
    }

    /**
     * Ein Baum. pos: verschobene Punkte im Baumraum (oder null = Ruhelage); Fuß und Drehung in der Welt.
     */
    public void tree(TreeMesh m, float[] pos, TreeSpecies sp, TreeAsset a, TreeSeason.Look look,
                     float x, float y, float z, float yaw, float scale) {
        int n = m.vertices;
        float[] world = new float[n * 3];
        float[] col = new float[n * 3];
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        float[] v = m.v;
        boolean[] hidden = new boolean[n];
        IntStream.range(0, (n + 4095) / 4096).parallel().forEach(chunk -> {
            float[] base = new float[3];
            int end = Math.min(n, (chunk + 1) * 4096);
            for (int i = chunk * 4096; i < end; i++) {
                int b = i * TreeMesh.STRIDE;
                float px = pos != null ? pos[i * 3] : v[b], py = pos != null ? pos[i * 3 + 1] : v[b + 1], pz = pos != null ? pos[i * 3 + 2] : v[b + 2];
                float wx = x + (px * c - pz * s) * scale, wy = y + py * scale, wz = z + (px * s + pz * c) * scale;
                world[i * 3] = wx;
                world[i * 3 + 1] = wy;
                world[i * 3 + 2] = wz;
                if (!m.color(i, sp, a, look, base)) {
                    hidden[i] = true;
                    continue;
                }
                float nx = v[b + 3] * c - v[b + 5] * s, ny = v[b + 4], nz = v[b + 3] * s + v[b + 5] * c;
                boolean leaf = m.leaf(i);
                float vx = ex - wx, vy = ey - wy, vz = ez - wz;
                float trans = 0;
                if (leaf) {
                    float dv = nx * vx + ny * vy + nz * vz;
                    float ndl0 = nx * sunDir[0] + ny * sunDir[1] + nz * sunDir[2];
                    if (dv < 0) { nx = -nx; ny = -ny; nz = -nz; ndl0 = -ndl0; }
                    // Gegenlicht: Sonne hinter dem Blatt
                    trans = Math.max(0, -ndl0) * 0.45f;
                }
                float ndl = nx * sunDir[0] + ny * sunDir[1] + nz * sunDir[2];
                float diff = leaf ? Math.max(0, ndl * 0.8f + 0.2f) : Math.max(0, ndl);
                float ao = m.ao(i);
                float hemi = 0.5f + 0.5f * ny;
                for (int e = 0; e < 3; e++) {
                    float light = sun[e] * (diff + trans) * (0.35f + 0.65f * ao) + (sky[e] * hemi + groundBounce[e] * (1 - hemi)) * (0.4f + 0.6f * ao);
                    col[i * 3 + e] = base[e] * light;
                }
            }
        });
        float[] scr = project(world, n);
        // verborgene Blätter: Dreieck auslassen
        int[] tri = m.tri;
        int nt = m.triangles;
        int[] keep = new int[nt * 3];
        int k = 0;
        for (int i = 0; i < nt; i++) {
            int i0 = tri[i * 3], i1 = tri[i * 3 + 1], i2 = tri[i * 3 + 2];
            if (hidden[i0] || hidden[i1] || hidden[i2]) continue;
            keep[k++] = i0;
            keep[k++] = i1;
            keep[k++] = i2;
        }
        raster(scr, col, keep, k / 3);
    }

    /** Welt → Bildpunkte: x, y, z (Tiefe), w (≤ 0: hinter der Kamera). */
    private float[] project(float[] p, int n) {
        float[] out = new float[n * 4];
        IntStream.range(0, (n + 4095) / 4096).parallel().forEach(chunk -> {
            int end = Math.min(n, (chunk + 1) * 4096);
            for (int i = chunk * 4096; i < end; i++) {
                float x = p[i * 3], y = p[i * 3 + 1], z = p[i * 3 + 2];
                float cx = vp[0] * x + vp[4] * y + vp[8] * z + vp[12];
                float cy = vp[1] * x + vp[5] * y + vp[9] * z + vp[13];
                float cz = vp[2] * x + vp[6] * y + vp[10] * z + vp[14];
                float cw = vp[3] * x + vp[7] * y + vp[11] * z + vp[15];
                out[i * 4 + 3] = cw;
                if (cw <= 0.05f) continue;
                out[i * 4] = (cx / cw * 0.5f + 0.5f) * width;
                out[i * 4 + 1] = (1 - (cy / cw * 0.5f + 0.5f)) * height;
                out[i * 4 + 2] = cz / cw;
            }
        });
        return out;
    }

    /** Dreiecke in waagerechten Streifen parallel zeichnen. */
    private void raster(float[] s, float[] col, int[] tri, int nt) {
        int bands = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
        int bh = (height + bands - 1) / bands;
        IntStream.range(0, bands).parallel().forEach(band -> {
            int y0 = band * bh, y1 = Math.min(height, y0 + bh);
            for (int t = 0; t < nt; t++) {
                int a = tri[t * 3], b = tri[t * 3 + 1], c = tri[t * 3 + 2];
                if (s[a * 4 + 3] <= 0.05f || s[b * 4 + 3] <= 0.05f || s[c * 4 + 3] <= 0.05f) continue;
                float ay = s[a * 4 + 1], by = s[b * 4 + 1], cy = s[c * 4 + 1];
                float minY = Math.min(ay, Math.min(by, cy)), maxY = Math.max(ay, Math.max(by, cy));
                if (maxY < y0 || minY >= y1) continue;
                fill(s, col, a, b, c, y0, y1);
            }
        });
    }

    private void fill(float[] s, float[] col, int a, int b, int c, int y0, int y1) {
        float ax = s[a * 4], ay = s[a * 4 + 1], az = s[a * 4 + 2];
        float bx = s[b * 4], by = s[b * 4 + 1], bz = s[b * 4 + 2];
        float cx = s[c * 4], cy = s[c * 4 + 1], cz = s[c * 4 + 2];
        float area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (Math.abs(area) < 1e-9f) {
            return;
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx))));
        int maxX = Math.min(width - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int minY = Math.max(y0, (int) Math.floor(Math.min(ay, Math.min(by, cy))));
        int maxY = Math.min(y1 - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        if (minX > maxX || minY > maxY) return;
        float inv = 1 / area;
        // sehr kleine Dreiecke: ein Punkt in der Mitte, damit feine Zweige nicht verschwinden
        boolean tiny = maxX - minX <= 1 && maxY - minY <= 1;
        if (tiny) {
            int px = (int) ((ax + bx + cx) / 3), py = (int) ((ay + by + cy) / 3);
            if (px < 0 || px >= width || py < y0 || py >= y1) return;
            float z = (az + bz + cz) / 3;
            int i = py * width + px;
            if (z < depth[i]) {
                depth[i] = z;
                fr[i] = (col[a * 3] + col[b * 3] + col[c * 3]) / 3;
                fg[i] = (col[a * 3 + 1] + col[b * 3 + 1] + col[c * 3 + 1]) / 3;
                fb[i] = (col[a * 3 + 2] + col[b * 3 + 2] + col[c * 3 + 2]) / 3;
            }
            return;
        }
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = ((bx - px) * (cy - py) - (by - py) * (cx - px)) * inv;
                float w1 = ((cx - px) * (ay - py) - (cy - py) * (ax - px)) * inv;
                float w2 = 1 - w0 - w1;
                if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                float z = w0 * az + w1 * bz + w2 * cz;
                int i = y * width + x;
                if (z >= depth[i]) continue;
                depth[i] = z;
                fr[i] = w0 * col[a * 3] + w1 * col[b * 3] + w2 * col[c * 3];
                fg[i] = w0 * col[a * 3 + 1] + w1 * col[b * 3 + 1] + w2 * col[c * 3 + 1];
                fb[i] = w0 * col[a * 3 + 2] + w1 * col[b * 3 + 2] + w2 * col[c * 3 + 2];
            }
        }
    }

    /** Linear → Bild (Belichtung, Reinhard, sRGB). */
    public BufferedImage finish() {
        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                rgb[i] = (tone(fr[i]) << 16) | (tone(fg[i]) << 8) | tone(fb[i]);
            }
        });
        return image;
    }

    private int tone(float c) {
        c *= exposure;
        c = c / (1 + c * 0.25f);
        c = (float) Math.pow(Math.max(0, c), 1 / 2.2);
        return Math.min(255, (int) (c * 255 + 0.5f));
    }

    static float smooth(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    static float[] norm(float[] v) {
        float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / l, v[1] / l, v[2] / l};
    }
}
