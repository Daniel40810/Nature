package com.dan.tree;

import java.util.Arrays;

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
