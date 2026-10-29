package com.dan.tree;

import java.util.Arrays;

/**
 * Backt aus einem {@link TreeAsset} ein {@link TreeMesh} in einer {@link TreeLod}-Stufe:
 * Stamm (Original oder Röhren aus dem Skelett), Zweigholz (Röhren aus dem Knotengraph),
 * Blätter (Sechseck, Raute oder Dreieck, ausgedünnt und vergrößert) oder in der Ferne eine Hülle.
 * Je Punkt kommen die Werte für den Wind dazu (siehe {@link TreeSkeleton}).
 */
public final class TreeBaker {

    private TreeBaker() {}

    public static TreeMesh bake(TreeAsset a, TreeLod lod) {
        return new Job(a, lod).run();
    }

    /** Hash 0..1 aus zwei Zahlen, reproduzierbar. */
    public static float hash(int a, int b) {
        int h = a * 0x27d4eb2d ^ (b + 0x165667b1) * 0x9e3779b1;
        h ^= h >>> 15;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return (h >>> 8) / (float) (1 << 24);
    }

    // ------------------------------------------------------------------

    private static final class Job {
        final TreeAsset a;
        final TreeLod lod;
        final TreeSkeleton sk;
        /** Reichweite je Hauptast samt der Zweige, die an ihm hängen */
        final float[] reach;

        float[] v = new float[1 << 16];
        int nv;
        int[] t = new int[1 << 16];
        int nt;
        int leaves;

        Job(TreeAsset a, TreeLod lod) {
            this.a = a;
            this.lod = lod;
            this.sk = new TreeSkeleton(a);
            reach = Arrays.copyOf(sk.limbReach, sk.n);
            float[] r = new float[sk.n];
            for (int j = 0; j < sk.n; j++) if (sk.limb[j] >= 0) r[sk.limb[j]] = Math.max(r[sk.limb[j]], sk.limbReach[j]);
            for (int k = 0; k < a.instances(); k++) {
                int j = dominant(a.instJoint, a.instWeight, k);
                if (sk.limb[j] < 0) continue;
                float len = a.twigs[a.instProto[k]].length * a.scale(k);
                r[sk.limb[j]] = Math.max(r[sk.limb[j]], blendLimbAlong(k) + len);
            }
            for (int j = 0; j < sk.n; j++) reach[j] = sk.limb[j] >= 0 ? Math.max(r[sk.limb[j]], 1e-3f) : 0;
        }

        TreeMesh run() {
            if (lod.trunkOriginal) trunkOriginal();
            else trunkTubes(lod.trunkMinRadius, lod.trunkSides);
            if (lod.twigWood) for (int k = 0; k < a.instances(); k++) twigWood(k);
            int bark = nt;
            if (lod.silhouette) hull();
            else if (lod.leafKeep > 0) for (int k = 0; k < a.instances(); k++) twigLeaves(k);
            float height = 0, radius = 0;
            for (int i = 0; i < nv; i++) {
                int b = i * TreeMesh.STRIDE;
                height = Math.max(height, v[b + 1]);
                radius = Math.max(radius, (float) Math.hypot(v[b], v[b + 2]));
            }
            occlusion(height);
            return new TreeMesh(Arrays.copyOf(v, nv * TreeMesh.STRIDE), nv, Arrays.copyOf(t, nt * 3), nt, bark, leaves,
                    height, radius, lod);
        }

        // ---------------------------------------------------------- Wind je Gelenk

        float limbAmp(int j) {
            if (sk.limb[j] < 0) return 0;
            float u = Math.min(sk.limbAlong[j] / reach[j], 1);
            return u * u * reach[j];
        }

        float twigAmp(int j) {
            if (sk.order[j] < 2) return 0;
            float u = Math.min(sk.along[j] / sk.branchLength[j], 1);
            return u * u * sk.branchLength[j];
        }

