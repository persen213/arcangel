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
import io.github.pinchan4273.reacademycraft.api.TargetRelation;

/**
 * Sanctuary (toggle, 5 s): wings close around the caster, or around the ally under the crosshair (within range). The shell
 * absorbs incoming damage until its durability is used up; a single hit at or above the break threshold shatters it.
 * Never invulnerability. The damage handling is in ServerEvents (the shell is stored on the protected entity).
 */
public final class Sanctuary implements SkillBehavior {
    private static final Map<UUID, UUID> PROTECTED = new HashMap<>(); // caster -> protected entity

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        LivingEntity target = p;
        LivingEntity aimed = Targeting.living(p, Cfg.SANCTUARY_RANGE.get());
        if (aimed != null && AcademySafety.relation(c.actor(), aimed) == TargetRelation.ALLY) target = aimed;
        if (State.shield(target.getUUID()) != null) { Fx.hint(p, "That target is already protected"); return ActivationOutcome.REFUSED_INVALID; }
        if (!c.pay(Cfg.f(Cfg.SANCTUARY_CP), Cfg.f(Cfg.SANCTUARY_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        ServerLevel level = p.serverLevel();
        State.Shield sh = new State.Shield();
        sh.caster = p.getUUID();
        sh.hp = (float) (Cfg.SANCTUARY_DURABILITY.get() * (State.protocol(p) ? Cfg.PROTOCOL_SANCTUARY.get() : 1.0));
        sh.fx = Fx.make(level, target.position(), FxType.SANCTUARY, Cfg.ticks(Cfg.SANCTUARY_SECONDS) + 2).a(target.getId()).range(96).send();
        State.putShield(target.getUUID(), sh);
        PROTECTED.put(p.getUUID(), target.getUUID());
        State.anim(p, Anims.SANCTUARY);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        UUID target = p == null ? null : PROTECTED.get(p.getUUID());
        if (p == null || target == null || c.age() >= Cfg.ticks(Cfg.SANCTUARY_SECONDS)) return false;
        State.Shield sh = State.shield(target);
        if (sh == null || sh.hp <= 0f) return false;
        var e = p.serverLevel().getEntity(target);
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return false;
        if (c.age() % 20 == 0) c.addProficiency(0.003f);
        return true;
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        UUID target = p == null ? null : PROTECTED.remove(p.getUUID());
        if (p != null && target != null) {
            ServerLevel level = p.serverLevel();
            State.Shield sh = State.shield(target);
            State.removeShield(target);
            if (sh != null) Fx.cancel(level, sh.fx);
            var e = level.getEntity(target);
            Vec3 at = e != null ? Targeting.centre(e) : p.position();
            Fx.make(level, at, FxType.SANCTUARY_END, 16).range(96).send();
            boolean broken = sh != null && sh.hp <= 0f;
            level.playSound(null, at.x, at.y, at.z, broken ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, broken ? 0.5f : 1.6f);
        }
        c.setCooldown(Cfg.ticks(Cfg.SANCTUARY_COOLDOWN));
    }

    public static void forget(UUID caster) {
        UUID t = PROTECTED.remove(caster);
        if (t != null) State.removeShield(t);
    }
    public static void clearAll() { PROTECTED.clear(); }
}
