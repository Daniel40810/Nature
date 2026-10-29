package com.dan.tree.mobiletree.demo;

import com.dan.tree.mobiletree.MobileTree;
import com.dan.tree.demo.TreeCheck;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class MobileTreeCheck {

    public static void main(String[] args) {
        TreeCheck.run(MobileTree.species(), 18f, 30f);
    }
}