        float limbPhase(int j) {
            return sk.limb[j] < 0 ? 0 : hash(sk.limb[j], 11);
        }

        float twigPhase(int j) {
            return sk.order[j] < 2 ? hash(sk.branch[j], 23) : hash(sk.branch[j], 29);
        }

        float blendLimbAlong(int k) {
            float s = 0;
            for (int q = 0; q < 2; q++) s += a.instWeight[k * 2 + q] * sk.limbAlong[a.instJoint[k * 2 + q]];
            return s;
        }

        static int dominant(short[] joint, float[] weight, int i) {
            return weight[i * 2] >= weight[i * 2 + 1] ? joint[i * 2] : joint[i * 2 + 1];
        }

        // ---------------------------------------------------------- Stamm

        void trunkOriginal() {
            TreeAsset.Mesh m = a.trunk;
            int base = nv;
            for (int i = 0; i < m.vertices(); i++) {
                int j0 = m.joint[i * 2], j1 = m.joint[i * 2 + 1];
                float w0 = m.weight[i * 2], w1 = m.weight[i * 2 + 1];
                float ws = w0 + w1;
                if (ws <= 0) { w0 = 1; w1 = 0; ws = 1; }
                w0 /= ws;
                w1 /= ws;
                int d = w0 >= w1 ? j0 : j1;
                vertex(m.pos[i * 3], m.pos[i * 3 + 1], m.pos[i * 3 + 2],
                        m.nrm[i * 3], m.nrm[i * 3 + 1], m.nrm[i * 3 + 2],
                        w0 * limbAmp(j0) + w1 * limbAmp(j1), w0 * twigAmp(j0) + w1 * twigAmp(j1), 0,
                        limbPhase(d), twigPhase(d), 0, hash(d, 3));
            }
            for (int i = 0; i < m.tri.length; i += 3) triangle(base + m.tri[i], base + m.tri[i + 1], base + m.tri[i + 2]);
        }

        void trunkTubes(float minRadius, int sides) {
            boolean[] use = new boolean[sk.n];
            for (int j = 0; j < sk.n; j++)
                use[j] = sk.parent[j] < 0 || sk.radius[j] >= minRadius && sk.order[j] <= lod.trunkMaxOrder;
            int[] anc = chains(sk.parent, sk.pos, use, lod.chainAngle, lod.chainLength);
            float[] p0 = new float[3], p1 = new float[3];
            for (int j = 0; j < sk.n; j++) {
                int p = anc[j];
                if (p < 0) continue;
                float seg = sk.dist(p, j);
                if (seg < 1e-4f) continue;
                float r1 = sk.radius[j];
                float r0 = Math.min(sk.radius[p], r1 * 1.3f);
                copy(sk.pos, p * 3, p0);
                copy(sk.pos, j * 3, p1);
                // Anfang: Werte des Gelenks j, um die Strecke zurückgenommen
                float la0 = sk.limb[j] < 0 ? 0 : sq(Math.max(0, sk.limbAlong[j] - seg) / reach[j]) * reach[j];
                float ta0 = sk.order[j] < 2 ? 0 : sq(Math.max(0, sk.along[j] - seg) / sk.branchLength[j]) * sk.branchLength[j];
                tube(p0, r0, la0, ta0, p1, r1, limbAmp(j), twigAmp(j), limbPhase(j), twigPhase(j), sides, hash(j, 3));
            }
        }

        static float sq(float x) {
            x = Math.min(x, 1);
            return x * x;
        }

        // ---------------------------------------------------------- Zweige

        /** Werte am Ansatz der Zweig-Instanz k. */
        final class Base {
            final float limbAlong, reachK, twigAlong, twigLen, limbPhase, twigPhase, scale, length;
            final boolean onLimb, onTwig;

