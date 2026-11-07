package dev.toaru.kazakiri.skills;

import dev.toaru.kazakiri.Anims;
import dev.toaru.kazakiri.Cfg;
import dev.toaru.kazakiri.Flight;
import dev.toaru.kazakiri.KazakiriMod;
import dev.toaru.kazakiri.State;
import dev.toaru.kazakiri.Targeting;
import dev.toaru.kazakiri.net.Fx;
import dev.toaru.kazakiri.net.FxType;
import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Archangel Protocol (toggle, 12 s): a fuller manifestation. Awakening: the caster floats up for 2 s while the halos and
 * the second wing pair assemble (client). Active: Speed II, and the other skills are stronger (spear damage, one more
 * Seraphic construct, tougher Sanctuary, stronger Angel's Descent with instant wings). Not invulnerable.
 * Expiry: halos slow, wings fold, the caster sinks gently (Slow Falling).
 */
public final class ArchangelProtocol implements SkillBehavior {
    static final int AWAKEN = 40;

    private static final class Run { double baseY; boolean flight; }
    private static final Map<UUID, Run> RUNS = new HashMap<>();

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        State s = State.of(p);
        if (s.protocol || s.convergence) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.PROTOCOL_CP), Cfg.f(Cfg.PROTOCOL_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        s.protocol = true;
        Run r = new Run();
        r.baseY = p.getY();
        Flight.acquire(p, s);
        r.flight = true;
        RUNS.put(p.getUUID(), r);
        State.updateAura(p);
        State.anim(p, Anims.PROTOCOL);
        ServerLevel level = p.serverLevel();
        Fx.make(level, p.position(), FxType.PROTOCOL_START, 64).a(p.getId()).range(128).send();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.8f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.get(p.getUUID());
        State s = p == null ? null : State.peek(p);
        int age = c.age();
        if (p == null || r == null || s == null || !s.protocol || age >= Cfg.ticks(Cfg.PROTOCOL_SECONDS)) return false;
        if (age < AWAKEN) {
            Vec3 m = p.getDeltaMovement();
            double vy = Math.max(-0.2, Math.min(0.2, (r.baseY + 1.5 - p.getY()) * 0.15));
            Targeting.setMotion(p, new Vec3(m.x * 0.3, vy, m.z * 0.3));
            p.fallDistance = 0f;
        } else if (age == AWAKEN) {
            if (r.flight) { Flight.release(p, s); r.flight = false; }
            if (!p.onGround()) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 0.6f);
        }
        if (age % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false, true));
            c.addProficiency(0.003f);
        }
        if (age < AWAKEN + 20) p.fallDistance = 0f;
        return true;
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.remove(p.getUUID());
        State s = p == null ? null : State.peek(p);
        if (p != null && s != null) {
            s.protocol = false;
            if (r != null && r.flight) Flight.release(p, s);
            p.removeEffect(MobEffects.MOVEMENT_SPEED);
            p.fallDistance = 0f;
            if (p.isAlive() && !p.onGround() && !p.isCreative() && !p.isSpectator())
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
            State.updateAura(p);
            if (reason != StopReason.FINISHED) State.anim(p, Anims.STOP);
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4f, 0.8f);
        }
        c.setCooldown(Cfg.ticks(Cfg.PROTOCOL_COOLDOWN));
    }

    public static void forget(UUID id) { RUNS.remove(id); }
    public static void clearAll() { RUNS.clear(); }
}
