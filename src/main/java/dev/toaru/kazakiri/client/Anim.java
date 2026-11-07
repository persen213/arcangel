package dev.toaru.kazakiri.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.toaru.kazakiri.Anims;
import dev.toaru.kazakiri.KazakiriMod;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Body poses through the PlayerAnimator library. Each pose is a keyframe file in assets/kazakiri/player_animation/.
 * One animation layer per player; new poses fade in, "stop" fades out. Any problem with a pose only skips that pose
 * (logged once); it never crashes the game.
 */
final class Anim {
    private Anim() { }

    private static final Map<AbstractClientPlayer, ModifierLayer<IAnimation>> LAYERS = new WeakHashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    static void play(int entityId, int anim) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(entityId) instanceof AbstractClientPlayer player)) return;
        try {
            ModifierLayer<IAnimation> layer = LAYERS.get(player);
            if (layer == null) {
                layer = new ModifierLayer<>();
                PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(1000, layer);
                LAYERS.put(player, layer);
            }
            String name = Anims.name(anim);
            if (name == null) {
                layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(6, Ease.INOUTSINE), null);
                return;
            }
            KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(new ResourceLocation(KazakiriMod.MOD_ID, name));
            if (data == null) {
                if (WARNED.add(name)) KazakiriMod.LOGGER.warn("Kazakiri pose '{}' was not found in player_animation", name);
                return;
            }
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE), new KeyframeAnimationPlayer(data));
        } catch (Throwable t) {
            if (WARNED.add("error")) KazakiriMod.LOGGER.error("Kazakiri could not play a pose", t);
        }
    }

    static void clear() { LAYERS.clear(); }
}
