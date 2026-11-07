package dev.toaru.kazakiri.net;

/** Client-side visual effects. The server only says what and where; clients draw geometry. Nothing is spawned. */
public enum FxType {
    /** pos = feet. Air distortion under the launch. */
    DESCENT_LAUNCH,
    /** pos = feet, aux.x = radius. Wing-assisted landing pressure wave. */
    LANDING,
    /** a = player id, duration = revelation length, aux.x = radius. Halo with three rings + scanning plane. */
    REVELATION,
    /** a = player id, duration = charge ticks. Spear assembling above the hand. */
    SPEAR_CHARGE,
    /** pos = start, aux = velocity per tick, power = range. The flying spear (straight, rotating). */
    SPEAR,
    /** pos = impact. The spear unfolds into a geometric distortion. */
    SPEAR_HIT,
    /** a = protected entity id. Closed-wing shell. */
    SANCTUARY,
    /** a = protected entity id, aux = direction of the hit. Sections deform and ripple. */
    SANCTUARY_HIT,
    /** pos = centre. The shell unfolds and disappears. */
    SANCTUARY_END,
    /** a = player id, c = number of constructs. Wing-shaped constructs behind the caster. */
    ARRAY,
    /** a = caster id, b = target id, c = construct index. One construct turns, compresses and strikes. */
    ARRAY_STRIKE,
    /** pos = chest, aux = facing, power = range. Curved distortion wave. */
    REJECTION,
    /** a = player id. Transformation sequence (float, halos, completion burst). */
    PROTOCOL_START,
    /** a = player id, duration = whole ultimate. The giant projection timeline. */
    CONVERGENCE,
    /** pos = centre, aux = caster facing, power = radius. The final synchronized wing strike. */
    CONV_FINAL
}
