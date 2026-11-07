package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.client.ClientFx.Effect;
import dev.toaru.kazakiri.net.FxType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * AIM Convergence: the giant Archangel projection behind the caster, its constructs, the field structure and the
 * final synchronized wing strike. Timings match the server skill (AimConvergence).
 */
final class ConvergenceFx {
    private ConvergenceFx() { }

    static final int AWAKEN = 40, MANIFEST = 80, JUDGMENT = 120, RAISE = 160, RELEASE = 165, DESCEND = 170, TOTAL = 200;
    static final double SCALE = 4.5;

    private static double smooth(double t) { return FxRenderer.smooth(t); }
    private static double clamp01(double t) { return FxRenderer.clamp01(t); }

    // ---------------------------------------------------------------- lookups for the camera and other effects
    /** Age of the CONVERGENCE effect cast by entity {@code id}, or -1. */
    static double age(int id, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        long now = mc.level.getGameTime();
        double best = -1;
        for (Effect fx : ClientFx.ACTIVE.values()) {
            if (fx.type != FxType.CONVERGENCE || fx.a != id) continue;
            double a = fx.age(now, partial);
            if (a >= 0 && a < fx.duration && (best < 0 || a < best)) best = a;
        }
        return best;
    }

    static boolean active(int id) {
        Minecraft mc = Minecraft.getInstance();
        return age(id, mc.getFrameTime()) >= 0;
    }

