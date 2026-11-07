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
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Celestial Spear: 10 ticks of charging (the spear assembles above the hand), then a fast straight spear (server data,
 * not an arrow or entity) that pierces up to the configured number of enemies, each hit once. Stops at walls and at
 * maximum range. Bosses take reduced damage. Stronger during Archangel Protocol.
 */
public final class CelestialSpear implements SkillBehavior {
    static final int CHARGE = 10;

    private static final class Shot {
        Vec3 pos, dir;
        double remaining;
        float damage;
        long fx;
        final Set<UUID> hit = new HashSet<>();
    }

    private static final Map<UUID, Shot> FLIGHTS = new HashMap<>();

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.SPEAR_CP), Cfg.f(Cfg.SPEAR_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        Fx.make(p.serverLevel(), p.position(), FxType.SPEAR_CHARGE, CHARGE + 2).a(p.getId()).range(96).send();
        State.anim(p, Anims.SPEAR);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.8f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return false;
        int age = c.age();
        if (age < CHARGE) return true;
        ServerLevel level = p.serverLevel();
        double speed = Cfg.SPEAR_SPEED.get();
        if (age == CHARGE) {
            Shot f = new Shot();
            f.dir = p.getLookAngle();
            f.pos = p.getEyePosition().add(f.dir.scale(0.8));
            f.remaining = Cfg.SPEAR_RANGE.get();
            f.damage = (float) ((Cfg.SPEAR_DAMAGE.get() + Cfg.SPEAR_DAMAGE_PROF.get() * c.proficiency()) * (State.protocol(p) ? Cfg.PROTOCOL_SPEAR.get() : 1.0));
            f.fx = Fx.make(level, f.pos, FxType.SPEAR, (int) Math.ceil(f.remaining / speed) + 3).aux(f.dir.scale(speed)).power((float) f.remaining).send();
            FLIGHTS.put(p.getUUID(), f);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.4f, 1.3f);
        }
        Shot f = FLIGHTS.get(p.getUUID());
        if (f == null) return false;
        var actor = c.actor();
        Vec3 next = f.pos.add(f.dir.scale(speed));
        BlockHitResult wall = level.clip(new ClipContext(f.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? next : wall.getLocation();
        int maxTargets = Cfg.SPEAR_PIERCE.get().intValue();
        for (LivingEntity e : Targeting.livingWithin(level, end, speed + 1.5, p)) {
            if (f.hit.contains(e.getUUID()) || !Targeting.hostile(actor, e, KazakiriMod.CELESTIAL_SPEAR)) continue;
            var box = e.getBoundingBox().inflate(0.35);
            if (box.clip(f.pos, end).isEmpty() && !box.contains(end)) continue;
            f.hit.add(e.getUUID());
            float dmg = f.damage * (Targeting.bossLike(e) ? Cfg.f(Cfg.SPEAR_BOSS) : 1f);
            if (AcademySafety.attack(actor, e, KazakiriMod.CELESTIAL_SPEAR, dmg)) {
                Targeting.setMotion(e, e.getDeltaMovement().add(f.dir.scale(0.35 * Targeting.controlFactor(e))));
                c.addProficiency(0.01f);
            }
            Fx.make(level, Targeting.centre(e), FxType.SPEAR_HIT, 14).aux(f.dir).send();
            if (f.hit.size() >= maxTargets) { end(level, f, Targeting.centre(e)); return false; }
        }
        if (wall.getType() != HitResult.Type.MISS) { end(level, f, end); return false; }
        f.pos = next;
        f.remaining -= speed;
        if (f.remaining <= 0) { end(level, f, f.pos); return false; }
        return true;
    }

    private static void end(ServerLevel level, Shot f, Vec3 at) {
        Fx.cancel(level, f.fx);
        f.fx = 0;
        Fx.make(level, at, FxType.SPEAR_HIT, 14).aux(f.dir).send();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        Shot f = p == null ? null : FLIGHTS.remove(p.getUUID());
        if (p != null && f != null) Fx.cancel(p.serverLevel(), f.fx);
        c.setCooldown(Cfg.ticks(Cfg.SPEAR_COOLDOWN));
    }

    public static void forget(UUID id) { FLIGHTS.remove(id); }
    public static void clearAll() { FLIGHTS.clear(); }
}
