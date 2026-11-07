package dev.toaru.kazakiri;

import net.minecraft.server.level.ServerPlayer;

/**
 * Temporary flight permission while a skill keeps the player in the air (Angel's Descent, the Protocol awakening, the
 * Convergence hover), so the server does not kick them for flying. Counted, only granted to players who did not already
 * have it, always taken back, and a saved tag removes it on the next login if the server stopped mid-skill.
 */
public final class Flight {
    private Flight() { }
    public static final String TAG = "kazakiri_flight";

    public static void acquire(ServerPlayer p, State s) {
        s.flightUsers++;
        if (s.grantedFlight || p.isCreative() || p.isSpectator() || p.getAbilities().mayfly) return;
        p.getAbilities().mayfly = true;
        p.onUpdateAbilities();
        p.getPersistentData().putBoolean(TAG, true);
        s.grantedFlight = true;
    }

    public static void release(ServerPlayer p, State s) {
        s.flightUsers = Math.max(0, s.flightUsers - 1);
        if (s.flightUsers == 0) revoke(p, s);
    }

    public static void revoke(ServerPlayer p, State s) {
        s.flightUsers = 0;
        if (!s.grantedFlight) return;
        s.grantedFlight = false;
        p.getPersistentData().remove(TAG);
        if (p.isCreative() || p.isSpectator()) return;
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }
}
