package dev.toaru.kazakiri.client;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** AIM Revelation marks, drawn through walls on the revealing player's own screen. Hostile: rose; neutral: pale cyan. */
final class ScanRenderer {
    private ScanRenderer() { }

    static void draw(Geo xray, Vec3 cam, long now, float partial, double time) {
        if (ScanClient.MARKED.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Map.Entry<Integer, ScanClient.Mark> m : ScanClient.MARKED.entrySet()) {
            Entity e = mc.level.getEntity(m.getKey());
            if (e == null || !e.isAlive()) continue;
            Body b = Body.of(e, partial);
            double left = m.getValue().end - now - partial;
            float a = (float) Math.min(FxRenderer.smooth((now + partial - ScanClient.markStart) / 6.0), FxRenderer.smooth(left / 20.0));
            if (a <= 0.01f) continue;
            boolean hostile = m.getValue().hostile;
            float r = hostile ? 1f : 0.6f, g = hostile ? 0.5f : 0.95f, bl = hostile ? 0.6f : 1f;
            Vec3 toCam = cam.subtract(b.chest());
            Vec3 n = toCam.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : toCam.normalize();
            // Luminous outline: hexagonal frames around the body plus a marker above the head.
            xray.color(r, g, bl, 0.55f * a).hexRing(b.chest(), n, b.height * 0.55 + 0.15, 0.025, time * 0.03);
            xray.color(r, g, bl, 0.3f * a).hexRing(b.feet.add(0, b.height * 0.25, 0), n, b.height * 0.35, 0.015, -time * 0.03);
            Vec3 mark = b.aboveHead().add(0, 0.35, 0);
            xray.color(r, g, bl, 0.8f * a).hexRing(mark, n, 0.24, 0.03, time * 0.06);
            xray.color(1f, 1f, 1f, 0.4f * a).hexagon(mark, n, 0.09, time * 0.06);
        }
    }
}
