package dev.toaru.kazakiri.net;

import dev.toaru.kazakiri.client.ClientNet;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: which aura parts (wings, halo, archangel) a player currently shows. */
public final class AuraMsg {
    public final int entityId, flags;

    public AuraMsg(int entityId, int flags) { this.entityId = entityId; this.flags = flags; }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(entityId); buf.writeVarInt(flags); }

    public static AuraMsg decode(FriendlyByteBuf buf) { return new AuraMsg(buf.readVarInt(), buf.readVarInt()); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNet.aura(this.entityId, this.flags)));
        c.setPacketHandled(true);
    }
}
