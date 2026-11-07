package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.client.ClientFx.Effect;
import dev.toaru.kazakiri.net.FxType;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the moveset's effects as geometry (white, pale cyan, silver; hexagons, crystals, feathers, curved waves).
 * The Convergence projection is in ConvergenceFx.
 */
final class FxRenderer {
    private FxRenderer() { }

    private static final double MAX_DISTANCE = 200.0;
    static final Vec3 UP = new Vec3(0, 1, 0);

    static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    static double smooth(double t) { t = clamp01(t); return t * t * (3 - 2 * t); }

    static void draw(Geo crystal, Geo glow, Vec3 cam, long now, float partial, double time) {
        if (ClientFx.ACTIVE.isEmpty()) return;
        for (Effect fx : ClientFx.ACTIVE.values()) {
            if (fx.pos.distanceTo(cam) > MAX_DISTANCE) continue;
            double age = fx.age(now, partial);
            if (age < 0) continue;
            double t = clamp01(age / Math.max(1, fx.duration));
            switch (fx.type) {
                case DESCENT_LAUNCH -> launch(crystal, glow, fx, t, time);
                case LANDING -> landing(crystal, glow, fx, t, time);
                case REVELATION -> revelation(crystal, glow, fx, age, partial, time);
                case SPEAR_CHARGE -> spearCharge(crystal, glow, fx, age, partial, time);
                case SPEAR -> spear(crystal, glow, fx, age);
                case SPEAR_HIT -> spearHit(crystal, glow, fx, t, time);
                case SANCTUARY -> sanctuary(crystal, glow, fx, age, now, partial, time);
                case SANCTUARY_HIT -> { }
                case SANCTUARY_END -> sanctuaryEnd(crystal, glow, fx, t);
                case ARRAY -> array(crystal, glow, fx, age, now, partial, time);
                case ARRAY_STRIKE -> strike(crystal, glow, fx, age, now, partial, cam, time);
                case REJECTION -> rejection(crystal, glow, fx, t, time);
                case PROTOCOL_START -> protocolStart(crystal, glow, fx, age, partial, time);
                case CONVERGENCE -> ConvergenceFx.draw(crystal, glow, fx, age, partial, time);
                case CONV_FINAL -> ConvergenceFx.finalWave(crystal, glow, fx, t, time);
            }
        }
    }

    static @Nullable Entity entity(int id) {
        Minecraft mc = Minecraft.getInstance();
        return id < 0 || mc.level == null ? null : mc.level.getEntity(id);
    }

    /** A curved wall: an arc of {@code radius} around {@code centre}, spanning +/- {@code half} radians from {@code dir}. */
    static void arcWall(Geo g, Vec3 centre, Vec3 dir, double radius, double half, double height, int segments) {
        Vec3 f = new Vec3(dir.x, 0, dir.z);
        f = f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 prev = null;
        for (int i = 0; i <= segments; i++) {
            double a = -half + 2 * half * i / segments;
            Vec3 p = centre.add(f.scale(Math.cos(a) * radius)).add(side.scale(Math.sin(a) * radius));
            if (prev != null) g.quad(prev.add(0, -height * 0.4, 0), p.add(0, -height * 0.4, 0), p.add(0, height * 0.6, 0), prev.add(0, height * 0.6, 0));
            prev = p;
        }
    }

    // ---------------------------------------------------------------- Angel's Descent
    private static void launch(Geo crystal, Geo glow, Effect fx, double t, double time) {
        float a = (float) (1 - t);
        for (int k = 0; k < 4; k++) {
            double r = 0.6 + t * (2.2 + k * 0.9);
            Vec3 c = fx.pos.add(0, 0.05 - k * 0.15 * t, 0);
            glow.color(0.85f, 0.97f, 1f, 0.6f * a).ring(c, UP, r, 0.04, 48, time * 0.03 + k, k % 2 == 1);
        }
        crystal.color(0.85f, 0.97f, 1f, 0.12f * a).ring(fx.pos.add(0, 0.05, 0), UP, 0.8 + 2.5 * t, 0.6, 48, 0, false);
    }

