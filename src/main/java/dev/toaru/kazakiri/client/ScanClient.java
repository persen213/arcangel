package dev.toaru.kazakiri.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Entities revealed by this player's own AIM Revelation (entity id -> mark). */
public final class ScanClient {
    private ScanClient() { }

    static final class Mark {
        final long end;
        final boolean hostile;
        Mark(long end, boolean hostile) { this.end = end; this.hostile = hostile; }
    }

    static final Map<Integer, Mark> MARKED = new HashMap<>();
    static long markStart;

    static void set(int duration, int[] ids, boolean[] hostile) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        markStart = now;
        MARKED.clear();
        for (int i = 0; i < Math.min(ids.length, hostile.length); i++) MARKED.put(ids[i], new Mark(now + duration, hostile[i]));
    }

    static void clear() { MARKED.clear(); }

    static void tick() {
        if (MARKED.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { MARKED.clear(); return; }
        long now = mc.level.getGameTime();
        MARKED.values().removeIf(m -> now >= m.end);
    }
}