            Base(int k) {
                int d = dominant(a.instJoint, a.instWeight, k);
                scale = a.scale(k);
                length = a.twigs[a.instProto[k]].length * scale;
                onLimb = sk.limb[d] >= 0;
                onTwig = sk.order[d] >= 2;
                float la = 0, ta = 0;
                for (int q = 0; q < 2; q++) {
                    int j = a.instJoint[k * 2 + q];
                    float w = a.instWeight[k * 2 + q];
                    la += w * (sk.limb[j] >= 0 ? sk.limbAlong[j] : 0);
                    ta += w * (sk.order[j] >= 2 ? sk.along[j] : 0);
                }
                limbAlong = la;
                reachK = onLimb ? reach[d] : 0;
                twigAlong = ta;
                twigLen = onTwig ? sk.branchLength[d] : 0;
                limbPhase = limbPhase(d);
                twigPhase = onTwig ? twigPhase(d) : hash(k, 31);
            }

            /** Hauptast-Ausschlag in d Metern Weg vom Ansatz des Zweigs */
            float limb(float dist) {
                if (!onLimb) return 0;
                return sq((limbAlong + dist) / reachK) * reachK;
            }

            float twig(float dist) {
                float own = sq(dist / Math.max(length, 1e-3f)) * length;
                if (!onTwig) return own;
                return sq(twigAlong / twigLen) * twigLen + own;
            }
        }

        /** Weg vom Ansatz je Knoten (vor dem Maßstab). */
        static float[] pathLength(TreeAsset.Twig tw) {
            float[] pd = new float[tw.nodes()];
            for (int i = 0; i < tw.nodes(); i++) {
                int p = tw.nodeParent[i];
                if (p < 0) {
                    pd[i] = len(tw.nodePos, i * 3, 0, 0, 0);
                } else {
                    pd[i] = pd[p] + len(tw.nodePos, i * 3, tw.nodePos[p * 3], tw.nodePos[p * 3 + 1], tw.nodePos[p * 3 + 2]);
                }
            }
            return pd;
        }

        float[][] paths;

        float[] paths(int proto) {
            if (paths == null) paths = new float[a.twigs.length][];
            if (paths[proto] == null) paths[proto] = pathLength(a.twigs[proto]);
            return paths[proto];
        }

        int[][] twigChains;

        void twigWood(int k) {
            int proto = a.instProto[k];
            TreeAsset.Twig tw = a.twigs[proto];
            if (twigChains == null) twigChains = new int[a.twigs.length][];
            if (twigChains[proto] == null) {
                boolean[] use = new boolean[tw.nodes()];
                for (int i = 0; i < use.length; i++) use[i] = tw.nodeRadius[i] >= lod.twigMinRadius || tw.nodeParent[i] < 0;
                twigChains[proto] = chains(tw.nodeParent, tw.nodePos, use, lod.twigChainAngle, 1e9f);
            }
            int[] anc = twigChains[proto];
            Base b = new Base(k);
            float[] pd = paths(proto);
            float[] p0 = new float[3], p1 = new float[3];
            for (int i = 0; i < tw.nodes(); i++) {
                int p = anc[i];
                if (p < 0) continue;
                a.transform(k, tw.nodePos, p * 3, p0, 0);
                a.transform(k, tw.nodePos, i * 3, p1, 0);
                float r1 = tw.nodeRadius[i] * b.scale, r0 = Math.min(tw.nodeRadius[p] * b.scale, r1 * 1.3f);
                float d0 = pd[p] * b.scale, d1 = pd[i] * b.scale;
                tube(p0, r0, b.limb(d0), b.twig(d0), p1, r1, b.limb(d1), b.twig(d1),
                        b.limbPhase, b.twigPhase, lod.twigSides, hash(k, i));
            }
        }

        int[][] leafNode;

