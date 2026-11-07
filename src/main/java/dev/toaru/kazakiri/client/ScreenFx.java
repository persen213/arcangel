package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.ClientCfg;
import dev.toaru.kazakiri.KazakiriMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Screen overlay: a short white flash for players near an AIM Convergence at the moment of release (client option). */
@Mod.EventBusSubscriber(modid = KazakiriMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ScreenFx {
    private ScreenFx() { }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("kazakiri_flash", (gui, graphics, partialTick, width, height) -> render(graphics, width, height, partialTick));
    }

    private static void render(GuiGraphics g, int w, int h, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !ClientCfg.SCREEN_FLASH.get()) return;
        double age = ConvergenceFx.nearbyAge(partialTick, 48);
        if (age < ConvergenceFx.RELEASE || age >= ConvergenceFx.RELEASE + 14) return;
        double t = (age - ConvergenceFx.RELEASE) / 14.0;
        int alpha = (int) (200 * (1 - t) * (1 - t));
        if (alpha <= 0) return;
        g.fill(0, 0, w, h, (alpha << 24) | 0xF4FBFF);
    }
}
