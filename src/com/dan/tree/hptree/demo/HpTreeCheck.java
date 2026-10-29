package com.dan.tree.hptree.demo;

import com.dan.tree.hptree.HpTree;
import com.dan.tree.demo.TreeCheck;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class HpTreeCheck {

    public static void main(String[] args) {
        TreeCheck.run(HpTree.species(), 20f, 32f);
    }
}
