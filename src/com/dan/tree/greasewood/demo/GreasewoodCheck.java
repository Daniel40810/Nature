package com.dan.tree.greasewood.demo;

import com.dan.tree.greasewood.Greasewood;
import com.dan.tree.demo.TreeCheck;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class GreasewoodCheck {

    public static void main(String[] args) {
        TreeCheck.run(Greasewood.species(), 0.3f, 4f);
    }
}