    static double ownAge(float partial) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? -1 : age(mc.player.getId(), partial);
    }

    /** Age of the nearest Convergence within {@code range} blocks of the local player, or -1. */
    static double nearbyAge(float partial, double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return -1;
        long now = mc.level.getGameTime();
        double best = -1;
        for (Effect fx : ClientFx.ACTIVE.values()) {
            if (fx.type != FxType.CONVERGENCE) continue;
            Entity e = FxRenderer.entity(fx.a);
            Vec3 at = e == null ? fx.pos : e.position();
            if (at.distanceTo(mc.player.position()) > range) continue;
            double a = fx.age(now, partial);
            if (a >= 0 && a < fx.duration && (best < 0 || a < best)) best = a;
        }
        return best;
    }

    /** Age of the local player's Archangel Protocol transformation, or -1. */
    static double ownProtocolAge(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return -1;
        long now = mc.level.getGameTime();
        for (Effect fx : ClientFx.ACTIVE.values()) {
            if (fx.type != FxType.PROTOCOL_START || fx.a != mc.player.getId()) continue;
            double a = fx.age(now, partial);
            if (a >= 0 && a < fx.duration) return a;
        }
        return -1;
    }

    // ---------------------------------------------------------------- geometry
    private static Body projection(Body b) {
        Vec3 feet = b.feet.subtract(b.fwd.scale(2.2)).add(0, 1.5, 0);
        return Body.at(feet, b.fwd, 6.0);
    }

    /** Construct {@code i} (0..5) of the giant projection: an arc above and behind the caster. */
    static Vec3 constructPos(Body b, int i, double time) {
        Body g = projection(b);
        double spread = (i % 6) / 5.0 - 0.5;   // -0.5 .. 0.5
        double ang = spread * Math.PI * 0.9;
        return g.feet.add(0, g.height * 0.9, 0)
                .add(g.right.scale(Math.sin(ang) * 6.5))
                .add(g.up.scale(Math.cos(ang) * 3.0))
                .add(g.fwd.scale(0.6 + 0.15 * Math.sin(time * 0.05 + i)));
    }

    static void draw(Geo crystal, Geo glow, Effect fx, double age, float partial, double time) {
        Entity e = FxRenderer.entity(fx.a);
        if (e == null) return;
        Body b = Body.of(e, partial);
        double radius = Math.max(4, fx.aux.x);
        double fadeOut = 1 - smooth((age - DESCEND) / (TOTAL - DESCEND));
        float a = (float) fadeOut;
        if (a <= 0.01f) return;

        // Phase 1 (0-40): awakening. A big halo, a ground sigil.
        double awaken = smooth(age / AWAKEN);
        Vec3 haloC = b.feet.add(0, b.height + 0.3, 0).subtract(b.fwd.scale(0.5));
        glow.color(0.95f, 0.99f, 1f, 0.85f * a).ring(haloC, b.fwd, 1.5 * awaken, 0.04, 64, time * 0.02, false);
        glow.color(0.75f, 0.96f, 1f, 0.6f * a).hexRing(haloC, b.fwd, 1.0 * awaken, 0.03, -time * 0.03);
        Vec3 ground = b.feet.add(0, 0.06, 0);
        double sr = radius * awaken;
        glow.color(0.8f, 0.97f, 1f, 0.55f * a).ring(ground, FxRenderer.UP, sr, 0.06, 96, time * 0.005, false);
        glow.color(0.85f, 0.98f, 1f, 0.4f * a).ring(ground, FxRenderer.UP, sr * 0.72, 0.035, 72, -time * 0.008, true);
        glow.color(0.9f, 1f, 1f, 0.45f * a).hexRing(ground, FxRenderer.UP, sr * 0.5, 0.04, time * 0.01);
        crystal.color(0.75f, 0.95f, 1f, 0.06f * a).ring(ground, FxRenderer.UP, sr * 0.5, sr * 0.5, 64, 0, false);

        // Phase 2 (40-80): the projection forms behind the caster and opens its wings.
        if (age >= AWAKEN) {
            Body g = projection(b);
            double form = smooth((age - AWAKEN) / 40.0);
            float open = (float) form;
            double flap = time * 0.05;
            float beat = 0.15f;
            // Raised wings 120-160, downbeat at 165.
            if (age >= JUDGMENT && age < RELEASE) { open = 1f; flap = -1.2 * smooth((age - JUDGMENT) / 40.0); beat = 0.1f; }
            if (age >= RELEASE) { double t = clamp01((age - RELEASE) / 6.0); flap = -1.2 + 2.6 * smooth(t); beat = 1f; }
            float alpha = (float) (0.75 * form) * a;
            WingModel.draw(crystal, glow, g, open, flap, beat, 0, 0, (float) SCALE, alpha, true);
            // The projection's silhouette: a tall, faint crystal body and its own halo.
            crystal.color(0.88f, 0.98f, 1f, 0.12f * (float) form * a).crystal(g.feet.add(0, g.height * 0.5, 0), FxRenderer.UP, 0.9, g.height * 0.5, time * 0.01);
            glow.color(0.95f, 1f, 1f, 0.7f * (float) form * a).ring(g.aboveHead(), g.fwd, 1.6, 0.06, 64, time * 0.015, false);
            glow.color(0.8f, 0.97f, 1f, 0.5f * (float) form * a).hexRing(g.aboveHead(), g.fwd, 2.4, 0.04, -time * 0.01);

            // Phase 3 (80-120): six constructs appear one by one.
            if (age >= MANIFEST) {
                for (int i = 0; i < 6; i++) {
                    double ci = smooth((age - MANIFEST - i * 5) / 10.0);
                    if (ci <= 0) continue;
                    Vec3 at = constructPos(b, i, time);
                    Vec3 axis = at.subtract(g.feet.add(0, g.height * 0.6, 0));
                    FxRenderer.construct(crystal, glow, at, axis, 1.6 * ci, (float) ci * a, time * 0.05 + i);
                }
            }

            // Phase 4 (120-165): judgment. A field structure over the area.
            if (age >= JUDGMENT) {
                double s = smooth((age - JUDGMENT) / 20.0);
                Vec3 dome = b.feet.add(0, radius * 0.35, 0);
                for (int k = 0; k < 3; k++) {
                    double h = k * radius * 0.12;
                    glow.color(0.85f, 0.98f, 1f, 0.35f * (float) s * a).ring(b.feet.add(0, 0.3 + h, 0), FxRenderer.UP, radius * s * (1 - 0.15 * k), 0.03, 96, time * 0.01 * (k % 2 == 0 ? 1 : -1), k == 1);
                }
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3 + time * 0.004;
                    Vec3 edge = b.feet.add(Math.cos(ang) * radius * s, 0.3, Math.sin(ang) * radius * s);
                    glow.color(0.8f, 0.96f, 1f, 0.3f * (float) s * a).ribbon(edge, dome, 0.025);
                }
            }
        }
    }

    /** The final synchronized wing strike: a massive forward curved wave and a ground ring. */
    static void finalWave(Geo crystal, Geo glow, Effect fx, double t, double time) {
        double radius = Math.max(4, fx.power);
        float a = (float) Math.pow(1 - t, 1.3);
        double r = radius * (0.2 + 0.95 * smooth(t * 1.2));
        double half = Math.toRadians(80);
        Vec3 base = fx.pos.add(0, 0.2, 0);
        crystal.color(0.85f, 0.97f, 1f, 0.2f * a);
        FxRenderer.arcWall(crystal, base, fx.aux, r, half, 6.0 * (1 - 0.5 * t), 40);
        crystal.color(0.92f, 0.99f, 1f, 0.12f * a);
        FxRenderer.arcWall(crystal, base, fx.aux, r * 0.8, half * 0.9, 4.0, 32);
        glow.color(1f, 1f, 1f, 0.85f * a).ring(fx.pos.add(0, 0.08, 0), FxRenderer.UP, radius * smooth(t * 1.4), 0.12, 128, 0, false);
        glow.color(0.75f, 0.96f, 1f, 0.6f * a).hexRing(fx.pos.add(0, 0.1, 0), FxRenderer.UP, radius * 0.7 * smooth(t * 1.4), 0.07, time * 0.01);
        for (int k = 0; k < 3; k++)
            glow.color(0.9f, 0.98f, 1f, 0.6f * a).ring(fx.pos.add(0, 1 + k * 1.5, 0), FxRenderer.UP, r * (0.4 + 0.2 * k), 0.05, 96, k, k == 1);
    }
}
