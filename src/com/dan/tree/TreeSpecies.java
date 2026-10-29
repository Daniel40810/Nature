package com.dan.tree;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Eine Baumart aus Unreal-Assets: Varianten als {@code .dtree}-Ressourcen neben {@link #owner},
 * Jahreszeit, Farben und Biegsamkeit. Die Pakete {@code com.dan.tree.<art>} liefern je eine
 * Voreinstellung; alle Felder sind öffentlich und lassen sich ändern.
 */
public final class TreeSpecies {

    public String name, latin;
    /** Klasse, neben der die Ressourcen liegen */
    public Class<?> owner;
    /** Ressourcen je Variante, etwa {"alder_a.dtree", …} */
    public String[] variants;
    /** Namen der Varianten für Oberflächen */
    public String[] variantNames;

    /** Rindenfarbe, linear; null = Farbe aus dem Asset */
    public float[] bark;
    public TreeSeason season = new TreeSeason();

    /** Biegsamkeit: Stamm, Äste, Zittern der Blätter (1 = mittel) */
    public float flexTrunk = 1, flexBranch = 1, flutter = 1;

    private final TreeAsset[] cache;
    private final Map<String, BufferedImage> textures = new HashMap<>();

    public TreeSpecies(String name, String latin, Class<?> owner, String... variants) {
        this.name = name;
        this.latin = latin;
        this.owner = owner;
        this.variants = variants;
        this.variantNames = new String[variants.length];
        for (int i = 0; i < variants.length; i++) variantNames[i] = String.valueOf((char) ('A' + i));
        this.cache = new TreeAsset[variants.length];
    }

    public int variantCount() { return variants.length; }

    /** Lädt die Variante (einmal, danach aus dem Zwischenspeicher). */
    public synchronized TreeAsset load(int variant) {
        if (cache[variant] == null) cache[variant] = TreeAsset.load(owner, variants[variant]);
        return cache[variant];
    }

    /** Textur neben {@link #owner} (Name aus {@link TreeAsset#barkTexture} / {@link TreeAsset#leafTexture}), oder null. */
    public synchronized BufferedImage texture(String name) {
        if (name == null || name.isEmpty()) return null;
        return textures.computeIfAbsent(name, n -> {
            try (InputStream in = owner.getResourceAsStream(n)) {
                if (in == null) throw new IllegalArgumentException("Textur fehlt: " + n + " neben " + owner.getName());
                return ImageIO.read(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Jahreszeit für Laubtexturen (FBX-Arten): Sommer ist die mittlere Farbe der Laubtextur der ersten Variante
     * (Tönung 1), Austrieb heller, Herbst wärmer und dunkler, Laubfall ab Tag 300, kahl ab Tag 315.
     * {@code flex} stellt Stamm (0,8 · flex) und Äste (flex) ein.
     */
    public TreeSpecies seasonFromLeafTexture(float flex) {
        float[] m = load(0).leafColor;
        flexTrunk = 0.8f * flex;
        flexBranch = flex;
        season.summer = m.clone();
        season.spring = new float[]{m[0] * 1.3f, m[1] * 1.45f, m[2] * 1.1f};
        season.autumn = new float[][]{{m[0] * 1.9f, m[1] * 1.1f, m[2] * 0.6f}, {m[0] * 1.5f, m[1] * 0.7f, m[2] * 0.5f}};
        season.leafOut = 105;
        season.colorStart = 280;
        season.colorFull = 300;
        season.bare = 315;
        return this;
    }

    /** Netz einer Variante in einer Stufe. */
    public TreeMesh bake(int variant, TreeLod lod) {
        return TreeBaker.bake(load(variant), lod);
    }

    public TreeMesh bake(int variant, int level) {
        return bake(variant, TreeLod.of(level));
    }

    @Override
    public String toString() {
        return name + " (" + latin + ") " + Arrays.toString(variantNames);
    }
}
