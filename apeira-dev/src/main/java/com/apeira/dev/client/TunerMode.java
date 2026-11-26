package com.apeira.dev.client;

/** The furniture tuner's four modes; the hitbox box editor of earlier versions is gone. */
public enum TunerMode {
    /** Seat count, each seat's position and its sit height. */
    SEATING("Seating Adjuster"),
    /** Right-click the bound piece to delete the footprint box under the crosshair. */
    BOXES("Bounding Box Deleter"),
    /** The light the piece gives (its own, or 0-15) and whether it has collision. */
    LIGHT("Light & Collision"),
    /** Where the model is drawn: its offset (pixels) and its turn about the block's centre column (degrees). */
    MODEL("Offset & Rotation");

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
