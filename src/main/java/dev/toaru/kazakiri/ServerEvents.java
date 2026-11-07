package dev.toaru.kazakiri;

import dev.toaru.kazakiri.net.Fx;
import dev.toaru.kazakiri.net.FxType;
import dev.toaru.kazakiri.skills.AimConvergence;
import dev.toaru.kazakiri.skills.AngelsDescent;
import dev.toaru.kazakiri.skills.ArchangelProtocol;
import dev.toaru.kazakiri.skills.CelestialSpear;
import dev.toaru.kazakiri.skills.Sanctuary;
import dev.toaru.kazakiri.skills.SeraphicArray;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side Forge events: Sanctuary shells, AIM Convergence damage reduction (caster and allies in the field), aura sync
 * to players who come into view, and clean-up. Re:AcademyCraft stops running skills on death, logout and dimension change
 * (each skill's stop() cleans up); this class removes everything else the add-on keeps.
 */
@Mod.EventBusSubscriber(modid = KazakiriMod.MOD_ID)
public final class ServerEvents {
    private ServerEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float amount = event.getAmount();
        float original = amount;

        State.Shield sh = State.shield(victim.getUUID());
        if (sh != null && sh.hp > 0f) {
            float absorbed = Math.min(amount, sh.hp);
            sh.hp -= absorbed;
            amount -= absorbed;
            if (original >= Cfg.f(Cfg.SANCTUARY_BREAK)) sh.hp = 0f;
            ServerLevel level = (ServerLevel) victim.level();
            long now = level.getGameTime();
            if (now - sh.lastFlash >= 3) {
                sh.lastFlash = now;
                Entity src = event.getSource().getEntity();
                Vec3 from = src != null ? src.position() : event.getSource().getSourcePosition();
                Vec3 dir = from == null ? Vec3.ZERO : from.subtract(victim.position());
                Fx.make(level, victim.position(), FxType.SANCTUARY_HIT, 12).a(victim.getId()).aux(dir).range(96).send();
            }
        }

        if (victim instanceof ServerPlayer p) {
            State s = State.peek(p);
            if (s != null && s.convergence) amount *= 1f - Cfg.f(Cfg.CONV_SELF_REDUCTION);
        }
        for (Map.Entry<UUID, Vec3> field : State.convergenceFields().entrySet()) {
            if (field.getKey().equals(victim.getUUID())) continue;
            Player caster = victim.level().getPlayerByUUID(field.getKey());
            if (caster == null || !victim.isAlliedTo(caster)) continue;
            double r = Cfg.CONV_RADIUS.get();
            if (victim.position().distanceToSqr(field.getValue()) > r * r) continue;
            amount *= 1f - Cfg.f(Cfg.CONV_ALLY_REDUCTION);
            break;
        }
        if (amount != original) event.setAmount(Math.max(0f, amount));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer)
            State.sendAuraTo(target, viewer);
    }

    /** Flight granted by a skill when the server stopped is removed on the next login. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!p.getPersistentData().getBoolean(Flight.TAG)) return;
        p.getPersistentData().remove(Flight.TAG);
        if (p.isCreative() || p.isSpectator()) return;
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        if (event.getEntity() instanceof ServerPlayer p) {
            State s = State.peek(p);
            if (s != null) Flight.revoke(p, s);
        }
        AngelsDescent.forget(id);
        CelestialSpear.forget(id);
        Sanctuary.forget(id);
        SeraphicArray.forget(id);
        ArchangelProtocol.forget(id);
        AimConvergence.forget(id);
        State.removeShield(id);
        State.forget(id);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        AngelsDescent.clearAll();
        CelestialSpear.clearAll();
        Sanctuary.clearAll();
        SeraphicArray.clearAll();
        ArchangelProtocol.clearAll();
        AimConvergence.clearAll();
        State.clearAll();
    }
}
