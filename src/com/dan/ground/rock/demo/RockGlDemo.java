package com.dan.ground.rock.demo;

import com.dan.ground.demo.GroundGlDemo;
import com.dan.ground.dirtcoarse.DirtCoarse;
import com.dan.ground.rock.Rock;

/**
 * Viele Felsen mit JOGL (OpenGL 4.3) und Normal-Maps, zufällig verteilt auf dem Boden „Erde grob“.
 *
 * <pre>
 * Fenster:  RockGlDemo [anzahl] [fläche m] [tag]
 * Bild:     RockGlDemo --bild datei.png [anzahl] [fläche m] [tag] [wind m/s] [breite] [höhe]
 * </pre>
 */
public final class RockGlDemo {

    public static void main(String[] args) throws Exception {
        GroundGlDemo.run(Rock.species(), DirtCoarse.material(), args, 60);
    }
}
