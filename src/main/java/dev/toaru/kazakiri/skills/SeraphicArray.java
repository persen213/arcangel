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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seraphic Array (toggle, 5 s): wing-shaped constructs (4, or 5 during Archangel Protocol) form behind the caster. One by
 * one, every 15 ticks, a construct turns to a different valid enemy in range and strikes it once (moderate damage), then
 * returns. Each construct strikes at most once per cast; the skill ends when all have struck.
 */
public final class SeraphicArray implements SkillBehavior {
    static final int FIRST = 14, INTERVAL = 15;

    private static final class Run {
        int count, fired, lastStrike = -100;
        long fx;
        final Set<UUID> used = new HashSet<>();
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.ARRAY_CP), Cfg.f(Cfg.ARRAY_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        Run r = new Run();
        r.count = Math.min(5, Cfg.ARRAY_CONSTRUCTS.get().intValue() + (State.protocol(p) ? 1 : 0));
        r.fx = Fx.make(p.serverLevel(), p.position(), FxType.ARRAY, Cfg.ticks(Cfg.ARRAY_SECONDS) + 2).a(p.getId()).c(r.count).range(128).send();
        RUNS.put(p.getUUID(), r);
        State.anim(p, Anims.ARRAY);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.4f, 1.4f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.get(p.getUUID());
        int age = c.age();
        if (p == null || r == null || age >= Cfg.ticks(Cfg.ARRAY_SECONDS)) return false;
        if (r.fired >= r.count) return age < r.lastStrike + 16;
        if (age >= FIRST && (age - FIRST) % INTERVAL == 0) {
            LivingEntity target = pick(c, p, r);
            if (target != null) {
                strike(c, p, target, r.fired, Cfg.f(Cfg.ARRAY_DAMAGE) + Cfg.f(Cfg.ARRAY_DAMAGE_PROF) * c.proficiency(), KazakiriMod.SERAPHIC_ARRAY);
                r.used.add(target.getUUID());
                r.fired++;
                r.lastStrike = age;
            }
        }
        return true;
    }

    private static LivingEntity pick(ActivationContext c, ServerPlayer p, Run r) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : Targeting.livingWithin(p.serverLevel(), p.position(), Cfg.ARRAY_RANGE.get(), p))
            if (Targeting.hostile(c.actor(), e, KazakiriMod.SERAPHIC_ARRAY)) list.add(e);
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        for (LivingEntity e : list) if (!r.used.contains(e.getUUID())) return e;
        return list.get(0);
    }

    /** One construct strike (also used by AIM Convergence): damage through the safety service and the strike visual. */
    static void strike(ActivationContext c, ServerPlayer p, LivingEntity target, int index, float damage, io.github.pinchan4273.reacademycraft.api.AbilityId skill) {
        ServerLevel level = p.serverLevel();
        Fx.make(level, Targeting.centre(target), FxType.ARRAY_STRIKE, 18).a(p.getId()).b(target.getId()).c(index).range(128).send();
        if (AcademySafety.attack(c.actor(), target, skill, damage)) {
            Vec3 away = Targeting.flatDir(p.position(), target.position(), p.getLookAngle());
            double k = 0.4 * Targeting.controlFactor(target);
            Targeting.setMotion(target, target.getDeltaMovement().add(away.x * k, 0.15, away.z * k));
            c.addProficiency(0.008f);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        Run r = p == null ? null : RUNS.remove(p.getUUID());
        if (p != null && r != null) Fx.cancel(p.serverLevel(), r.fx);
        if (p != null && reason != StopReason.FINISHED) State.anim(p, Anims.STOP);
        c.setCooldown(Cfg.ticks(Cfg.ARRAY_COOLDOWN));
    }

    public static void forget(UUID id) { RUNS.remove(id); }
    public static void clearAll() { RUNS.clear(); }

}
