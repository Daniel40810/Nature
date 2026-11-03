package com.dan.ground;

import com.dan.tree.TreeAsset;

import java.util.Arrays;

/**
 * Baut ein statisches Netz (Punkte, Normalen, UVs, Dreiecke) und macht daraus ein {@link TreeAsset} ohne Laub:
 * das Netz ist der „Stamm“ mit einem Gelenk am Fuß, die Textur die „Rinde“. So zeichnen es die Renderer aus
 * {@code com.dan.tree} wie ein Gelände. Dreiecke gegen den Uhrzeigersinn, von der Seite der Normalen aus gesehen.
 */
public final class MeshBuilder {

    private float[] pos = new float[3 * 256], nrm = new float[3 * 256], uv = new float[2 * 256];
    private int[] tri = new int[3 * 256];
    private int nv, nt;

    public int vertices() {
        return nv;
    }

    public int triangles() {
        return nt;
    }

    /** Neuer Punkt; liefert seinen Index. */
    public int vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        if ((nv + 1) * 3 > pos.length) {
            pos = Arrays.copyOf(pos, pos.length * 2);
            nrm = Arrays.copyOf(nrm, nrm.length * 2);
            uv = Arrays.copyOf(uv, uv.length * 2);
        }
        pos[nv * 3] = x;
        pos[nv * 3 + 1] = y;
        pos[nv * 3 + 2] = z;
        nrm[nv * 3] = nx;
        nrm[nv * 3 + 1] = ny;
        nrm[nv * 3 + 2] = nz;
        uv[nv * 2] = u;
        uv[nv * 2 + 1] = v;
        return nv++;
    }

    public void triangle(int a, int b, int c) {
        if ((nt + 1) * 3 > tri.length) tri = Arrays.copyOf(tri, tri.length * 2);
        tri[nt * 3] = a;
        tri[nt * 3 + 1] = b;
        tri[nt * 3 + 2] = c;
        nt++;
    }

    /** Viereck a-b-c-d (gegen den Uhrzeigersinn) als zwei Dreiecke. */
    public void quad(int a, int b, int c, int d) {
        triangle(a, b, c);
        triangle(a, c, d);
    }

    /**
     * Raster aus (cols+1) × (rows+1) Punkten, die {@code at} liefert (Ort und UV je Gitterpunkt); die Normalen
     * kommen aus den Nachbarn. Die Fläche zeigt zur Seite von (Richtung der Zeilen × Richtung der Spalten), auf dem Boden mit
     * Spalten nach +x und Zeilen nach +z also nach oben.
     */
    public void grid(int cols, int rows, GridPoint at) {
        int base = nv;
        float[] p = new float[5];
        for (int r = 0; r <= rows; r++)
            for (int c = 0; c <= cols; c++) {
                at.at(c, r, p);
                vertex(p[0], p[1], p[2], 0, 0, 0, p[3], p[4]);
            }
        int t0 = nt;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int a = base + r * (cols + 1) + c, b = a + 1, d = a + cols + 1, e = d + 1;
                // abwechselnde Diagonale: keine Vorzugsrichtung im Relief
                if (((r + c) & 1) == 0) {
                    triangle(a, d, e);
                    triangle(a, e, b);
                } else {
                    triangle(a, d, b);
                    triangle(b, d, e);
                }
            }
        normals(base, nv, t0, nt);
    }

    /** Ort (x, y, z) und UV (u, v) eines Gitterpunkts nach out[0..4]. */
    @FunctionalInterface
    public interface GridPoint {
        void at(int col, int row, float[] out);
    }

    /** Normalen der Punkte from..to-1 aus den Dreiecken tFrom..tTo-1 (nach Fläche gewichtet). */
    public void normals(int from, int to, int tFrom, int tTo) {
        Arrays.fill(nrm, from * 3, to * 3, 0);
        for (int t = tFrom; t < tTo; t++) {
            int a = tri[t * 3], b = tri[t * 3 + 1], c = tri[t * 3 + 2];
            float ux = pos[b * 3] - pos[a * 3], uy = pos[b * 3 + 1] - pos[a * 3 + 1], uz = pos[b * 3 + 2] - pos[a * 3 + 2];
            float vx = pos[c * 3] - pos[a * 3], vy = pos[c * 3 + 1] - pos[a * 3 + 1], vz = pos[c * 3 + 2] - pos[a * 3 + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            for (int k : new int[]{a, b, c}) {
                if (k < from || k >= to) continue;
                nrm[k * 3] += nx;
                nrm[k * 3 + 1] += ny;
                nrm[k * 3 + 2] += nz;
            }
        }
        for (int i = from; i < to; i++) normalize(i);
    }

    /** Alle Normalen neu aus den Dreiecken. */
    public void normals() {
        normals(0, nv, 0, nt);
    }

    private void normalize(int i) {
        float x = nrm[i * 3], y = nrm[i * 3 + 1], z = nrm[i * 3 + 2];
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        if (l < 1e-12f) {
            nrm[i * 3] = 0;
            nrm[i * 3 + 1] = 1;
            nrm[i * 3 + 2] = 0;
        } else {
            nrm[i * 3] = x / l;
            nrm[i * 3 + 1] = y / l;
            nrm[i * 3 + 2] = z / l;
        }
    }

    /** Ein anderes Netz anhängen, um yaw (rad, wie {@code TreeGl}: x' = x·cos − z·sin) gedreht und verschoben. */
    public void append(MeshBuilder o, float yaw, float dx, float dy, float dz) {
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        int base = nv;
        for (int i = 0; i < o.nv; i++) {
            float x = o.pos[i * 3], z = o.pos[i * 3 + 2], nx = o.nrm[i * 3], nz = o.nrm[i * 3 + 2];
            vertex(x * c - z * s + dx, o.pos[i * 3 + 1] + dy, x * s + z * c + dz,
                    nx * c - nz * s, o.nrm[i * 3 + 1], nx * s + nz * c, o.uv[i * 2], o.uv[i * 2 + 1]);
        }
        for (int t = 0; t < o.nt; t++) triangle(base + o.tri[t * 3], base + o.tri[t * 3 + 1], base + o.tri[t * 3 + 2]);
    }

    /** Punkt i: Ort nach out[0..2]. */
    public void position(int i, float[] out) {
        out[0] = pos[i * 3];
        out[1] = pos[i * 3 + 1];
        out[2] = pos[i * 3 + 2];
    }

    /** Punkt i: Normale nach out[0..2]. */
    public void normal(int i, float[] out) {
        out[0] = nrm[i * 3];
        out[1] = nrm[i * 3 + 1];
        out[2] = nrm[i * 3 + 2];
    }

    /** Punkt i verschieben. */
    public void move(int i, float x, float y, float z) {
        pos[i * 3] = x;
        pos[i * 3 + 1] = y;
        pos[i * 3 + 2] = z;
    }

    /** Kleinste und größte Werte: {minX, minY, minZ, maxX, maxY, maxZ}. */
    public float[] bounds() {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < nv; i++)
            for (int k = 0; k < 3; k++) {
                b[k] = Math.min(b[k], pos[i * 3 + k]);
                b[k + 3] = Math.max(b[k + 3], pos[i * 3 + k]);
            }
        return b;
    }

    /**
     * Als Asset: Netz als Stamm ohne Laub, ein Gelenk am Fuß, Textur {@code texture} (neben der Klasse der Art
     * oder mit {@code TreeSpecies.putTexture} bereitgestellt), Grundfarbe linear.
     */
    public TreeAsset toAsset(String name, String source, String texture, float[] color) {
        TreeAsset a = new TreeAsset();
        a.name = name;
        a.source = source;
        a.gustAttenuation = 0;
        a.groundCover = true;
        a.barkColor = color.clone();
        a.leafColor = new float[]{0.06f, 0.12f, 0.03f};
        a.jointParent = new int[]{-1};
        a.jointPos = new float[3];
        a.jointRadius = new float[]{0.1f};
        TreeAsset.Mesh m = new TreeAsset.Mesh();
        m.pos = Arrays.copyOf(pos, nv * 3);
        m.nrm = Arrays.copyOf(nrm, nv * 3);
        m.joint = new short[nv * 2];
        m.weight = new float[nv * 2];
        for (int i = 0; i < nv; i++) m.weight[i * 2] = 1;
        m.tri = Arrays.copyOf(tri, nt * 3);
        a.trunk = m;
        a.trunkUv = Arrays.copyOf(uv, nv * 2);
        a.twigs = new TreeAsset.Twig[0];
        a.instProto = new short[0];
        a.instMatrix = new float[0];
        a.instJoint = new short[0];
        a.instWeight = new float[0];
        a.barkTexture = texture == null ? "" : texture;
        a.leafTexture = "";
        return a;
    }
}