        int leafNode(int proto, int leaf) {
            if (leafNode == null) leafNode = new int[a.twigs.length][];
            if (leafNode[proto] == null) {
                TreeAsset.Twig tw = a.twigs[proto];
                int[] ln = new int[tw.leaves()];
                for (int l = 0; l < tw.leaves(); l++) {
                    float best = Float.MAX_VALUE;
                    for (int i = 0; i < tw.nodes(); i++) {
                        float d = len(tw.nodePos, i * 3, tw.leafBase[l * 3], tw.leafBase[l * 3 + 1], tw.leafBase[l * 3 + 2]);
                        if (d < best) { best = d; ln[l] = i; }
                    }
                }
                leafNode[proto] = ln;
            }
            return leafNode[proto][leaf];
        }

        void twigLeaves(int k) {
            int proto = a.instProto[k];
            TreeAsset.Twig tw = a.twigs[proto];
            Base b = new Base(k);
            float[] pd = paths(proto);
            float grow = (float) Math.sqrt(1 / Math.max(lod.leafKeep, 1e-3f));
            int ring = tw.ring;
            float[] base = new float[3], c = new float[3], n = new float[3], rp = new float[ring * 3];
            float[] q = new float[3];
            for (int l = 0; l < tw.leaves(); l++) {
                if (lod.leafKeep < 1 && hash(k * 131 + l, 7) >= lod.leafKeep) continue;
                a.transform(k, tw.leafBase, l * 3, base, 0);
                a.transform(k, tw.leafCenter, l * 3, c, 0);
                a.rotate(k, tw.leafNormal, l * 3, n, 0);
                normalize(n);
                for (int r = 0; r < ring; r++) a.transform(k, tw.leafRing, (l * ring + r) * 3, rp, r * 3);
                if (grow != 1) {
                    for (int r = 0; r < ring; r++)
                        for (int e = 0; e < 3; e++) rp[r * 3 + e] = c[e] + (rp[r * 3 + e] - c[e]) * grow;
                    for (int e = 0; e < 3; e++) base[e] = c[e] + (base[e] - c[e]) * grow;
                }
                int node = leafNode(proto, l);
                float dist0 = pd[node] * b.scale;
                float rand = hash(k, l * 7 + 1);
                float size = 1e-4f;
                for (int r = 0; r < ring; r++) size = Math.max(size, len(rp, r * 3, base[0], base[1], base[2]));

                // Auswahl der Randpunkte nach Form; Ring 6: 0 Spitze, 3 Stiel
                int[] pick = switch (lod.leafShape) {
                    case 3 -> new int[]{0, 2, 4};
                    case 4 -> new int[]{0, -1, 3, -2};       // −1: Mitte 1/2, −2: Mitte 4/5
                    default -> null;
                };
                int first = nv;
                if (pick == null) {
                    leafVertex(c, n, c, base, size, dist0, b, rand);
                    for (int r = 0; r < ring; r++) {
                        copy(rp, r * 3, q);
                        leafVertex(q, n, c, base, size, dist0, b, rand);
                    }
                    for (int r = 0; r < ring; r++) triangle(first, first + 1 + r, first + 1 + (r + 1) % ring);
                } else {
                    for (int s : pick) {
                        if (s >= 0) copy(rp, s * 3, q);
                        else {
                            int i0 = s == -1 ? 1 : 4, i1 = s == -1 ? 2 : 5;
                            for (int e = 0; e < 3; e++) q[e] = (rp[i0 * 3 + e] + rp[i1 * 3 + e]) * 0.5f;
                        }
                        leafVertex(q, n, c, base, size, dist0, b, rand);
                    }
                    triangle(first, first + 1, first + 2);
                    if (pick.length == 4) triangle(first, first + 2, first + 3);
                }
                leaves++;
            }
        }

