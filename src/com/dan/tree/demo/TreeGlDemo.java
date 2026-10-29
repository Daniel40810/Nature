package com.dan.tree.demo;

import com.dan.fcheckbox.FCheckBox;
import com.dan.fframe.FFrame;
import com.dan.fslider.FSlider;
import com.dan.tree.TreeSpecies;
import com.dan.tree.TreeWind;
import com.dan.tree.gl.TreeGl;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Grafikteil von {@code com.dan.tree}: ein Bestand aus Asset-Bäumen einer Art mit {@link TreeGl}
 * (OpenGL 4.3), Stufe nach Entfernung, Wind und Jahreszeit im Shader. Die Pakete
 * {@code com.dan.tree.<art>.demo} rufen nur {@link #run(TreeSpecies, String[])} auf.
 *
 * <pre>
 * …GlDemo [bäume] [fläche m] [tag]
 * </pre>
 */
public final class TreeGlDemo implements GLEventListener {

    final TreeSpecies sp;
    final TreeGl trees;
    final TreeWind wind = new TreeWind();
    final TreeGl.Light light = new TreeGl.Light();
    final float area;
    volatile int day = 180;
    volatile float snow;
    volatile boolean running = true;
    volatile double yaw = 0.7, pitch = 0.18, dist = 55;
    double time;
    long last;
    double frameMs;
    int width = 1, height = 1;
    JLabel info;

    int groundProg, groundVao;

    TreeGlDemo(TreeSpecies sp, int count, float area) {
        this.sp = sp;
        this.area = area;
        trees = new TreeGl(sp);
        Random r = new Random(7);
        List<float[]> pts = new ArrayList<>();
        float minGap = 6.5f;
        int tries = 0;
        while (pts.size() < count && tries++ < count * 60) {
            float x = (r.nextFloat() - 0.5f) * area, z = (r.nextFloat() - 0.5f) * area;
            boolean ok = true;
            for (float[] p : pts) if (Math.abs(p[0] - x) < minGap && Math.abs(p[1] - z) < minGap && Math.hypot(p[0] - x, p[1] - z) < minGap) { ok = false; break; }
            if (ok) pts.add(new float[]{x, z});
        }
        for (float[] p : pts)
            trees.add(r.nextInt(sp.variantCount()), p[0], 0, p[1], r.nextFloat() * 6.283f, 0.85f + r.nextFloat() * 0.3f);
        wind.speed = 7;
        wind.gustiness = 0.5f;
    }

    public static void run(TreeSpecies sp, String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 150;
        float area = args.length > 1 ? Float.parseFloat(args[1]) : 220;
        TreeGlDemo d = new TreeGlDemo(sp, count, area);
        if (args.length > 2) d.day = Integer.parseInt(args[2]);
        SwingUtilities.invokeLater(d::open);
    }

    void open() {
        FFrame f = new FFrame(sp.name + " – Grafikteil von com.dan.tree (JOGL)");
        JPanel root = f.getComponentPane();
        root.setLayout(new BorderLayout());
        root.setBackground(TreeDemo.BG);
        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL4));
        caps.setDepthBits(24);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        GLJPanel canvas = new GLJPanel(caps);
        canvas.addGLEventListener(this);
        MouseAdapter mouse = new MouseAdapter() {
            Point lastP;

            @Override public void mousePressed(MouseEvent e) { lastP = e.getPoint(); }

            @Override public void mouseDragged(MouseEvent e) {
                yaw += (e.getX() - lastP.x) * 0.008;
                pitch = Math.max(0.02, Math.min(1.4, pitch + (e.getY() - lastP.y) * 0.006));
                lastP = e.getPoint();
            }

            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                dist = Math.max(4, Math.min(1500, dist * Math.pow(1.12, e.getWheelRotation())));
            }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseWheelListener(mouse);
        root.add(canvas, BorderLayout.CENTER);
        root.add(controls(), BorderLayout.EAST);
        info = new JLabel(" ");
        info.setForeground(TreeDemo.MUTED);
        info.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        root.add(info, BorderLayout.SOUTH);
        f.setPreferredFrameSize(new Dimension(1440, 880));
        f.setSize(1440, 880);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        new FPSAnimator(canvas, 60).start();
    }

    JComponent controls() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(TreeDemo.BG);
        side.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        side.setPreferredSize(new Dimension(280, 10));
        JLabel head = new JLabel(sp.name + " (" + sp.latin + ")");
        head.setForeground(TreeDemo.INK);
        head.setFont(new Font("SansSerif", Font.BOLD, 14));
        side.add(head);
        slider(side, "Wind", 0, 30, (int) wind.speed, v -> { wind.speed = v; return v + " m/s"; });
        slider(side, "Böigkeit", 0, 100, 50, v -> { wind.gustiness = v / 100f; return v + " %"; });
        slider(side, "aus Richtung", 0, 359, (int) wind.fromDeg, v -> { wind.fromDeg = v; return v + "°"; });
        slider(side, "Tag im Jahr", 0, 364, day, v -> { day = v; return TreeDemo.dateLabel(v); });
        slider(side, "Schnee", 0, 100, 0, v -> { snow = v / 100f; return v + " %"; });
        slider(side, "Sonne (Azimut)", 0, 359, 210, v -> {
            double az = Math.toRadians(v), el = Math.toRadians(40);
            light.sunDir = new double[]{Math.sin(az) * Math.cos(el), Math.sin(el), -Math.cos(az) * Math.cos(el)};
            return v + "°";
        });
        FCheckBox run = new FCheckBox("Zeit läuft");
        run.setSelected(true);
        run.addActionListener(e -> running = run.isSelected());
        run.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(Box.createVerticalStrut(10));
        side.add(run);
        JLabel note = new JLabel("Maus ziehen: drehen · Rad: Abstand");
        note.setForeground(TreeDemo.MUTED);
        note.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        side.add(note);
        side.add(Box.createVerticalGlue());
        return side;
    }

    void slider(JPanel side, String name, int min, int max, int value, TreeDemo.Change c) {
        JLabel l = new JLabel(name + ": " + c.set(value));
        l.setForeground(TreeDemo.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(10, 0, 3, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        FSlider s = new FSlider(min, max, value);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.addChangeListener(e -> l.setText(name + ": " + c.set(s.getValue())));
        side.add(l);
        side.add(s);
    }

    // ------------------------------------------------------------------ GL

    static final String GROUND_VS = """
            layout(location = 0) in vec2 aXZ;
            uniform mat4 uVP;
            out vec3 vWorld;
            void main() { vWorld = vec3(aXZ.x, 0.0, aXZ.y); gl_Position = uVP * vec4(vWorld, 1.0); }
            """;
    static final String GROUND_FS = """
            in vec3 vWorld;
            uniform vec3 uCam, uSunDir, uSunColor, uSkyColor;
            uniform vec4 uFog;
            uniform float uSnow;
            out vec4 fragColor;
            void main() {
                float v = 0.92 + 0.08 * sin(vWorld.x * 0.37 + sin(vWorld.z * 0.23) * 2.0) * sin(vWorld.z * 0.31 + vWorld.x * 0.11);
                vec3 base = mix(vec3(0.045, 0.07, 0.025) * v, vec3(0.8), uSnow);
                vec3 c = base * (uSunColor * max(uSunDir.y, 0.0) + uSkyColor * 0.9);
                c = mix(c, uFog.rgb, 1.0 - exp(-length(uCam - vWorld) / uFog.w));
                c = c / (1.0 + c * 0.25);
                fragColor = vec4(pow(max(c, vec3(0.0)), vec3(1.0 / 2.2)), 1.0);
            }
            """;

    @Override
    public void init(GLAutoDrawable d) {
        GL4 gl = d.getGL().getGL4();
        trees.init(gl);
        groundProg = shaderProgram(gl, GROUND_VS, GROUND_FS);
        int[] ids = new int[1];
        gl.glGenVertexArrays(1, ids, 0);
        groundVao = ids[0];
        gl.glBindVertexArray(groundVao);
        gl.glGenBuffers(1, ids, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, ids[0]);
        float s = 4000;
        float[] quad = {-s, -s, s, -s, s, s, -s, -s, s, s, -s, s};
        gl.glBufferData(GL.GL_ARRAY_BUFFER, quad.length * 4L, Buffers.newDirectFloatBuffer(quad), GL.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 8, 0L);
        gl.glBindVertexArray(0);
        last = System.nanoTime();
    }

    @Override
    public void display(GLAutoDrawable d) {
        GL4 gl = d.getGL().getGL4();
        long now = System.nanoTime();
        double dt = (now - last) / 1e9;
        last = now;
        frameMs = frameMs * 0.9 + dt * 1000 * 0.1;
        if (running) time += dt;
        float h = trees.height(0) * 0.45f;
        double[] eye = {Math.cos(yaw) * Math.cos(pitch) * dist, h + Math.sin(pitch) * dist, Math.sin(yaw) * Math.cos(pitch) * dist};
        float[] vp = mul(persp(45, width / (double) Math.max(height, 1), 0.2, 6000), look(eye, new double[]{0, h, 0}));

        gl.glViewport(0, 0, width, height);
        double[] fog = light.fogColor;
        float[] sky = {tone(fog[0]), tone(fog[1]), tone(fog[2])};
        gl.glClearColor(sky[0], sky[1], sky[2], 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);

        gl.glUseProgram(groundProg);
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(groundProg, "uVP"), 1, false, vp, 0);
        gl.glUniform3f(gl.glGetUniformLocation(groundProg, "uCam"), (float) eye[0], (float) eye[1], (float) eye[2]);
        double[] sd = light.sunDir;
        gl.glUniform3f(gl.glGetUniformLocation(groundProg, "uSunDir"), (float) sd[0], (float) sd[1], (float) sd[2]);
        gl.glUniform3f(gl.glGetUniformLocation(groundProg, "uSunColor"), (float) light.sunColor[0], (float) light.sunColor[1], (float) light.sunColor[2]);
        gl.glUniform3f(gl.glGetUniformLocation(groundProg, "uSkyColor"), (float) light.skyColor[0], (float) light.skyColor[1], (float) light.skyColor[2]);
        gl.glUniform4f(gl.glGetUniformLocation(groundProg, "uFog"), (float) fog[0], (float) fog[1], (float) fog[2], (float) light.fogDistance);
        gl.glUniform1f(gl.glGetUniformLocation(groundProg, "uSnow"), snow);
        gl.glBindVertexArray(groundVao);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl.glBindVertexArray(0);

        trees.setSeason(day, snow);
        trees.setWind(wind);
        trees.update(gl, eye);
        TreeGl.View view = new TreeGl.View();
        view.viewProj = vp;
        view.eye = eye;
        trees.draw(gl, view, light, time);

        String s = String.format("%d Bäume · %,d Dreiecke · %.1f ms je Bild%s", trees.trees().size(), trees.triangles(), frameMs,
                trees.ready() ? "" : " · backe Stufen …");
        SwingUtilities.invokeLater(() -> info.setText(s));
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        width = w;
        height = h;
    }

    @Override
    public void dispose(GLAutoDrawable d) {
        GL4 gl = d.getGL().getGL4();
        trees.dispose(gl);
        gl.glDeleteProgram(groundProg);
    }

    static float tone(double c) {
        c = c / (1 + c * 0.25);
        return (float) Math.pow(Math.max(0, c), 1 / 2.2);
    }

    static int shaderProgram(GL4 gl, String vs, String fs) {
        int p = gl.glCreateProgram();
        for (int k = 0; k < 2; k++) {
            int s = gl.glCreateShader(k == 0 ? GL4.GL_VERTEX_SHADER : GL4.GL_FRAGMENT_SHADER);
            gl.glShaderSource(s, 1, new String[]{"#version 430 core\n" + (k == 0 ? vs : fs)}, null);
            gl.glCompileShader(s);
            int[] ok = new int[1];
            gl.glGetShaderiv(s, GL4.GL_COMPILE_STATUS, ok, 0);
            if (ok[0] == 0) {
                byte[] b = new byte[4096];
                gl.glGetShaderInfoLog(s, b.length, null, 0, b, 0);
                throw new IllegalStateException(new String(b).trim());
            }
            gl.glAttachShader(p, s);
            gl.glDeleteShader(s);
        }
        gl.glLinkProgram(p);
        return p;
    }

    /** Perspektive, Spalten wie OpenGL. */
    static float[] persp(double fovDeg, double aspect, double near, double far) {
        float f = (float) (1 / Math.tan(Math.toRadians(fovDeg) / 2));
        return new float[]{f / (float) aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, (float) ((far + near) / (near - far)), -1,
                0, 0, (float) (2 * far * near / (near - far)), 0};
    }

    static float[] look(double[] e, double[] t) {
        double fx = t[0] - e[0], fy = t[1] - e[1], fz = t[2] - e[2];
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl; fy /= fl; fz /= fl;
        double sx = -fz, sy = 0, sz = fx;
        double sl = Math.sqrt(sx * sx + sz * sz);
        sx /= sl; sz /= sl;
        double ux = sy * fz - sz * fy, uy = sz * fx - sx * fz, uz = sx * fy - sy * fx;
        return new float[]{(float) sx, (float) ux, (float) -fx, 0, (float) sy, (float) uy, (float) -fy, 0,
                (float) sz, (float) uz, (float) -fz, 0,
                (float) -(sx * e[0] + sy * e[1] + sz * e[2]), (float) -(ux * e[0] + uy * e[1] + uz * e[2]),
                (float) (fx * e[0] + fy * e[1] + fz * e[2]), 1};
    }

    static float[] mul(float[] a, float[] b) {
        float[] o = new float[16];
        SoftRenderer.mul(a, b, o);
        return o;
    }
}