    private static void landing(Geo crystal, Geo glow, Effect fx, double t, double time) {
        double r = fx.aux.x * smooth(t * 1.2);
        float a = (float) (1 - t);
        glow.color(1f, 1f, 1f, 0.8f * a).ring(fx.pos.add(0, 0.08, 0), UP, r, 0.07, 64, 0, false);
        glow.color(0.7f, 0.95f, 1f, 0.5f * a).hexRing(fx.pos.add(0, 0.1, 0), UP, r * 0.7, 0.04, time * 0.02);
        crystal.color(0.8f, 0.96f, 1f, 0.1f * a).ring(fx.pos.add(0, 0.6, 0), UP, r, 0.6, 64, 0, false);
    }

    // ---------------------------------------------------------------- AIM Revelation
    private static void revelation(Geo crystal, Geo glow, Effect fx, double age, float partial, double time) {
        Entity e = entity(fx.a);
        Body b = e == null ? null : Body.of(e, partial);
        Vec3 feet = b == null ? fx.pos : b.feet;
        double in = smooth(age / 10.0), out = smooth((fx.duration - age) / 10.0);
        float a = (float) Math.min(in, out);
        if (b != null) {
            Vec3 c = b.feet.add(0, b.height + 0.2, 0).subtract(b.fwd.scale(0.45));
            for (int k = 0; k < 3; k++) {
                double tilt = 0.3 + 0.45 * k;
                double spin = time * (0.02 + 0.015 * k) * (k == 1 ? -1 : 1);
                Vec3 n = b.fwd.scale(Math.cos(tilt)).add(b.up.scale(Math.sin(tilt))).normalize();
                glow.color(0.85f, 0.97f, 1f, (0.75f - 0.15f * k) * a).ring(c, n, 0.6 + 0.25 * k, 0.025, 48, spin, k == 1);
            }
            glow.color(1f, 1f, 1f, 0.5f * a).hexRing(c, b.fwd, 0.35, 0.02, time * 0.04);
        }
        // The scanning plane expands, then contracts.
        double grow = smooth(age / (fx.duration * 0.45));
        double shrink = smooth((age - fx.duration * 0.6) / (fx.duration * 0.4));
        double r = fx.aux.x * grow * (1 - shrink);
        if (r > 0.2) {
            Vec3 c = feet.add(0, 1.0, 0);
            crystal.color(0.6f, 0.9f, 1f, 0.07f * a).ring(c, UP, r * 0.5, r * 0.5, 64, 0, false);
            glow.color(0.75f, 0.96f, 1f, 0.6f * a).ring(c, UP, r, 0.05, 96, time * 0.01, false);
            glow.color(0.85f, 1f, 1f, 0.35f * a).ring(c, UP, r * 0.66, 0.025, 72, -time * 0.01, true);
        }
    }

    // ---------------------------------------------------------------- Celestial Spear
    private static void spearCharge(Geo crystal, Geo glow, Effect fx, double age, float partial, double time) {
        Entity e = entity(fx.a);
        if (e == null) return;
        Body b = Body.of(e, partial);
        Vec3 hand = b.raisedHand().add(0, 0.35, 0);
        double t = smooth(age / Math.max(1, fx.duration - 2));
        Vec3 axis = b.fwd.scale(0.85).add(b.up.scale(0.5)).normalize();
        int sections = 6;
        for (int i = 0; i < sections; i++) {
            double ang = time * 0.35 + i * Math.PI * 2 / sections;
            Vec3[] uv = Geo.basis(axis);
            double rad = 0.75 * (1 - t);
            Vec3 at = hand.add(uv[0].scale(Math.cos(ang) * rad)).add(uv[1].scale(Math.sin(ang) * rad));
            crystal.color(0.88f, 0.98f, 1f, 0.65f).crystal(at, axis, 0.05, 0.45 + 0.4 * t, ang);
        }
        glow.color(0.75f, 0.96f, 1f, (float) (0.4 + 0.5 * t)).crystal(hand, axis, 0.06 + 0.04 * t, 0.3 + 1.0 * t, time * 0.3);
        glow.color(1f, 1f, 1f, (float) (0.5 * t)).hexRing(hand, axis, 0.5 * (1 - t) + 0.15, 0.02, time * 0.1);
    }

