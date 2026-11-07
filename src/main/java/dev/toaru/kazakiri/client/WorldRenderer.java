package dev.toaru.kazakiri.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.toaru.kazakiri.KazakiriMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * One render hook for everything the add-on draws in the world. It uses its own buffers (one per render type) so the
 * translucent, glowing and through-walls geometry can be built at the same time, then draws them in that order.
 */
@Mod.EventBusSubscriber(modid = KazakiriMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WorldRenderer {
    private WorldRenderer() { }

    private static MultiBufferSource.BufferSource buffers;

    private static MultiBufferSource.BufferSource buffers() {
        if (buffers == null) {
            Map<RenderType, BufferBuilder> fixed = new HashMap<>();
            fixed.put(KzRenderTypes.CRYSTAL, new BufferBuilder(1 << 16));
            fixed.put(KzRenderTypes.GLOW, new BufferBuilder(1 << 16));
            fixed.put(KzRenderTypes.XRAY, new BufferBuilder(1 << 13));
            buffers = MultiBufferSource.immediateWithBuffers(fixed, new BufferBuilder(256));
        }
        return buffers;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ClientFx.ACTIVE.isEmpty() && AuraClient.AURAS.isEmpty() && ScanClient.MARKED.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = mc.isPaused() ? 0f : event.getPartialTick();
        long now = mc.level.getGameTime();
        double time = now + partial;
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f pose = event.getPoseStack().last().pose();
        MultiBufferSource.BufferSource src = buffers();

        Geo crystal = new Geo(src.getBuffer(KzRenderTypes.CRYSTAL), pose, cam);
        Geo glow = new Geo(src.getBuffer(KzRenderTypes.GLOW), pose, cam);
        Geo xray = new Geo(src.getBuffer(KzRenderTypes.XRAY), pose, cam);

        AuraRenderer.draw(crystal, glow, cam, partial, time);
        FxRenderer.draw(crystal, glow, cam, now, partial, time);
        ScanRenderer.draw(xray, cam, now, partial, time);

        src.endBatch(KzRenderTypes.CRYSTAL);
        src.endBatch(KzRenderTypes.GLOW);
        src.endBatch(KzRenderTypes.XRAY);
    }
}
