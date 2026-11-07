package dev.toaru.kazakiri;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

/**
 * Every balance number (config/kazakiri-common.toml). Seconds are real seconds, ranges are blocks.
 * Only read while the game runs, never during loading.
 */
public final class Cfg {
    private Cfg() { }
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    private static DoubleValue d(String name, double def, double min, double max) { return B.defineInRange(name, def, min, max); }

    // 1 Angel's Descent
    public static final DoubleValue DESCENT_CP = d("descent_cp", 500, 0, 100000);
    public static final DoubleValue DESCENT_OVERLOAD = d("descent_overload", 40, 0, 10000);
    public static final DoubleValue DESCENT_COOLDOWN = d("descent_cooldown_seconds", 14, 0, 3600);
    public static final DoubleValue DESCENT_SECONDS = d("descent_max_seconds", 7, 1, 30);
    public static final DoubleValue DESCENT_LIFT = d("descent_launch_lift", 1.0, 0.2, 3);
    public static final DoubleValue DESCENT_GLIDE_FALL = d("descent_glide_max_fall_speed", 0.12, 0.01, 1);
    public static final DoubleValue DESCENT_ACCEL = d("descent_sprint_acceleration", 0.06, 0, 0.5);
    public static final DoubleValue DESCENT_MAX_SPEED = d("descent_max_horizontal_speed", 1.2, 0.2, 4);
    public static final DoubleValue DESCENT_LAND_RADIUS = d("descent_landing_radius", 4, 1, 12);
    public static final DoubleValue DESCENT_LAND_DAMAGE = d("descent_landing_damage", 3, 0.5, 100);
    public static final DoubleValue DESCENT_LAND_KNOCKBACK = d("descent_landing_knockback", 0.8, 0, 4);

    // 2 AIM Revelation
    public static final DoubleValue REVELATION_CP = d("revelation_cp", 400, 0, 100000);
    public static final DoubleValue REVELATION_OVERLOAD = d("revelation_overload", 30, 0, 10000);
    public static final DoubleValue REVELATION_COOLDOWN = d("revelation_cooldown_seconds", 20, 0, 3600);
    public static final DoubleValue REVELATION_SECONDS = d("revelation_seconds", 5, 1, 30);
    public static final DoubleValue REVELATION_RADIUS = d("revelation_radius", 28, 4, 96);

    // 3 Celestial Spear
    public static final DoubleValue SPEAR_CP = d("spear_cp", 900, 0, 100000);
    public static final DoubleValue SPEAR_OVERLOAD = d("spear_overload", 60, 0, 10000);
    public static final DoubleValue SPEAR_COOLDOWN = d("spear_cooldown_seconds", 8, 0, 3600);
    public static final DoubleValue SPEAR_RANGE = d("spear_range", 28, 4, 96);
    public static final DoubleValue SPEAR_SPEED = d("spear_speed", 2.2, 0.5, 6);
    public static final DoubleValue SPEAR_DAMAGE = d("spear_damage", 12, 0.5, 500);
    public static final DoubleValue SPEAR_DAMAGE_PROF = d("spear_damage_at_full_proficiency_bonus", 6, 0, 500);
    public static final DoubleValue SPEAR_PIERCE = d("spear_max_targets", 3, 1, 10);
    public static final DoubleValue SPEAR_BOSS = d("spear_boss_damage_fraction", 0.6, 0, 1);

    // 4 Sanctuary
    public static final DoubleValue SANCTUARY_CP = d("sanctuary_cp", 1200, 0, 100000);
    public static final DoubleValue SANCTUARY_OVERLOAD = d("sanctuary_overload", 90, 0, 10000);
    public static final DoubleValue SANCTUARY_COOLDOWN = d("sanctuary_cooldown_seconds", 22, 0, 3600);
    public static final DoubleValue SANCTUARY_SECONDS = d("sanctuary_seconds", 5, 1, 30);
    public static final DoubleValue SANCTUARY_RANGE = d("sanctuary_ally_range", 8, 1, 32);
    public static final DoubleValue SANCTUARY_DURABILITY = d("sanctuary_durability", 30, 1, 1000);
    public static final DoubleValue SANCTUARY_BREAK = d("sanctuary_break_threshold", 14, 1, 1000);