        void leafVertex(float[] p, float[] n, float[] c, float[] base, float size, float dist0, Base b, float rand) {
            float d = len(p, 0, base[0], base[1], base[2]);
            float dist = dist0 + d;
            // Normale leicht nach außen gewölbt, damit das Laub weich schattiert
            float nx = n[0] + (p[0] - c[0]) / size * 0.35f;
            float ny = n[1] + (p[1] - c[1]) / size * 0.35f;
            float nz = n[2] + (p[2] - c[2]) / size * 0.35f;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            vertex(p[0], p[1], p[2], nx / l, ny / l, nz / l, b.limb(dist), b.twig(dist),
                    Math.min(1, d / size), b.limbPhase, b.twigPhase, 1, rand);
        }

        // ---------------------------------------------------------- Hülle

        void hull() {
            int S = lod.hullSectors, B = lod.hullBands;
            // Blattmitten aller Zweige im Baumraum
            float cx = 0, cz = 0, y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
            int count = 0;
            float[] q = new float[3];
            for (int k = 0; k < a.instances(); k++) {
                TreeAsset.Twig tw = a.twigs[a.instProto[k]];
                for (int l = 0; l < tw.leaves(); l++) {
                    a.transform(k, tw.leafCenter, l * 3, q, 0);
                    cx += q[0];
                    cz += q[2];
                    y0 = Math.min(y0, q[1]);
                    y1 = Math.max(y1, q[1]);
                    count++;
                }
            }
            if (count == 0) return;
            cx /= count;
            cz /= count;
            float[][] r = new float[B][S];
            float[][] la = new float[B][S];
            for (int k = 0; k < a.instances(); k++) {
                TreeAsset.Twig tw = a.twigs[a.instProto[k]];
                Base b = new Base(k);
                for (int l = 0; l < tw.leaves(); l++) {
                    a.transform(k, tw.leafCenter, l * 3, q, 0);
                    int band = Math.min(B - 1, (int) ((q[1] - y0) / (y1 - y0 + 1e-6f) * B));
                    double ang = Math.atan2(q[2] - cz, q[0] - cx);
                    int sec = Math.floorMod((int) Math.floor((ang / (2 * Math.PI) + 0.5 / S) * S), S);
                    float d = (float) Math.hypot(q[0] - cx, q[2] - cz) + 0.08f;
                    if (d > r[band][sec]) { r[band][sec] = d; la[band][sec] = b.limb(b.length * 0.7f); }
                }
            }
            // Lücken glätten
            for (int band = 0; band < B; band++) {
                float m = 0;
                for (float x : r[band]) m = Math.max(m, x);
                for (int s = 0; s < S; s++) if (r[band][s] <= 0) r[band][s] = m * 0.6f;
            }
            int bottom = nv;
            vertex(cx, y0, cz, 0, -1, 0, 0, 0, 0, 0, 0, 1, 0.5f);
            int[] ring = new int[B];
            for (int band = 0; band < B; band++) {
                ring[band] = nv;
                float y = y0 + (band + 0.5f) / B * (y1 - y0);
                for (int s = 0; s < S; s++) {
                    double ang = 2 * Math.PI * s / S;
                    float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
                    float rr = r[band][s];
                    float ny = (band == 0 ? -0.5f : band == B - 1 ? 0.5f : 0);
                    float nl = (float) Math.sqrt(1 + ny * ny);
                    vertex(cx + ca * rr, y, cz + sa * rr, ca / nl, ny / nl, sa / nl, la[band][s], 0, 0.3f,
                            hash(s, 5), hash(band, s), 1, 0.5f);
                }
            }
            int top = nv;
            vertex(cx, y1 + 0.15f, cz, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0.5f);
            for (int s = 0; s < S; s++) {
                int s1 = (s + 1) % S;
                triangle(bottom, ring[0] + s1, ring[0] + s);
                for (int band = 0; band + 1 < B; band++)
                    quad(ring[band] + s, ring[band] + s1, ring[band + 1] + s1, ring[band + 1] + s);
                triangle(top, ring[B - 1] + s, ring[B - 1] + s1);
            }
            leaves++;
        }

        // ---------------------------------------------------------- Verdeckung

