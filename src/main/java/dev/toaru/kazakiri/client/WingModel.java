package dev.toaru.kazakiri.client;

import net.minecraft.world.phys.Vec3;

/**
 * Procedural angel wings: per side an arm (three arcing segments) carrying three layers of individual feathers
 * (coverts, secondaries, primaries). "open" folds/unfolds them, "flap" beats them, and vertical / horizontal speed
 * changes the pose (raised and spread when descending, swept back when moving fast). About 60 small shapes per player.
 */
final class WingModel {
    private WingModel() { }

    /** count, length (share of arm), first and last position along the arm, width (share of length), opacity. */
    private static final double[][] LAYERS = {
            {10, 0.30, 0.04, 0.80, 0.30, 0.75},
            {10, 0.62, 0.08, 0.78, 0.20, 0.55},
            {8, 0.95, 0.50, 1.00, 0.16, 0.45},
    };

    static void draw(Geo crystal, Geo glow, Body body, float open, double flap, float beat, double vy, double hspeed,
                     float scale, float alpha, boolean archangel) {
        draw(crystal, glow, body, open, flap, beat, vy, hspeed, scale, alpha, archangel, 0.0);
    }

    /** {@code rootDrop}: how far below the normal root this pair sits (the Protocol form's second, lower pair). */
    static void draw(Geo crystal, Geo glow, Body body, float open, double flap, float beat, double vy, double hspeed,
                     float scale, float alpha, boolean archangel, double rootDrop) {
        if (open < 0.01f || alpha < 0.01f) return;
        for (int s = -1; s <= 1; s += 2) wing(crystal, glow, body, s, open, flap, beat, vy, hspeed, scale, alpha, archangel, rootDrop);
    }

    private static void wing(Geo crystal, Geo glow, Body b, int side, float open, double flap, float beat, double vy, double hspeed,
                             float scale, float alpha, boolean archangel, double rootDrop) {
        Vec3 lateral = b.right.scale(side);
        double amp = (0.06 + 0.32 * beat) * open;
        double theta = -0.55 + 0.80 * open + Math.sin(flap) * amp;
        if (vy < -0.08) theta += 0.25 * open;                       // gliding down: wings held high
        double phi = 1.25 - 1.05 * open + 0.35 * Math.min(hspeed, 1.0) * open;   // folded = swept back
        Vec3 spread = lateral.scale(Math.cos(theta)).add(b.up.scale(Math.sin(theta)));
        Vec3 arm = spread.scale(Math.cos(phi)).subtract(b.fwd.scale(Math.sin(phi))).normalize();
        Vec3 root = b.back().add(lateral.scale(0.12)).subtract(b.up.scale(rootDrop));
        double length = 2.25 * scale * (0.55 + 0.45 * open);
        Vec3 hang = b.up.scale(-0.9).subtract(b.fwd.scale(0.45)).normalize();

        // Arm bone (glowing arc).
        Vec3 prev = root;
        for (int i = 1; i <= 6; i++) {
            Vec3 pnt = armPoint(root, arm, b.up, length, i / 6.0, open);
            glow.color(0.75f, 0.95f, 1f, 0.55f * alpha).ribbon(prev, pnt, 0.025 * scale);
            prev = pnt;
        }

        for (int layer = 0; layer < LAYERS.length; layer++) {
            double[] L = LAYERS[layer];
            int n = (int) L[0];
            for (int j = 0; j < n; j++) {
                double u = n == 1 ? 1 : j / (double) (n - 1);
                double t = L[2] + (L[3] - L[2]) * u;
                Vec3 base = armPoint(root, arm, b.up, length, t, open);
                Vec3 dir = hang.scale(1.0 - u * 0.8 * open).add(arm.scale(u * 0.95 * open)).subtract(b.fwd.scale(0.12)).normalize();
                double len = length * L[1] * (layer == 2 ? 0.7 + 0.55 * u : 0.85 + 0.25 * u);
                Vec3 tip = base.add(dir.scale(len));
                Vec3 sideAxis = arm.subtract(dir.scale(arm.dot(dir)));
                sideAxis = sideAxis.lengthSqr() < 1e-6 ? lateral : sideAxis.normalize();
                float a = (float) (alpha * L[5]);
                // Gradient: white near the body, cyan towards the tips; the Archangel form is brighter and goldish.
                float r = archangel ? 1f : (float) (0.92 - 0.25 * u);
                float g = archangel ? (float) (0.97 - 0.05 * u) : (float) (0.98 - 0.05 * u);
                float bl = archangel ? (float) (0.85 + 0.1 * u) : 1f;
                crystal.color(r, g, bl, a).kite(base, tip, sideAxis, len * L[4]);
                if (layer == 2 || (layer == 1 && (j & 1) == 0)) {
                    glow.color(0.6f, 0.92f, 1f, 0.35f * alpha).ribbon(base, base.add(dir.scale(len * 0.9)), 0.008 * scale);
                }
            }
        }
    }

    private static Vec3 armPoint(Vec3 root, Vec3 arm, Vec3 up, double length, double t, float open) {
        return root.add(arm.scale(length * t)).add(up.scale(0.22 * length * Math.sin(Math.PI * t) * open));
    }
}