    // 5 Seraphic Array
    public static final DoubleValue ARRAY_CP = d("array_cp", 2200, 0, 100000);
    public static final DoubleValue ARRAY_OVERLOAD = d("array_overload", 140, 0, 10000);
    public static final DoubleValue ARRAY_COOLDOWN = d("array_cooldown_seconds", 28, 0, 3600);
    public static final DoubleValue ARRAY_SECONDS = d("array_seconds", 5, 2, 30);
    public static final DoubleValue ARRAY_RANGE = d("array_range", 22, 4, 64);
    public static final DoubleValue ARRAY_CONSTRUCTS = d("array_constructs", 4, 1, 4);
    public static final DoubleValue ARRAY_DAMAGE = d("array_damage", 7, 0.5, 500);
    public static final DoubleValue ARRAY_DAMAGE_PROF = d("array_damage_at_full_proficiency_bonus", 3, 0, 500);

    // 6 Heaven's Rejection
    public static final DoubleValue REJECTION_CP = d("rejection_cp", 1800, 0, 100000);
    public static final DoubleValue REJECTION_OVERLOAD = d("rejection_overload", 120, 0, 10000);
    public static final DoubleValue REJECTION_COOLDOWN = d("rejection_cooldown_seconds", 18, 0, 3600);
    public static final DoubleValue REJECTION_RANGE = d("rejection_range", 9, 2, 32);
    public static final DoubleValue REJECTION_ARC = d("rejection_arc_degrees", 120, 20, 360);
    public static final DoubleValue REJECTION_DAMAGE = d("rejection_damage", 5, 0.5, 500);
    public static final DoubleValue REJECTION_DAMAGE_PROF = d("rejection_damage_at_full_proficiency_bonus", 3, 0, 500);
    public static final DoubleValue REJECTION_KNOCKBACK = d("rejection_knockback", 2.0, 0, 6);

    // 7 Archangel Protocol
    public static final DoubleValue PROTOCOL_CP = d("protocol_cp", 4500, 0, 100000);
    public static final DoubleValue PROTOCOL_OVERLOAD = d("protocol_overload", 220, 0, 10000);
    public static final DoubleValue PROTOCOL_COOLDOWN = d("protocol_cooldown_seconds", 75, 0, 3600);
    public static final DoubleValue PROTOCOL_SECONDS = d("protocol_seconds", 12, 3, 120);
    public static final DoubleValue PROTOCOL_SPEAR = d("protocol_spear_damage_multiplier", 1.3, 1, 5);
    public static final DoubleValue PROTOCOL_SANCTUARY = d("protocol_sanctuary_durability_multiplier", 1.25, 1, 5);

    // 8 AIM Convergence
    public static final DoubleValue CONV_CP = d("convergence_cp", 9500, 0, 100000);
    public static final DoubleValue CONV_OVERLOAD = d("convergence_overload", 420, 0, 10000);
    public static final DoubleValue CONV_COOLDOWN = d("convergence_cooldown_seconds", 150, 0, 3600);
    public static final DoubleValue CONV_RADIUS = d("convergence_radius", 14, 4, 48);
    public static final DoubleValue CONV_SELF_REDUCTION = d("convergence_caster_damage_reduction", 0.5, 0, 0.95);
    public static final DoubleValue CONV_ALLY_REDUCTION = d("convergence_ally_damage_reduction", 0.3, 0, 0.95);
    public static final DoubleValue CONV_CONSTRUCT_DAMAGE = d("convergence_construct_damage", 6, 0.5, 500);
    public static final DoubleValue CONV_FINAL_DAMAGE = d("convergence_final_damage", 18, 0.5, 500);
    public static final DoubleValue CONV_FINAL_DAMAGE_PROF = d("convergence_final_damage_at_full_proficiency_bonus", 8, 0, 500);
    public static final DoubleValue CONV_FINAL_KNOCKBACK = d("convergence_final_knockback", 2.0, 0, 6);
    public static final DoubleValue CONV_HOVER = d("convergence_hover_height", 3, 0, 10);

    public static final ForgeConfigSpec SPEC = B.build();

    public static int ticks(DoubleValue seconds) { return (int) Math.round(seconds.get() * 20.0); }
    public static float f(DoubleValue v) { return v.get().floatValue(); }
}