    private static void spear(Geo crystal, Geo glow, Effect fx, double age) {
        double speed = fx.aux.length();
        if (speed < 1e-4) return;
        Vec3 dir = fx.aux.scale(1 / speed);
        Vec3 at = fx.pos.add(fx.aux.scale(age));
        if (at.distanceTo(fx.pos) > fx.power + 2) return;
        double spin = age * 0.9;
        crystal.color(0.92f, 0.99f, 1f, 0.85f).crystal(at, dir, 0.13, 1.35, spin);
        glow.color(0.7f, 0.95f, 1f, 0.9f).crystal(at, dir, 0.06, 1.5, spin);
        Vec3[] uv = Geo.basis(dir);
        for (int k = 0; k < 3; k++) {
            double ang = spin + k * Math.PI * 2 / 3;
            Vec3 fin = uv[0].scale(Math.cos(ang)).add(uv[1].scale(Math.sin(ang)));
            Vec3 base = at.subtract(dir.scale(0.6));
            crystal.color(0.85f, 0.97f, 1f, 0.55f).kite(base, base.subtract(dir.scale(0.7)).add(fin.scale(0.3)), fin, 0.06);
        }
        Vec3 tail = at.subtract(dir.scale(Math.min(age * speed, 4.0)));
        glow.color(0.65f, 0.92f, 1f, 0.45f).ribbon(tail, at.subtract(dir.scale(1.3)), 0.035);
    }

