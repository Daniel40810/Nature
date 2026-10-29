package com.dan.tree.trees60;

import com.dan.tree.TreeSpecies;

/**
 * Zehn Laubbäume aus „Trees60“ (FBX, Stamm und Laubkarten mit Textur, ohne Skelett), 14 bis 27 m hoch.
 * Die Laubtextur ist schon herbstlich rot-orange; die Jahreszeit tönt sie (Bezug ist die mittlere Farbe der
 * Textur), im Winter sind die Bäume kahl. Die Art gibt das Pack nicht an.
 */
public final class Trees60 {

    private Trees60() {}

    public static final String[] VARIANTS = {"trees60_1.dtree", "trees60_2.dtree", "trees60_3.dtree", "trees60_4.dtree", "trees60_5.dtree", "trees60_6.dtree", "trees60_7.dtree", "trees60_8.dtree", "trees60_9.dtree", "trees60_10.dtree"};

    /** Jede Variante wird beim ersten Gebrauch geladen; jeder Aufruf liefert eine frische Art. */
    public static TreeSpecies species() {
        TreeSpecies s = new TreeSpecies("Trees60", "Art nicht angegeben", Trees60.class, VARIANTS);
        s.variantNames = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        s.seasonFromLeafTexture(1.0f);
        return s;
    }
}