        /** Laub innen und unten dunkler; Holz in der Krone dunkler. Ergebnis im Ganzzahl-Teil von RAND. */
        void occlusion(float height) {
            int B = 16;
            float[] crown = new float[B];
            float cx = sk.pos[0], cz = sk.pos[2];
            for (int i = 0; i < nv; i++) {
                int b = i * TreeMesh.STRIDE;
                if (v[b + TreeMesh.KIND] < 0.5f) continue;
                int band = Math.min(B - 1, (int) (v[b + 1] / Math.max(height, 1e-3f) * B));
                crown[band] = Math.max(crown[band], (float) Math.hypot(v[b] - cx, v[b + 2] - cz));
            }
            float yLeaf = height;
            for (int band = 0; band < B; band++) if (crown[band] > 0) { yLeaf = band * height / B; break; }
            for (int i = 0; i < nv; i++) {
                int b = i * TreeMesh.STRIDE;
                float y = v[b + 1];
                int band = Math.min(B - 1, Math.max(0, (int) (y / Math.max(height, 1e-3f) * B)));
                float r = (float) Math.hypot(v[b] - cx, v[b + 2] - cz);
                float rc = Math.max(crown[band], 0.3f);
                float out = Math.min(1, r / rc);
                float h = Math.max(0, Math.min(1, (y - yLeaf) / Math.max(height - yLeaf, 1e-3f)));
                float ao;
                if (v[b + TreeMesh.KIND] > 0.5f) ao = (0.45f + 0.55f * out) * (0.8f + 0.2f * h);
                else ao = y < yLeaf ? 1 : 0.5f + 0.5f * out;
                if (lod.silhouette && v[b + TreeMesh.KIND] > 0.5f) ao = 0.8f;
                float rand = v[b + TreeMesh.RAND];
                v[b + TreeMesh.RAND] = Math.round(Math.max(0, Math.min(1, ao)) * 100) + Math.min(rand, 0.9999f);
            }
        }

        // ---------------------------------------------------------- Bausteine

        void tube(float[] p0, float r0, float la0, float ta0, float[] p1, float r1, float la1, float ta1,
                  float limbPh, float twigPh, int sides, float rand) {
            float dx = p1[0] - p0[0], dy = p1[1] - p0[1], dz = p1[2] - p0[2];
            float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (l < 1e-6f) return;
            dx /= l;
            dy /= l;
            dz /= l;
            // Basis senkrecht zur Achse
            float ax, ay, az;
            if (Math.abs(dy) < 0.9f) { ax = -dz; ay = 0; az = dx; } else { ax = 1; ay = 0; az = 0; ax -= dx * dx; ay -= dx * dy; az -= dx * dz; }
            float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            ax /= al;
            ay /= al;
            az /= al;
            float bx = dy * az - dz * ay, by = dz * ax - dx * az, bz = dx * ay - dy * ax;
            int first = nv;
            for (int e = 0; e < 2; e++) {
                float[] p = e == 0 ? p0 : p1;
                float r = e == 0 ? r0 : r1;
                for (int s = 0; s < sides; s++) {
                    double ang = 2 * Math.PI * s / sides;
                    float c = (float) Math.cos(ang), sn = (float) Math.sin(ang);
                    float nx = ax * c + bx * sn, ny = ay * c + by * sn, nz = az * c + bz * sn;
                    vertex(p[0] + nx * r, p[1] + ny * r, p[2] + nz * r, nx, ny, nz,
                            e == 0 ? la0 : la1, e == 0 ? ta0 : ta1, 0, limbPh, twigPh, 0, rand);
                }
            }
            for (int s = 0; s < sides; s++) {
                int s1 = (s + 1) % sides;
                quad(first + s, first + s1, first + sides + s1, first + sides + s);
            }
        }

