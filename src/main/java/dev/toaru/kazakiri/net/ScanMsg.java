package dev.toaru.kazakiri.net;

import dev.toaru.kazakiri.client.ClientNet;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to the revealing player only: detected entities, whether each is hostile, and how long to show them. */
public final class ScanMsg {
    public static final int MAX = 32;
    public final int duration;
    public final int[] ids;
    public final boolean[] hostile;

    public ScanMsg(int duration, int[] ids, boolean[] hostile) { this.duration = duration; this.ids = ids; this.hostile = hostile; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(duration);
        int n = Math.min(Math.min(ids.length, hostile.length), MAX);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) { buf.writeVarInt(ids[i]); buf.writeBoolean(hostile[i]); }
    }

    public static ScanMsg decode(FriendlyByteBuf buf) {
        int duration = buf.readVarInt();
        int n = Math.max(0, Math.min(buf.readVarInt(), MAX));
        int[] ids = new int[n];
        boolean[] hostile = new boolean[n];
        for (int i = 0; i < n; i++) { ids[i] = buf.readVarInt(); hostile[i] = buf.readBoolean(); }
        return new ScanMsg(duration, ids, hostile);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNet.scan(this.duration, this.ids, this.hostile)));
        c.setPacketHandled(true);
    }
}
