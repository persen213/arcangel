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
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Heaven's Rejection: 8 ticks of wind-up (wings spread), then a curved wave across a forward arc. Enemies are pushed away
 * (harder when closer), take low-moderate damage, and have their current action interrupted (bows, shields, eating:
 * stopUsingItem). Enemy projectiles in the arc are removed. Bosses are pushed much less. No blocks are changed.
 */
public final class HeavensRejection implements SkillBehavior {
    static final int WINDUP = 8;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.REJECTION_CP), Cfg.f(Cfg.REJECTION_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        State.anim(p, Anims.REJECTION);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.4f, 0.9f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return false;
        if (c.age() < WINDUP) return true;
        release(c, p);
        return false;
    }

    private static void release(ActivationContext c, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        double range = Cfg.REJECTION_RANGE.get();
        double minDot = Math.cos(Math.toRadians(Cfg.REJECTION_ARC.get() / 2.0));
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 chest = Targeting.centre(p);
        float damage = Cfg.f(Cfg.REJECTION_DAMAGE) + Cfg.f(Cfg.REJECTION_DAMAGE_PROF) * c.proficiency();
        int hits = 0;
        for (LivingEntity e : Targeting.livingWithin(level, chest, range, p)) {
            Vec3 to = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
            double d = to.length();
            if (d > 1e-3 && to.scale(1 / d).dot(flat) < minDot) continue;
            if (!Targeting.hostile(actor, e, KazakiriMod.HEAVENS_REJECTION)) continue;
            double closeness = 1.0 - Math.min(1.0, d / range);
            if (!AcademySafety.attack(actor, e, KazakiriMod.HEAVENS_REJECTION, (float) (damage * (0.6 + 0.4 * closeness)))) continue;
            Vec3 away = d > 1e-3 ? to.scale(1 / d) : flat;
            double k = Cfg.REJECTION_KNOCKBACK.get() * (0.4 + 0.6 * closeness) * Targeting.controlFactor(e);
            Targeting.setMotion(e, new Vec3(away.x * k, 0.3 + 0.1 * k, away.z * k));
            e.stopUsingItem();
            hits++;
        }
        for (Projectile pr : Targeting.projectilesWithin(level, chest, range, p)) {
            Vec3 to = new Vec3(pr.getX() - p.getX(), 0, pr.getZ() - p.getZ());
            if (to.lengthSqr() < 1e-6 || to.normalize().dot(flat) >= minDot) pr.discard();
        }
        Fx.make(level, chest, FxType.REJECTION, 18).aux(flat).power((float) range).range(96).send();
        level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.7f);
        if (hits > 0) c.addProficiency(0.01f * Math.min(hits, 4)); else c.addProficiency(0.002f);
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        c.setCooldown(Cfg.ticks(Cfg.REJECTION_COOLDOWN));
    }
}