        void vertex(float x, float y, float z, float nx, float ny, float nz, float limb, float twig, float flutter,
                    float limbPh, float twigPh, float kind, float rand) {
            if ((nv + 1) * TreeMesh.STRIDE > v.length) v = Arrays.copyOf(v, v.length * 2);
            int b = nv * TreeMesh.STRIDE;
            v[b] = x;
            v[b + 1] = y;
            v[b + 2] = z;
            v[b + 3] = nx;
            v[b + 4] = ny;
            v[b + 5] = nz;
            v[b + TreeMesh.LIMB] = limb;
            v[b + TreeMesh.TWIG] = twig;
            v[b + TreeMesh.FLUTTER] = flutter;
            v[b + TreeMesh.PHASE_LIMB] = limbPh;
            v[b + TreeMesh.PHASE_TWIG] = twigPh;
            v[b + TreeMesh.KIND] = kind;
            v[b + TreeMesh.RAND] = rand;
            nv++;
        }

        void triangle(int i0, int i1, int i2) {
            if ((nt + 1) * 3 > t.length) t = Arrays.copyOf(t, t.length * 2);
            t[nt * 3] = i0;
            t[nt * 3 + 1] = i1;
            t[nt * 3 + 2] = i2;
            nt++;
        }

        void quad(int i0, int i1, int i2, int i3) {
            triangle(i0, i1, i2);
            triangle(i0, i2, i3);
        }
    }

    /**
     * Fasst Ketten in einem Baum aus Knoten (Eltern vor Kindern) zusammen. Ein Knoten bleibt, wenn er
     * Wurzel, Gabel oder Spitze ist, die Richtung um mehr als {@code maxAngleDeg} knickt oder die
     * Strecke seit dem letzten behaltenen Knoten {@code maxLength} übersteigt.
     *
     * @return je Knoten der behaltene Vorfahr, mit dem er eine Strecke bildet; −1 für Wurzel und weggelassene
     */
    public static int[] chains(int[] parent, float[] pos, boolean[] use, float maxAngleDeg, float maxLength) {
        int n = parent.length;
        boolean[] u = use.clone();
        for (int i = 0; i < n; i++) if (parent[i] >= 0 && !u[parent[i]]) u[i] = false;
        int[] kids = new int[n], child = new int[n];
        Arrays.fill(child, -1);
        for (int i = 0; i < n; i++)
            if (u[i] && parent[i] >= 0) { kids[parent[i]]++; child[parent[i]] = i; }
        float cosMax = (float) Math.cos(Math.toRadians(maxAngleDeg));
        int[] last = new int[n];      // letzter behaltener Knoten auf dem Weg, einschließlich i
        int[] anc = new int[n];
        Arrays.fill(anc, -1);
        for (int i = 0; i < n; i++) {
            if (!u[i]) continue;
            int p = parent[i];
            if (p < 0) { last[i] = i; continue; }
            int a = last[p];
            boolean keep = kids[i] != 1;
            if (!keep) {
                float len = len(pos, i * 3, pos[a * 3], pos[a * 3 + 1], pos[a * 3 + 2]);
                int c = child[i];
                float lc = len(pos, c * 3, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
                float dot = 0;
                for (int e = 0; e < 3; e++) dot += (pos[i * 3 + e] - pos[a * 3 + e]) * (pos[c * 3 + e] - pos[i * 3 + e]);
                keep = len > maxLength || len < 1e-6f || lc < 1e-6f || dot / (len * lc) < cosMax;
            }
            if (keep) { anc[i] = a; last[i] = i; } else last[i] = a;
        }
        return anc;
    }

    static void copy(float[] src, int o, float[] dst) {
        dst[0] = src[o];
        dst[1] = src[o + 1];
        dst[2] = src[o + 2];
    }

    static float len(float[] p, int o, float x, float y, float z) {
        float dx = p[o] - x, dy = p[o + 1] - y, dz = p[o + 2] - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static void normalize(float[] n) {
        float l = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (l > 1e-9f) { n[0] /= l; n[1] /= l; n[2] /= l; }
    }
}
