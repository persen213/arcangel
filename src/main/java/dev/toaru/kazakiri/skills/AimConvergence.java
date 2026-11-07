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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.Entity;

/**
 * AIM Convergence (ultimate, 10 s). Server timeline (clients draw the same timeline from one CONVERGENCE effect):
 * 0-40 Awakening: rise and hover.            40-80 Manifestation: the giant projection forms (client).
 * 80-120 Convergence: constructs gather.      120-160 Judgment: constructs strike enemies in sequence; enemies in the
 *                                             field are slowed; allies inside take less damage (ServerEvents).
 * 165 Release: one synchronized wing strike (damage + displacement).   170-200: descent and fade.
 * The caster takes reduced damage throughout. Cancelling before the release deals no final damage and costs half the
 * cooldown. Death, logout and dimension change end it cleanly.
 */
public final class AimConvergence implements SkillBehavior {
    static final int AWAKEN = 40, JUDGMENT = 120, RELEASE = 165, DESCEND = 170, TOTAL = 200;

    private static final class Run {
        double baseY;
        Vec3 centre;
        long fx;
        boolean released;
        int strikes;
        final List<UUID> field = new ArrayList<>();
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        State s = State.of(p);
        if (s.convergence) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.CONV_CP), Cfg.f(Cfg.CONV_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        s.convergence = true;
        Flight.acquire(p, s);
        Run r = new Run();
        r.baseY = p.getY();
        r.centre = p.position();
        ServerLevel level = p.serverLevel();
        r.fx = Fx.make(level, p.position(), FxType.CONVERGENCE, TOTAL + 4).a(p.getId()).aux(new Vec3(Cfg.CONV_RADIUS.get(), 0, 0)).range(192).send();
        RUNS.put(p.getUUID(), r);
        State.setConvergence(p.getUUID(), r.centre);
        State.updateAura(p);
        State.anim(p, Anims.CONVERGENCE);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0f, 0.5f);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0f, 1.5f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.get(p.getUUID());
        State s = p == null ? null : State.peek(p);
        int age = c.age();
        if (p == null || r == null || s == null || !s.convergence || age >= TOTAL) return false;
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        double radius = Cfg.CONV_RADIUS.get();

        // Hover, then a slow descent.
        Vec3 m = p.getDeltaMovement();
        double vy = age < DESCEND ? Math.max(-0.25, Math.min(0.25, (r.baseY + Cfg.CONV_HOVER.get() - p.getY()) * (age < AWAKEN ? 0.12 : 0.2))) : -0.08;
        Targeting.setMotion(p, new Vec3(m.x * 0.5, vy, m.z * 0.5));
        p.fallDistance = 0f;

        if (age >= JUDGMENT && age < RELEASE) {
            if (age % 10 == 0) {
                r.field.clear();
                for (LivingEntity e : Targeting.livingWithin(level, r.centre, radius, p))
                    if (Targeting.hostile(actor, e, KazakiriMod.AIM_CONVERGENCE)) r.field.add(e.getUUID());
            }
            if (age % 5 == 0) restrict(level, r);
            if ((age - JUDGMENT) % 8 == 0 && r.strikes < 6) {
                LivingEntity target = nextTarget(level, r);
                if (target != null) {
                    SeraphicArray.strike(c, p, target, r.strikes % 6, Cfg.f(Cfg.CONV_CONSTRUCT_DAMAGE), KazakiriMod.AIM_CONVERGENCE);
                    r.strikes++;
                }
            }
        }
        if (age == RELEASE) release(c, p, r, level, radius);
        if (age % 20 == 0) c.addProficiency(0.003f);
        return true;
    }

    /** Moderate movement restriction for enemies in the field (bosses much less). */
    private static void restrict(ServerLevel level, Run r) {
        for (UUID id : r.field) {
            Entity found = level.getEntity(id);
            if (!(found instanceof LivingEntity e) || !e.isAlive()) continue;
            boolean boss = Targeting.bossLike(e);
            Vec3 m = e.getDeltaMovement();
            double damp = boss ? 0.85 : 0.5;
            Targeting.setMotion(e, new Vec3(m.x * damp, m.y, m.z * damp));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, boss ? 0 : 2, false, false, false));
        }
    }

    private static LivingEntity nextTarget(ServerLevel level, Run r) {
        List<LivingEntity> alive = new ArrayList<>();
        for (UUID id : r.field) {
            Entity found = level.getEntity(id);
            if (found instanceof LivingEntity e && e.isAlive()) alive.add(e);
        }
        if (alive.isEmpty()) return null;
        alive.sort(Comparator.comparingDouble(e -> e.distanceToSqr(r.centre.x, r.centre.y, r.centre.z)));
        return alive.get(r.strikes % alive.size());
    }

    private static void release(ActivationContext c, ServerPlayer p, Run r, ServerLevel level, double radius) {
        var actor = c.actor();
        r.released = true;
        float base = Cfg.f(Cfg.CONV_FINAL_DAMAGE) + Cfg.f(Cfg.CONV_FINAL_DAMAGE_PROF) * c.proficiency();
        Vec3 centre = Targeting.centre(p);
        int hits = 0;
        for (LivingEntity e : Targeting.livingWithin(level, centre, radius + 2, p)) {
            if (!Targeting.hostile(actor, e, KazakiriMod.AIM_CONVERGENCE)) continue;
            double d = Targeting.centre(e).distanceTo(centre);
            double falloff = 1.0 - 0.5 * Math.min(1.0, d / radius);
            if (!AcademySafety.attack(actor, e, KazakiriMod.AIM_CONVERGENCE, (float) (base * falloff))) continue;
            Vec3 away = Targeting.flatDir(centre, e.position(), p.getLookAngle());
            double k = Cfg.CONV_FINAL_KNOCKBACK.get() * falloff * (Targeting.bossLike(e) ? 0.3 : Targeting.controlFactor(e));
            Targeting.setMotion(e, new Vec3(away.x * k, 0.4, away.z * k));
            hits++;
        }
        Vec3 look = p.getLookAngle();
        Fx.make(level, centre, FxType.CONV_FINAL, 34).aux(new Vec3(look.x, 0, look.z)).power((float) radius).range(192).send();
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8f, 0.9f);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 3.0f, 0.6f);
        if (hits > 0) c.addProficiency(0.02f * Math.min(hits, 5));
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.remove(p.getUUID());
        State s = p == null ? null : State.peek(p);
        boolean released = r != null && r.released;
        if (p != null) {
            if (r != null) Fx.cancel(p.serverLevel(), r.fx);
            State.setConvergence(p.getUUID(), null);
            if (s != null) {
                s.convergence = false;
                Flight.release(p, s);
                State.updateAura(p);
            }
            p.fallDistance = 0f;
            if (p.isAlive() && !p.onGround() && !p.isCreative() && !p.isSpectator())
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
            if (reason != StopReason.FINISHED) State.anim(p, Anims.STOP);
        }
        int full = Cfg.ticks(Cfg.CONV_COOLDOWN);
        c.setCooldown(released ? full : full / 2);
    }

    public static void forget(UUID id) { RUNS.remove(id); State.setConvergence(id, null); }
    public static void clearAll() { RUNS.clear(); }
}
