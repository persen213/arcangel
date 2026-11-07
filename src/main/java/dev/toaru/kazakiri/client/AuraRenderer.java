package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.ClientCfg;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws every visible player's wings, halos and forms from AuraClient, so everyone sees everyone's wings.
 * Protocol form: a second lower wing pair, three independently rotating halos, and bright geometry around the body.
 * Convergence form: brighter, warmer wings.
 */
final class AuraRenderer {
    private AuraRenderer() { }

    private static final double MAX_DISTANCE = 128.0;

    static void draw(Geo crystal, Geo glow, Vec3 cam, float partial, double time) {
        if (AuraClient.AURAS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Map.Entry<Integer, AuraClient.Aura> entry : AuraClient.AURAS.entrySet()) {
            Entity e = mc.level.getEntity(entry.getKey());
            if (e == null || e.isInvisible()) continue;
            if (e == mc.player && mc.options.getCameraType().isFirstPerson() && !ClientCfg.SHOW_OWN_WINGS_FIRST_PERSON.get()) continue;
            Body body = Body.of(e, partial);
            if (body.feet.distanceTo(cam) > MAX_DISTANCE) continue;
            AuraClient.Aura a = entry.getValue();
            float open = a.open(partial), halo = a.halo(partial), arch = a.arch(partial), form = a.form(partial);
            double flap = a.flap(partial);
            WingModel.draw(crystal, glow, body, open, flap, a.beat(partial), a.vy, a.hspeed,
                    1.0f + 0.2f * form + 0.2f * arch, Math.min(1f, open * 1.4f), arch > 0.5f);
            if (form > 0.01f) {
                WingModel.draw(crystal, glow, body, open * form, flap + 1.1, a.beat(partial), a.vy, a.hspeed,
                        0.72f, Math.min(1f, open * form * 1.3f) * 0.85f, false, 0.45);
                drawForm(crystal, glow, body, form, time);
            }
            if (halo > 0.01f) drawHalo(crystal, glow, body, halo, time);
        }
    }

    private static void drawHalo(Geo crystal, Geo glow, Body b, float alpha, double time) {
        Vec3 centre = b.aboveHead();
        Vec3 normal = b.up.subtract(b.fwd.scale(0.22)).normalize();
        double r = 0.4;
        glow.color(0.95f, 0.98f, 1f, 0.9f * alpha).ring(centre, normal, r, 0.03, 40, time * 0.02, false);
        glow.color(0.7f, 0.95f, 1f, 0.45f * alpha).ring(centre, normal, r * 1.22, 0.012, 40, -time * 0.03, true);
        crystal.color(0.9f, 0.97f, 1f, 0.1f * alpha).ring(centre, normal, r, 0.09, 40, 0, false);
    }

    /** Protocol form: three halos rotating at different speeds and tilts, and slow bright rings travelling along the body. */
    private static void drawForm(Geo crystal, Geo glow, Body b, float a, double time) {
        Vec3 head = b.feet.add(0, b.height + 0.15, 0).subtract(b.fwd.scale(0.35));
        for (int k = 0; k < 3; k++) {
            double tilt = 0.35 + 0.5 * k;
            double spin = time * (0.015 + 0.012 * k) * (k == 1 ? -1 : 1);
            Vec3 n = b.fwd.scale(Math.cos(tilt)).add(b.right.scale(Math.sin(tilt) * Math.cos(spin * 0.5))).add(b.up.scale(Math.sin(tilt) * Math.sin(spin * 0.5))).normalize();
            double r = 0.75 + 0.22 * k;
            glow.color(0.85f, 0.97f, 1f, (0.65f - 0.12f * k) * a).ring(head, n, r, 0.02, 48, spin * 3, k == 2);
            glow.color(1f, 1f, 1f, 0.35f * a).hexRing(head, n, r * 0.55, 0.015, -spin * 2);
        }
        for (int k = 0; k < 3; k++) {
            double ph = ((time * 0.02 + k / 3.0) % 1.0);
            Vec3 c = b.feet.add(0, ph * b.height, 0);
            float ra = (float) (Math.sin(ph * Math.PI) * 0.35 * a);
            glow.color(0.8f, 0.97f, 1f, ra).ring(c, b.up, 0.45, 0.015, 32, time * 0.05, true);
        }
        crystal.color(0.9f, 0.98f, 1f, 0.06f * a).ring(b.chest(), b.up, 0.42, 0.55, 24, 0, false);
    }
}
