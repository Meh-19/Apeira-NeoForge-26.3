package com.apeira.dev.client;

/** The furniture tuner's three modes; the hitbox box editor of earlier versions is gone. */
public enum TunerMode {
    /** Seat count, each seat's position and its sit height. */
    SEATING("Seating Adjuster"),
    /** Right-click the bound piece to delete the footprint box under the crosshair. */
    BOXES("Bounding Box Deleter"),
    /** The light the piece gives (its own, or 0-15) and whether it has collision. */
    LIGHT("Light & Collision");

    private final String label;

    TunerMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    TunerMode next(int steps) {
        TunerMode[] values = values();
        return values[Math.floorMod(ordinal() + Integer.signum(steps), values.length)];
    }
}
