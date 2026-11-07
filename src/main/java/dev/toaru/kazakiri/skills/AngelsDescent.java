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
 * Angel's Descent (toggle, up to 7 s): wings manifest and launch the caster upward. While active the caster glides (fall
 * speed limited), accelerates along the look direction while sprinting, and drops faster while sneaking. Touching the
 * ground after being airborne ends it with a small pressure wave. Press again to end early. Not unlimited flight: the
 * movement is only steered by the server for the skill's duration (flight permission is held only to avoid the
 * server's "flying" kick).
 */
public final class AngelsDescent implements SkillBehavior {
    private static final class Run { boolean airborne; }
    private static final Map<UUID, Run> RUNS = new HashMap<>();

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        State s = State.of(p);
        if (s.descent) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.DESCENT_CP), Cfg.f(Cfg.DESCENT_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        s.descent = true;
        Flight.acquire(p, s);
        double lift = Cfg.DESCENT_LIFT.get() * (s.protocol ? 1.3 : 1.0);
        Vec3 m = p.getDeltaMovement();
        Targeting.setMotion(p, new Vec3(m.x, lift, m.z));
        p.fallDistance = 0f;
        RUNS.put(p.getUUID(), new Run());
        State.updateAura(p);
        State.anim(p, Anims.DESCENT);
        ServerLevel level = p.serverLevel();
        Fx.make(level, p.position(), FxType.DESCENT_LAUNCH, 14).range(96).send();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.6f, 0.7f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.get(p.getUUID());
        if (p == null || r == null || c.age() >= Cfg.ticks(Cfg.DESCENT_SECONDS)) return false;
        p.fallDistance = 0f;
        if (!p.onGround()) {
            r.airborne = true;
        } else if (r.airborne && c.age() > 6) {
            landing(c, p);
            return false;
        }
        Vec3 m = p.getDeltaMovement();
        double maxFall = Cfg.DESCENT_GLIDE_FALL.get() * (p.isShiftKeyDown() ? 4.0 : 1.0);
        boolean changed = false;
        double vx = m.x, vy = m.y, vz = m.z;
        if (vy < -maxFall) { vy = -maxFall; changed = true; }
        if (p.isSprinting()) {
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() > 1e-6) {
                flat = flat.normalize().scale(Cfg.DESCENT_ACCEL.get());
                vx += flat.x; vz += flat.z;
                double max = Cfg.DESCENT_MAX_SPEED.get() * (State.protocol(p) ? 1.3 : 1.0);
                double h = Math.hypot(vx, vz);
                if (h > max) { vx *= max / h; vz *= max / h; }
                changed = true;
            }
        }
        if (changed) Targeting.setMotion(p, new Vec3(vx, vy, vz));
        if (c.age() % 20 == 0) c.addProficiency(0.003f);
        return true;
    }

    private static void landing(ActivationContext c, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        double radius = Cfg.DESCENT_LAND_RADIUS.get();
        for (LivingEntity e : Targeting.livingWithin(level, p.position(), radius, p)) {
            if (!Targeting.hostile(actor, e, KazakiriMod.ANGELS_DESCENT)) continue;
            if (!AcademySafety.attack(actor, e, KazakiriMod.ANGELS_DESCENT, Cfg.f(Cfg.DESCENT_LAND_DAMAGE))) continue;
            Vec3 away = Targeting.flatDir(p.position(), e.position(), p.getLookAngle());
            double k = Cfg.DESCENT_LAND_KNOCKBACK.get() * Targeting.controlFactor(e);
            Targeting.setMotion(e, e.getDeltaMovement().add(away.x * k, 0.25, away.z * k));
        }
        Fx.make(level, p.position(), FxType.LANDING, 16).aux(new Vec3(radius, 0, 0)).range(96).send();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.4f, 1.2f);
        c.addProficiency(0.01f);
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        RUNS.remove(p == null ? null : p.getUUID());
        State s = p == null ? null : State.peek(p);
        if (p != null && s != null) {
            s.descent = false;
            Flight.release(p, s);
            p.fallDistance = 0f;
            if (p.isAlive() && !p.onGround() && !p.isCreative() && !p.isSpectator())
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
            State.updateAura(p);
        }
        c.setCooldown(Cfg.ticks(Cfg.DESCENT_COOLDOWN));
    }

    public static void forget(UUID id) { RUNS.remove(id); }
    public static void clearAll() { RUNS.clear(); }
}
