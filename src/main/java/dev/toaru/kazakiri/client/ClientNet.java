package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.net.FxStartMsg;

/** Client-side entry points for network messages. Only ever loaded on a client (reached through DistExecutor). */
public final class ClientNet {
    private ClientNet() { }

    public static void start(FxStartMsg msg) { ClientFx.start(msg); }
    public static void cancel(long key) { ClientFx.cancel(key); }
    public static void aura(int entityId, int flags) { AuraClient.set(entityId, flags); }
    public static void scan(int duration, int[] ids, boolean[] hostile) { ScanClient.set(duration, ids, hostile); }
    public static void anim(int entityId, int anim) { Anim.play(entityId, anim); }
}
