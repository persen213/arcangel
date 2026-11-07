package dev.toaru.kazakiri;

import dev.toaru.kazakiri.net.AnimMsg;
import dev.toaru.kazakiri.net.AuraMsg;
import dev.toaru.kazakiri.net.Net;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Per-player server state. Server thread only, never saved (except the flight tag in Flight). */
public final class State {
    private static final Map<UUID, State> ALL = new HashMap<>();
    /** Protected entity -> shield (Sanctuary can protect allies too, so it is keyed by the protected entity). */
    private static final Map<UUID, Shield> SHIELDS = new HashMap<>();
    /** Active AIM Convergence fields: caster -> centre (allies inside take less damage). */
    private static final Map<UUID, Vec3> CONVERGENCE = new HashMap<>();

    /** Aura flags sent to clients. */
    public static final int WINGS = 1, HALO = 2, PROTOCOL = 4, CONVERGENCE_FORM = 8;

    public boolean descent, protocol, convergence;
    public boolean grantedFlight;
    public int flightUsers;
    private int sentFlags = -1;

    public static final class Shield {
        public float hp;
        public long fx;
        public UUID caster;
        public long lastFlash;
    }

    public static State of(ServerPlayer p) { return ALL.computeIfAbsent(p.getUUID(), k -> new State()); }
    public static @Nullable State peek(ServerPlayer p) { return ALL.get(p.getUUID()); }
    public static void forget(UUID id) { ALL.remove(id); CONVERGENCE.remove(id); }
    public static void clearAll() { ALL.clear(); SHIELDS.clear(); CONVERGENCE.clear(); }

    public static boolean protocol(ServerPlayer p) { State s = peek(p); return s != null && s.protocol; }

    public static @Nullable Shield shield(UUID entity) { return SHIELDS.get(entity); }
    public static void putShield(UUID entity, Shield s) { SHIELDS.put(entity, s); }
    public static void removeShield(UUID entity) { SHIELDS.remove(entity); }

    public static void setConvergence(UUID caster, @Nullable Vec3 centre) {
        if (centre == null) CONVERGENCE.remove(caster); else CONVERGENCE.put(caster, centre);
    }
    public static Map<UUID, Vec3> convergenceFields() { return CONVERGENCE; }

    public int flags() {
        int f = 0;
        if (descent || protocol || convergence) f |= WINGS;
        if (protocol || convergence) f |= HALO;
        if (protocol) f |= PROTOCOL;
        if (convergence) f |= CONVERGENCE_FORM;
        return f;
    }

    /** Sends the player's aura (wings / halos / forms) to everyone who can see them, if it changed. */
    public static void updateAura(ServerPlayer p) {
        State s = of(p);
        int f = s.flags();
        if (f == s.sentFlags) return;
        s.sentFlags = f;
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new AuraMsg(p.getId(), f));
    }

    public static void sendAuraTo(ServerPlayer target, ServerPlayer viewer) {
        State s = peek(target);
        if (s == null || s.flags() == 0) return;
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new AuraMsg(target.getId(), s.flags()));
    }

    /** Plays a body pose (see Anims) on this player for everyone who can see them. 0 = stop. */
    public static void anim(ServerPlayer p, int anim) {
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new AnimMsg(p.getId(), anim));
    }
}
