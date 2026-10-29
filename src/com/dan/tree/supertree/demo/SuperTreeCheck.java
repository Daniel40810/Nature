package com.dan.tree.supertree.demo;

import com.dan.tree.supertree.SuperTree;
import com.dan.tree.demo.TreeCheck;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class SuperTreeCheck {

    public static void main(String[] args) {
        TreeCheck.run(SuperTree.species(), 18f, 30f);
    }
}
