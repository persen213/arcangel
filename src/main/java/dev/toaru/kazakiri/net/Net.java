package dev.toaru.kazakiri.net;

import dev.toaru.kazakiri.KazakiriMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** The add-on's network channel. Server and clients must run the same protocol version. */
public final class Net {
    private Net() { }

    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KazakiriMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, FxStartMsg.class, FxStartMsg::encode, FxStartMsg::decode, FxStartMsg::handle);
        CHANNEL.registerMessage(id++, FxCancelMsg.class, FxCancelMsg::encode, FxCancelMsg::decode, FxCancelMsg::handle);
        CHANNEL.registerMessage(id++, AuraMsg.class, AuraMsg::encode, AuraMsg::decode, AuraMsg::handle);
        CHANNEL.registerMessage(id++, ScanMsg.class, ScanMsg::encode, ScanMsg::decode, ScanMsg::handle);
        CHANNEL.registerMessage(id++, AnimMsg.class, AnimMsg::encode, AnimMsg::decode, AnimMsg::handle);
    }
}
