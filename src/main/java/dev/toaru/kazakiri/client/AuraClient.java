package dev.toaru.kazakiri.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Wings / halo state of every player this client can see. The server only sends on/off flags; the smooth unfolding,
 * folding and flapping are animated here, per tick, with values interpolated per frame.
 */
public final class AuraClient {
    private AuraClient() { }

    public static final int WINGS = 1, HALO = 2, PROTOCOL = 4, CONVERGENCE = 8;

    static final class Aura {
        int flags;
        float open, openO, halo, haloO, arch, archO, form, formO;
        double flap, flapO;
        float beat, beatO;
        double vy, hspeed;
        int missing;

        float open(float pt) { return openO + (open - openO) * pt; }
        float halo(float pt) { return haloO + (halo - haloO) * pt; }
        float arch(float pt) { return archO + (arch - archO) * pt; }
        float form(float pt) { return formO + (form - formO) * pt; }
        double flap(float pt) { return flapO + (flap - flapO) * pt; }
        float beat(float pt) { return beatO + (beat - beatO) * pt; }
        boolean idle() { return flags == 0 && open < 0.01f && halo < 0.01f && arch < 0.01f && form < 0.01f; }
    }

    static final Map<Integer, Aura> AURAS = new HashMap<>();

    static void set(int entityId, int flags) {
        Aura a = AURAS.computeIfAbsent(entityId, k -> new Aura());
        a.flags = flags;
        a.missing = 0;
    }

    static void clear() { AURAS.clear(); }

    private static float approach(float v, float target, float rate) { return v + (target - v) * rate; }

    static void tick() {
        if (AURAS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { AURAS.clear(); return; }
        AURAS.entrySet().removeIf(entry -> {
            Aura a = entry.getValue();
            a.openO = a.open; a.haloO = a.halo; a.archO = a.arch; a.formO = a.form; a.flapO = a.flap; a.beatO = a.beat;
            boolean wings = (a.flags & WINGS) != 0, halo = (a.flags & HALO) != 0, protocol = (a.flags & PROTOCOL) != 0, arch = (a.flags & CONVERGENCE) != 0;
            a.open = approach(a.open, wings ? 1f : 0f, wings ? (protocol ? 0.35f : 0.09f) : 0.14f);
            a.form = approach(a.form, protocol ? 1f : 0f, protocol ? 0.06f : 0.08f);
            a.halo = approach(a.halo, halo ? 1f : 0f, 0.12f);
            a.arch = approach(a.arch, arch ? 1f : 0f, 0.08f);
            Entity e = mc.level.getEntity(entry.getKey());
            if (e == null) {
                a.missing++;
            } else {
                a.missing = 0;
                a.vy = e.getY() - e.yo;
                a.hspeed = Math.hypot(e.getX() - e.xo, e.getZ() - e.zo);
                float wantBeat = (float) Math.min(1.0, 0.35 + (a.vy > 0.04 ? 0.6 : 0.0) + Math.min(a.hspeed, 0.6));
                if (e.onGround()) wantBeat = 0.15f;
                a.beat = approach(a.beat, wantBeat, 0.1f);
                a.flap += (0.16 + 0.22 * a.beat) * Math.max(0.2, a.open);
            }
            return a.missing > 200 || (a.idle() && a.missing > 0) || (a.idle() && a.flags == 0);
        });
    }
}
