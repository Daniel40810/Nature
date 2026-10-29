package com.dan.grass.kikuyu.demo;

import com.dan.grass.demo.GrassCheck;
import com.dan.grass.kikuyu.Kikuyu;

/** Selbsttest ohne Fenster; endet mit „Alles in Ordnung.“ */
public final class KikuyuCheck {

    public static void main(String[] args) {
        GrassCheck.run(Kikuyu.species(), Kikuyu.field(), 0.1f, 0.8f);
    }
}
