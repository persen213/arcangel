package dev.toaru.kazakiri.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** An entity's interpolated position and body directions for this frame (where wings, halos and constructs attach). */
final class Body {
    final Vec3 feet, fwd, right, up;
    final double height;
    final boolean crouching;

    private Body(Vec3 feet, Vec3 fwd, Vec3 right, double height, boolean crouching) {
        this.feet = feet; this.fwd = fwd; this.right = right; this.up = new Vec3(0, 1, 0);
        this.height = height; this.crouching = crouching;
    }

    static Body of(Entity e, float partial) {
        float yaw = e instanceof LivingEntity le ? Mth.rotLerp(partial, le.yBodyRotO, le.yBodyRot) : e.getYRot();
        double rad = Math.toRadians(yaw);
        Vec3 fwd = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        return new Body(e.getPosition(partial), fwd, right, e.getBbHeight(), e.isCrouching());
    }

    /** A body frame at an arbitrary place (used for the giant Convergence projection). */
    static Body at(Vec3 feet, Vec3 fwd, double height) {
        Vec3 f = new Vec3(fwd.x, 0, fwd.z);
        f = f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
        return new Body(feet, f, new Vec3(-f.z, 0, f.x), height, false);
    }

    /** Where the right hand is with the arm raised (spear, array). */
    Vec3 raisedHand() { return feet.add(0, height * 1.12, 0).add(right.scale(0.38)).add(fwd.scale(0.1)); }

    /** Upper back, where the wings are rooted. */
    Vec3 back() { return feet.add(0, height * (crouching ? 0.62 : 0.74), 0).subtract(fwd.scale(crouching ? 0.05 : 0.17)); }

    Vec3 chest() { return feet.add(0, height * 0.62, 0); }

    Vec3 aboveHead() { return feet.add(0, height + 0.32, 0); }
}
