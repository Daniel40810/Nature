// tree.frag — Sonne, Himmel, Durchscheinen der Blätter (beidseitig), Dunst; wie demo/SoftRenderer.
in vec3 vWorld;
in vec3 vNormal;
in vec3 vColor;
in float vLeaf;
in float vAo;
in vec2 vUv;
in float vTex;
uniform sampler2D uBarkTex, uLeafTex;  // sRGB-Texturen, liefern linear
uniform sampler2D uBarkNrm;             // Normal-Map der Rinde (OpenGL-Format, linear)
uniform int uBarkNrmOn;
uniform vec3 uCam;
uniform vec3 uSunDir;                   // zur Sonne
uniform vec3 uSunColor, uSkyColor, uGroundColor;
uniform vec4 uFog;                      // Farbe, Sichtweite (m)
uniform float uExposure;
uniform int uTonemap;                   // 1: Belichtung, Reinhard, sRGB hier; 0: linear ausgeben
out vec4 fragColor;

void main() {
    vec3 albedo = vColor;
    if (vTex > 1.5) {
        vec4 t = texture(uLeafTex, vUv);
        if (t.a < 0.5) discard;
        albedo *= t.rgb;
    } else if (vTex > 0.5) {
        albedo *= texture(uBarkTex, vUv).rgb;
    }
    vec3 n = normalize(vNormal);
    vec3 v = normalize(uCam - vWorld);
    if (uBarkNrmOn == 1 && vTex > 0.5 && vTex < 1.5) {
        // Tangentenraum aus den Ableitungen von Ort und UV (ohne Tangenten im Netz); v läuft wie die Bildzeilen nach unten
        vec3 dp1 = dFdx(vWorld), dp2 = dFdy(vWorld);
        vec2 du1 = dFdx(vUv), du2 = dFdy(vUv);
        vec3 p2 = cross(dp2, n), p1 = cross(n, dp1);
        vec3 T = p2 * du1.x + p1 * du2.x, B = p2 * du1.y + p1 * du2.y;
        float s = inversesqrt(max(max(dot(T, T), dot(B, B)), 1e-20));
        vec3 tn = texture(uBarkNrm, vUv).xyz * 2.0 - 1.0;
        vec3 m = normalize(T * s * tn.x - B * s * tn.y + n * max(tn.z, 0.05));
        if (all(equal(m, m))) n = m;
    }
    float trans = 0.0;
    float ndl;
    if (vLeaf > 0.5) {
        if (dot(n, v) < 0.0) n = -n;
        ndl = dot(n, uSunDir);
        trans = max(0.0, -ndl) * 0.45;
        ndl = max(0.0, ndl * 0.8 + 0.2);
    } else {
        ndl = max(0.0, dot(n, uSunDir));
    }
    float hemi = 0.5 + 0.5 * n.y;
    vec3 light = uSunColor * (ndl + trans) * (0.35 + 0.65 * vAo)
               + (uSkyColor * hemi + uGroundColor * (1.0 - hemi)) * (0.4 + 0.6 * vAo);
    vec3 c = albedo * light;
    float dist = length(uCam - vWorld);
    c = mix(c, uFog.rgb, 1.0 - exp(-dist / max(uFog.w, 1.0)));
    if (uTonemap == 1) {
        c *= uExposure;
        c = c / (1.0 + c * 0.25);
        c = pow(max(c, vec3(0.0)), vec3(1.0 / 2.2));
    }
    fragColor = vec4(c, 1.0);
}
