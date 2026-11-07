package dev.toaru.kazakiri;

/** Body pose ids (sent to clients; each maps to assets/kazakiri/player_animation/<name>.json). */
public final class Anims {
    private Anims() { }
    public static final int STOP = 0, DESCENT = 1, SPEAR = 2, SANCTUARY = 3, REVELATION = 4, ARRAY = 5, REJECTION = 6,
            PROTOCOL = 7, CONVERGENCE = 8;
    private static final String[] NAMES = {null, "descent", "spear", "sanctuary", "revelation", "array", "rejection", "protocol", "convergence"};

    public static String name(int id) { return id > 0 && id < NAMES.length ? NAMES[id] : null; }
}
