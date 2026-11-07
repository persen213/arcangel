package dev.toaru.kazakiri.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types for the add-on's geometry. Uses RenderType.create (public in Forge 1.20.1) exactly like the Supernova
 * add-on that compiled. GLOW is the vanilla additive lightning type.
 */
public final class KzRenderTypes extends RenderType {
    private KzRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                          Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
        throw new IllegalStateException("not meant to be constructed");
    }

    /** Translucent, normally blended, both sides: feathers, crystals, the barrier dome. */
    public static final RenderType CRYSTAL = create("kazakiri_crystal", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /** Drawn on top of walls: Diffusion Scan markers (only on the scanning player's screen). */
    public static final RenderType XRAY = create("kazakiri_xray", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /** Additive glow: rings, halos, edges, beams. */
    public static final RenderType GLOW = RenderType.lightning();
}
