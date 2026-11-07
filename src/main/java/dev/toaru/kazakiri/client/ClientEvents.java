package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.ClientCfg;
import dev.toaru.kazakiri.KazakiriMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client upkeep and the Archangel camera. The camera is only changed for the player who cast the ability, only while
 * its effect exists (it ends with the skill, on death, logout, dimension change or cancellation), only with the client
 * option on, and only as offsets for this frame: nothing about the player's real view settings is changed.
 * AIM Convergence: zoom, low angle, a slow sideways drift, a release shake. Archangel Protocol: a small push-in.
 */
@Mod.EventBusSubscriber(modid = KazakiriMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientFx.tick();
        AuraClient.tick();
        ScanClient.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFx.clear();
        AuraClient.clear();
        ScanClient.clear();
        Anim.clear();
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getPlayer() != mc.player || !ClientCfg.CAMERA_EFFECTS.get()) return;
        float partial = mc.getFrameTime();
        double age = ConvergenceFx.ownAge(partial);
        if (age >= 0) {
            event.setNewFovModifier(event.getNewFovModifier() * (float) fov(age));
            return;
        }
        double p = ConvergenceFx.ownProtocolAge(partial);
        if (p >= 0) event.setNewFovModifier(event.getNewFovModifier() * (float) protocolFov(p));
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientCfg.CAMERA_EFFECTS.get()) return;
        double age = ConvergenceFx.ownAge((float) event.getPartialTick());
        if (age < 0) return;
        double hold = env(age, ConvergenceFx.AWAKEN, ConvergenceFx.RAISE, 30, 30);
        // A slow sideways drift so the projection behind comes into view, returning by the end.
        double orbit = 25.0 * Math.min(smooth((age - ConvergenceFx.AWAKEN) / 80.0), 1 - smooth((age - ConvergenceFx.RAISE) / 40.0));
        float pitch = (float) (-6.0 * hold);
        float roll = (float) (1.5 * Math.sin(age * 0.06) * hold);
        float yaw = (float) orbit;
        double rel = ConvergenceFx.RELEASE;
        if (age >= rel && age < rel + 18) {
            double amp = 2.6 * (1 - (age - rel) / 18.0);
            yaw += (float) (amp * Math.sin(age * 2.3));
            pitch += (float) (amp * 0.7 * Math.cos(age * 3.1));
            roll += (float) (amp * 0.5 * Math.sin(age * 1.7));
        }
        event.setPitch(event.getPitch() + pitch);
        event.setYaw(event.getYaw() + yaw);
        event.setRoll(event.getRoll() + roll);
    }

    /** Zoom over the Convergence timeline: push-in, tighter before the release, a punch out at the release, back to normal. */
    private static double fov(double age) {
        if (age < 40) return lerp(1.0, 0.85, smooth(age / 40.0));
        if (age < 120) return 0.85 + 0.01 * Math.sin(age * 0.08);
        if (age < 160) return lerp(0.85, 0.80, smooth((age - 120) / 40.0));
        if (age < 170) return lerp(0.80, 1.12, smooth((age - 160) / 10.0));
        if (age < 200) return lerp(1.12, 1.0, smooth((age - 170) / 30.0));
        return 1.0;
    }

    /** Archangel Protocol: a small push-in during the transformation, eased back out. */
    private static double protocolFov(double age) {
        if (age < 40) return lerp(1.0, 0.92, smooth(age / 40.0));
        return lerp(0.92, 1.0, smooth((age - 40) / 20.0));
    }

    /** 0 outside [start, end + out], easing in over {@code in} ticks after start and out over {@code out} ticks after end. */
    private static double env(double age, double start, double end, double in, double out) {
        if (age < start || age > end + out) return 0;
        double a = smooth((age - start) / in);
        double b = age > end ? 1 - smooth((age - end) / out) : 1;
        return Math.min(a, b);
    }

    private static double smooth(double t) { t = t < 0 ? 0 : (t > 1 ? 1 : t); return t * t * (3 - 2 * t); }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
