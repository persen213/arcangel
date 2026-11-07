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
import dev.toaru.kazakiri.net.Net;
import dev.toaru.kazakiri.net.ScanMsg;
import io.github.pinchan4273.reacademycraft.api.TargetRelation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraftforge.network.PacketDistributor;

/**
 * AIM Revelation (toggle, 5 s): detects living entities within the radius and reveals them through walls on the caster's
 * screen only, hostile and neutral in different colours (based on the server's team / PvP relation). Not permanent: the
 * marks show where they were at the moment of the scan and fade when it ends. Everyone sees the halo and scanning plane.
 */
public final class AimRevelation implements SkillBehavior {
    private static final String[] DIRECTIONS = {"ahead", "ahead-right", "right", "behind-right", "behind", "behind-left", "left", "ahead-left"};

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Cfg.f(Cfg.REVELATION_CP), Cfg.f(Cfg.REVELATION_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        double radius = Cfg.REVELATION_RADIUS.get();
        int duration = Cfg.ticks(Cfg.REVELATION_SECONDS);
        List<LivingEntity> found = new ArrayList<>();
        List<Boolean> hostile = new ArrayList<>();
        List<LivingEntity> all = new ArrayList<>(Targeting.livingWithin(level, p.position(), radius, p));
        all.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int hostiles = 0;
        for (LivingEntity e : all) {
            if (found.size() >= ScanMsg.MAX) break;
            TargetRelation r = AcademySafety.relation(actor, e);
            if (r == TargetRelation.SELF || r == TargetRelation.ALLY || r == TargetRelation.NONE) continue;
            boolean h = r == TargetRelation.HOSTILE || r == TargetRelation.PLAYER;
            found.add(e);
            hostile.add(h);
            if (h) hostiles++;
        }
        int[] ids = new int[found.size()];
        boolean[] flags = new boolean[found.size()];
        for (int i = 0; i < ids.length; i++) { ids[i] = found.get(i).getId(); flags[i] = hostile.get(i); }
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ScanMsg(duration, ids, flags));
        Fx.make(level, p.position(), FxType.REVELATION, duration).a(p.getId()).aux(new Vec3(radius, 0, 0)).range(Math.max(64, radius + 16)).send();
        State.anim(p, Anims.REVELATION);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.45f);
        StringBuilder text = new StringBuilder("AIM Revelation: " + hostiles + " hostile, " + (found.size() - hostiles) + " neutral");
        for (int i = 0; i < Math.min(2, found.size()); i++) text.append(" | ").append(describe(p, found.get(i)));
        Fx.hint(p, text.toString());
        c.addProficiency(0.004f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        return c.naturalUser() != null && c.age() < Cfg.ticks(Cfg.REVELATION_SECONDS);
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        if (p != null && reason != StopReason.FINISHED) State.anim(p, Anims.STOP);
        c.setCooldown(Cfg.ticks(Cfg.REVELATION_COOLDOWN));
    }

    private static String describe(ServerPlayer p, LivingEntity e) {
        Vec3 look = p.getLookAngle();
        double lx = look.x, lz = look.z, ll = Math.hypot(lx, lz);
        if (ll < 1e-4) { lx = 0; lz = 1; } else { lx /= ll; lz /= ll; }
        Vec3 to = e.position().subtract(p.position());
        double tl = Math.hypot(to.x, to.z), tx = tl < 1e-4 ? lx : to.x / tl, tz = tl < 1e-4 ? lz : to.z / tl;
        double angle = Math.toDegrees(Math.atan2(lx * tz - lz * tx, lx * tx + lz * tz));
        int index = ((int) Math.round(angle / 45.0) % 8 + 8) % 8;
        return (int) Math.round(to.length()) + "m " + DIRECTIONS[index];
    }
}
