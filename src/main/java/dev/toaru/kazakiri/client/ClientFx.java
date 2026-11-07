package dev.toaru.kazakiri.client;

import dev.toaru.kazakiri.net.FxStartMsg;
import dev.toaru.kazakiri.net.FxType;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Running visual effects on this client: plain data, drawn by WorldRenderer, removed when their time is up (capped). */
public final class ClientFx {
    private ClientFx() { }

    private static final int MAX = 96;
    static final Map<Long, Effect> ACTIVE = new LinkedHashMap<>();

    public static final class Effect {
        public final long key;
        public final FxType type;
        public final long start;
        public final int duration;
        public final Vec3 pos, aux;
        public final int a, b, c;
        public final float power;

        Effect(FxStartMsg m, long start) {
            this.key = m.key; this.type = m.type; this.start = start; this.duration = m.duration;
            this.pos = new Vec3(m.x, m.y, m.z); this.aux = new Vec3(m.ax, m.ay, m.az);
            this.a = m.a; this.b = m.b; this.c = m.c; this.power = m.power;
        }

        public double age(long now, float partial) { return (now - start) + partial; }
    }

    static void start(FxStartMsg msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ACTIVE.put(msg.key, new Effect(msg, mc.level.getGameTime()));
        if (ACTIVE.size() > MAX) {
            Iterator<Long> it = ACTIVE.keySet().iterator();
            it.next();
            it.remove();
        }
    }

    static void cancel(long key) { ACTIVE.remove(key); }

    static void clear() { ACTIVE.clear(); }

    static void tick() {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        long now = mc.level.getGameTime();
        ACTIVE.values().removeIf(e -> now - e.start > e.duration + 2 || now < e.start - 40);
    }
}
