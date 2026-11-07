package dev.toaru.kazakiri;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player visual options (config/kazakiri-client.toml). Only read on the client, while the game runs. */
public final class ClientCfg {
    private ClientCfg() { }
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue CAMERA_EFFECTS = B
            .comment("Cinematic camera (zoom, tilt, shake) when YOU use Archangel. Other players' visuals are not affected.")
            .define("camera_effects", true);
    public static final ForgeConfigSpec.BooleanValue SCREEN_FLASH = B
            .comment("White flash on screen when an Archangel releases near you.")
            .define("screen_flash", true);
    public static final ForgeConfigSpec.BooleanValue SHOW_OWN_WINGS_FIRST_PERSON = B
            .comment("Draw your own wings and halo while in first-person view.")
            .define("show_own_wings_first_person", false);

    public static final ForgeConfigSpec SPEC = B.build();
}
