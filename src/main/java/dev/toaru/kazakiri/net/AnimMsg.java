package dev.toaru.kazakiri.net;

import dev.toaru.kazakiri.client.ClientNet;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to clients: play a body pose (Anims id) on a player. */
public final class AnimMsg {
    public final int entityId, anim;

    public AnimMsg(int entityId, int anim) { this.entityId = entityId; this.anim = anim; }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(entityId); buf.writeVarInt(anim); }

    public static AnimMsg decode(FriendlyByteBuf buf) { return new AnimMsg(buf.readVarInt(), buf.readVarInt()); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNet.anim(this.entityId, this.anim)));
        c.setPacketHandled(true);
    }
}
