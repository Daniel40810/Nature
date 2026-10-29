package com.dan.tree.blackalder.demo;

import com.dan.tree.blackalder.BlackAlder;
import com.dan.tree.demo.TreeCheck;

/** Selbsttest der Schwarzerle ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class BlackAlderCheck {

    public static void main(String[] args) {
        TreeCheck.run(BlackAlder.species(), 9, 20);
    }
}