    private static void spearHit(Geo crystal, Geo glow, Effect fx, double t, double time) {
        Vec3 dir = fx.aux.lengthSqr() < 1e-6 ? UP : fx.aux.normalize();
        Vec3[] uv = Geo.basis(dir);
        float a = (float) (1 - t);
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3 + time * 0.05;
            Vec3 out = uv[0].scale(Math.cos(ang)).add(uv[1].scale(Math.sin(ang)));
            crystal.color(0.9f, 0.98f, 1f, 0.6f * a).crystal(fx.pos.add(out.scale(0.3 + 1.4 * t)), out, 0.05, 0.4 * (1 - t * 0.5), ang);
        }
        for (int k = 0; k < 2; k++) glow.color(0.8f, 0.97f, 1f, 0.8f * a).hexRing(fx.pos, dir, 0.3 + t * (1.2 + k * 0.8), 0.04, k * 0.5);
    }

    // ---------------------------------------------------------------- Sanctuary
    private static void sanctuary(Geo crystal, Geo glow, Effect fx, double age, long now, float partial, double time) {
        Entity e = entity(fx.a);
        if (e == null) return;
        Body b = Body.of(e, partial);
        double close = smooth(age / 10.0);
        double out = smooth((fx.duration - age) / 6.0);
        float a = (float) Math.min(close, out);
        if (a <= 0.01f) return;
        // Recent hits: direction and strength of the dent.
        Vec3 hitDir = null;
        double dent = 0;
        for (Effect h : ClientFx.ACTIVE.values()) {
            if (h.type != FxType.SANCTUARY_HIT || h.a != fx.a) continue;
            double ht = clamp01(h.age(now, partial) / h.duration);
            double strength = (1 - ht) * (1 - ht);
            if (strength > dent && h.aux.lengthSqr() > 1e-6) { dent = strength; hitDir = new Vec3(h.aux.x, 0, h.aux.z).normalize(); }
        }
        double rx = Math.max(0.75, e.getBbWidth() * 0.9 + 0.35);
        double top = b.height + 0.45;
        int feathers = 16;
        for (int layer = 0; layer < 2; layer++) {
            double lr = rx * (1.0 + 0.18 * layer);
            for (int i = 0; i < feathers; i++) {
                double ang = (i + 0.5 * layer) * Math.PI * 2 / feathers + 0.15 * (1 - close);
                Vec3 radial = b.right.scale(Math.cos(ang)).add(b.fwd.scale(Math.sin(ang)));
                double push = 0;
                if (hitDir != null) push = Math.max(0, radial.dot(hitDir)) * dent * 0.35;
                double open = (1 - close) * 0.9;
                Vec3 topP = b.feet.add(0, top, 0).add(radial.scale(lr * 0.35 + open));
                Vec3 mid = b.feet.add(0, top * 0.55, 0).add(radial.scale(lr * (1.05 - push) + open * 1.5));
                Vec3 bottom = b.feet.add(0, 0.05, 0).add(radial.scale(lr * (0.9 - push * 0.5) + open * 2));
                Vec3 side = new Vec3(-radial.z, 0, radial.x);
                double w = lr * Math.PI * 2 / feathers * 0.62;
                float shade = layer == 0 ? 0.95f : 0.85f;
                crystal.color(shade, 0.99f, 1f, (layer == 0 ? 0.42f : 0.22f) * a).kite(topP, mid, side, w * 0.8);
                crystal.color(shade, 0.98f, 1f, (layer == 0 ? 0.38f : 0.2f) * a).kite(mid, bottom, side, w);
                if (layer == 0) glow.color(0.75f, 0.96f, 1f, 0.35f * a).ribbon(topP, bottom, 0.01);
            }
        }
        if (hitDir != null && dent > 0.01) {
            Vec3 c = b.chest().add(hitDir.scale(rx * 0.95));
            for (int k = 0; k < 3; k++)
                glow.color(1f, 1f, 1f, (float) (0.8 * dent)).hexRing(c, hitDir, 0.2 + (1 - dent) * (0.8 + 0.5 * k), 0.03, k);
        }
        glow.color(0.85f, 0.97f, 1f, 0.5f * a).ring(b.feet.add(0, top, 0), UP, rx * 0.4, 0.03, 32, time * 0.03, false);
    }

    private static void sanctuaryEnd(Geo crystal, Geo glow, Effect fx, double t) {
        float a = (float) (1 - t);
        int feathers = 16;
        for (int i = 0; i < feathers; i++) {
            double ang = i * Math.PI * 2 / feathers;
            Vec3 radial = new Vec3(Math.cos(ang), 0, Math.sin(ang));
            Vec3 base = fx.pos.add(radial.scale(0.6 + 2.2 * t)).add(0, 0.6 * t, 0);
            Vec3 tip = base.add(radial.scale(1.0)).add(0, 1.0 - 1.6 * t, 0);
            crystal.color(0.95f, 0.99f, 1f, 0.45f * a).kite(base, tip, new Vec3(-radial.z, 0, radial.x), 0.16);
        }
    }

    // ---------------------------------------------------------------- Seraphic Array
    /** Where construct {@code i} of {@code n} floats behind the caster. */
    static Vec3 constructPos(Body b, int i, int n, double time) {
        double spread = n <= 1 ? 0 : (i / (double) (n - 1) - 0.5) * 2;   // -1 .. 1
        return b.back().subtract(b.fwd.scale(0.9)).add(b.right.scale(spread * 1.6)).add(0, 0.9 + 0.5 * (1 - Math.abs(spread)) + 0.12 * Math.sin(time * 0.08 + i), 0);
    }

    static void construct(Geo crystal, Geo glow, Vec3 at, Vec3 axis, double scale, float a, double phase) {
        Vec3 ax = axis.normalize();
        Vec3[] uv = Geo.basis(ax);
        crystal.color(0.9f, 0.98f, 1f, 0.6f * a).crystal(at, ax, 0.09 * scale, 0.85 * scale, phase);
        glow.color(0.7f, 0.95f, 1f, 0.7f * a).ribbon(at.subtract(ax.scale(0.8 * scale)), at.add(ax.scale(0.9 * scale)), 0.015 * scale);
        for (int k = 0; k < 3; k++) {
            Vec3 base = at.subtract(ax.scale((0.5 - 0.3 * k) * scale));
            Vec3 vane = uv[0].scale(Math.cos(phase * 0.2)).add(uv[1].scale(Math.sin(phase * 0.2)));
            Vec3 tip = base.subtract(ax.scale(0.55 * scale)).add(vane.scale((0.45 - 0.1 * k) * scale));
            crystal.color(0.95f, 0.99f, 1f, 0.45f * a).kite(base, tip, ax, 0.09 * scale);
            Vec3 tip2 = base.subtract(ax.scale(0.55 * scale)).subtract(vane.scale((0.45 - 0.1 * k) * scale));
            crystal.color(0.95f, 0.99f, 1f, 0.45f * a).kite(base, tip2, ax, 0.09 * scale);
        }
    }

    private static void array(Geo crystal, Geo glow, Effect fx, double age, long now, float partial, double time) {
        Entity e = entity(fx.a);
        if (e == null) return;
        Body b = Body.of(e, partial);
        int n = Math.max(1, Math.min(5, fx.c));
        double in = smooth(age / 12.0), out = smooth((fx.duration - age) / 10.0);
        float a = (float) Math.min(in, out);
        if (a <= 0.01f) return;
        for (int i = 0; i < n; i++) {
            Vec3 at = constructPos(b, i, n, time);
            Vec3 axis = b.up.scale(0.8).add(b.right.scale(n <= 1 ? 0 : (i / (double) (n - 1) - 0.5) * 0.8));
            double scale = in;
            for (Effect s : ClientFx.ACTIVE.values()) {
                if (s.type != FxType.ARRAY_STRIKE || s.a != fx.a || s.c != i) continue;
                double sa = s.age(now, partial);
                Entity target = entity(s.b);
                if (sa < 10 && target != null) {
                    Vec3 to = Body.of(target, partial).chest().subtract(at);
                    double turn = smooth(sa / 5.0);
                    axis = axis.normalize().scale(1 - turn).add(to.normalize().scale(turn));
                    scale *= 1 - 0.3 * smooth((sa - 3) / 3.0);
                }
            }
            construct(crystal, glow, at, axis, scale, a, time * 0.05 + i);
        }
    }

    private static void strike(Geo crystal, Geo glow, Effect fx, double age, long now, float partial, Vec3 cam, double time) {
        Entity caster = entity(fx.a), target = entity(fx.b);
        Vec3 to = target == null ? fx.pos : Body.of(target, partial).chest();
        Vec3 from;
        if (caster == null) {
            from = to.add(0, 3, 0);
        } else {
            Body b = Body.of(caster, partial);
            from = ConvergenceFx.active(fx.a) ? ConvergenceFx.constructPos(b, fx.c, time) : constructPos(b, fx.c, 4, time);
        }
        if (age < 6) return;
        double t = (age - 6) / 4.0;
        if (t < 1) {
            Vec3 at = from.add(to.subtract(from).scale(smooth(t)));
            glow.color(0.85f, 0.98f, 1f, 0.85f).ribbon(from.add(to.subtract(from).scale(Math.max(0, smooth(t) - 0.4))), at, 0.05);
            crystal.color(0.95f, 1f, 1f, 0.8f).crystal(at, to.subtract(from), 0.1, 0.6, age);
        } else {
            double bt = clamp01((age - 10) / Math.max(1, fx.duration - 10));
            float a = (float) (1 - bt);
            Vec3 toCam = cam.subtract(to);
            Vec3 n = toCam.lengthSqr() < 1e-6 ? UP : toCam.normalize();
            for (int k = 0; k < 3; k++) glow.color(0.85f, 0.97f, 1f, 0.8f * a).hexRing(to, n, 0.25 + bt * (0.8 + 0.5 * k), 0.035, k + time * 0.02);
        }
    }

    // ---------------------------------------------------------------- Heaven's Rejection
    private static void rejection(Geo crystal, Geo glow, Effect fx, double t, double time) {
        double range = fx.power;
        double r = 0.8 + range * smooth(t * 1.15);
        float a = (float) Math.pow(1 - t, 1.2);
        double half = Math.toRadians(62);
        Vec3 base = fx.pos.add(0, -0.4, 0);
        crystal.color(0.82f, 0.96f, 1f, 0.18f * a);
        arcWall(crystal, base, fx.aux, r, half, 2.6 * (1 - 0.4 * t), 28);
        crystal.color(0.9f, 0.99f, 1f, 0.1f * a);
        arcWall(crystal, base, fx.aux, r * 0.82, half * 0.9, 2.0, 24);
        Vec3 f = new Vec3(fx.aux.x, 0, fx.aux.z);
        f = f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 prevTop = null, prevBottom = null;
        for (int i = 0; i <= 28; i++) {
            double ang = -half + 2 * half * i / 28;
            Vec3 p = base.add(f.scale(Math.cos(ang) * r)).add(side.scale(Math.sin(ang) * r));
            Vec3 topP = p.add(0, 2.6 * (1 - 0.4 * t) * 0.6, 0), bottomP = p.add(0, -2.6 * 0.4 * (1 - 0.4 * t), 0);
            if (prevTop != null) {
                glow.color(1f, 1f, 1f, 0.75f * a).ribbon(prevTop, topP, 0.03);
                glow.color(0.7f, 0.95f, 1f, 0.5f * a).ribbon(prevBottom, bottomP, 0.025);
            }
            if (i % 4 == 0) glow.color(0.75f, 0.96f, 1f, 0.35f * a).hexRing(p, f, 0.35, 0.015, time * 0.03 + i);
            prevTop = topP;
            prevBottom = bottomP;
        }
    }

    // ---------------------------------------------------------------- Archangel Protocol (transformation sequence)
    private static void protocolStart(Geo crystal, Geo glow, Effect fx, double age, float partial, double time) {
        Entity e = entity(fx.a);
        if (e == null) return;
        Body b = Body.of(e, partial);
        float fade = (float) smooth((fx.duration - age) / 14.0);
        // Awakening: a large halo behind the head grows.
        double grow = smooth(age / 30.0);
        Vec3 haloC = b.feet.add(0, b.height + 0.25, 0).subtract(b.fwd.scale(0.5));
        glow.color(0.95f, 0.99f, 1f, 0.8f * fade).ring(haloC, b.fwd, 1.2 * grow, 0.035, 56, time * 0.02, false);
        glow.color(0.7f, 0.95f, 1f, 0.5f * fade).hexRing(haloC, b.fwd, 0.8 * grow, 0.025, -time * 0.03);
        // Completion: a controlled outward expansion of hexagonal geometry.
        if (age >= 40) {
            double t = clamp01((age - 40) / 20.0);
            float a = (float) (1 - t);
            for (int k = 0; k < 3; k++)
                glow.color(0.9f, 0.98f, 1f, 0.7f * a).hexRing(b.chest(), b.up, 0.8 + t * (3 + k * 1.5), 0.05, k * 0.4 + time * 0.01);
            crystal.color(0.85f, 0.97f, 1f, 0.12f * a).sphere(b.chest(), 0.6 + 2.6 * t, 8, 16);
        }
    }
}
