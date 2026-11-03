package com.dan.ground;

import com.dan.grass.GrassField;
import com.dan.tree.TreeSpecies;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Eine kachelbare Oberfläche aus Megascans (Erde, Leinen, Wellblech …), wie sie
 * {@code tools/ground/megascans_surface.py} neben eine Klasse legt: Farbe (mit etwas Verdeckung), Normal-Map
 * (OpenGL-Format), Höhenkarte (16 bit) und {@code <prefix>.material} mit Kachelgröße und Relief.
 *
 * <p>Koordinaten: Eine Kachel deckt {@link #tileX} × {@link #tileZ} Meter. Auf dem Boden ist u = x / tileX und
 * v = z / tileZ, v läuft wie die Bildzeilen (wie überall in {@code com.dan.tree}).
 */
public final class GroundMaterial {

    public final Class<?> owner;
    public final String prefix, name;
    /** Namen der Bilder neben {@link #owner} */
    public final String albedoName, normalName, heightName;
    /** Größe einer Kachel in m */
    public final float tileX, tileZ;
    /** Höhenunterschied von Weiß zu Schwarz in der Höhenkarte (m) */
    public final float relief;
    /** Wert der Höhenkarte (0..1), der der mittleren Fläche entspricht */
    public final float heightMid;
    /** mittlere Farbe, linear */
    public final float[] color;

    private BufferedImage albedo, normal;
    private float[] heights;
    private int hw, hh;

    private GroundMaterial(Class<?> owner, String prefix, Properties p) {
        this.owner = owner;
        this.prefix = prefix;
        name = p.getProperty("name", prefix);
        albedoName = p.getProperty("albedo", prefix + "_albedo.jpg");
        normalName = p.getProperty("normal", prefix + "_normal.jpg");
        heightName = p.getProperty("height", prefix + "_height.png");
        tileX = Float.parseFloat(p.getProperty("tileX", "1"));
        tileZ = Float.parseFloat(p.getProperty("tileZ", "1"));
        relief = Float.parseFloat(p.getProperty("relief", "0.05"));
        heightMid = Float.parseFloat(p.getProperty("heightMid", "0.5"));
        String[] c = p.getProperty("color", "0.2,0.18,0.14").split(",");
        color = new float[]{Float.parseFloat(c[0].trim()), Float.parseFloat(c[1].trim()), Float.parseFloat(c[2].trim())};
    }

    /** Liest {@code <prefix>.material} neben {@code owner}. */
    public static GroundMaterial load(Class<?> owner, String prefix) {
        String res = prefix + ".material";
        try (InputStream in = owner.getResourceAsStream(res)) {
            if (in == null) throw new IllegalArgumentException("Ressource fehlt: " + res + " neben " + owner.getName());
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return new GroundMaterial(owner, prefix, p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Farbbild (sRGB), einmal geladen. */
    public synchronized BufferedImage albedo() {
        if (albedo == null) albedo = image(albedoName);
        return albedo;
    }

    /** Normal-Map (OpenGL-Format, Daten, kein sRGB), einmal geladen. */
    public synchronized BufferedImage normal() {
        if (normal == null) normal = image(normalName);
        return normal;
    }

    private BufferedImage image(String res) {
        try (InputStream in = owner.getResourceAsStream(res)) {
            if (in == null) throw new IllegalArgumentException("Ressource fehlt: " + res + " neben " + owner.getName());
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private synchronized void heights() {
        if (heights != null) return;
        BufferedImage img = image(heightName);
        Raster r = img.getRaster();
        hw = img.getWidth();
        hh = img.getHeight();
        float max = (1 << r.getSampleModel().getSampleSize(0)) - 1;
        float[] h = new float[hw * hh];
        for (int y = 0; y < hh; y++)
            for (int x = 0; x < hw; x++) h[y * hw + x] = r.getSample(x, y, 0) / max;
        heights = h;
    }

    /** Breite und Höhe der Höhenkarte in Punkten. */
    public int heightMapSize() {
        heights();
        return hw;
    }

    /** Höhenkarte 0..1 an (u, v) in Kacheln, bilinear und wiederholt. */
    public float height01(float u, float v) {
        heights();
        float fx = u * hw - 0.5f, fy = v * hh - 0.5f;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        float tx = fx - x0, ty = fy - y0;
        int xa = Math.floorMod(x0, hw), xb = Math.floorMod(x0 + 1, hw), ya = Math.floorMod(y0, hh), yb = Math.floorMod(y0 + 1, hh);
        float a = heights[ya * hw + xa], b = heights[ya * hw + xb], c = heights[yb * hw + xa], d = heights[yb * hw + xb];
        return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty;
    }

    /** Relief am Ort in m (0 = mittlere Fläche), Kachel {@link #tileX} × {@link #tileZ}. */
    public float displacement(float x, float z) {
        return (height01(x / tileX, z / tileZ) - heightMid) * relief;
    }

    /** Relief als Boden für {@link GrassField} (etwa um Gras oder Steine darauf zu setzen), mal {@code scale}. */
    public GrassField.Ground ground(float scale) {
        return (x, z) -> displacement(x, z) * scale;
    }

    /**
     * Macht Farbe und Normal-Map in einer Art unter ihren Namen verfügbar, auch wenn die Art ihre Ressourcen
     * neben einer anderen Klasse sucht (etwa der Graben, der mehrere Materialien nutzt).
     */
    public void addTo(TreeSpecies s) {
        s.putTexture(albedoName, albedo());
        s.putTexture(normalName, normal());
        s.barkNormal.put(albedoName, normalName);
    }

    @Override
    public String toString() {
        return String.format("%s (Kachel %.1f × %.1f m, Relief %.3f m)", name, tileX, tileZ, relief);
    }
}
