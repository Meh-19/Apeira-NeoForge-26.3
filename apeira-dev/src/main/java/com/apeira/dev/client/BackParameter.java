package com.apeira.dev.client;

import com.apeira.decor.cosmetic.BackOffset;

import net.minecraft.util.Mth;

/**
 * The back cosmetic values the dev wand changes with Ctrl + scroll while it is bound to no furniture piece and the
 * player wears a back cosmetic. Moves step by a model pixel (Shift: a quarter), turns by 5 degrees (Shift: 1), scale by
 * 0.1 (Shift: 0.01).
 */
public enum BackParameter {
    X("Offset X"),
    Y("Offset Y"),
    Z("Offset Z"),
    PITCH("Pitch (X)"),
    YAW("Yaw (Y)"),
    ROLL("Roll (Z)"),
    SCALE("Scale");

    private final String label;

    BackParameter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isMove() {
        return this == X || this == Y || this == Z;
    }

    public boolean isTurn() {
        return this == PITCH || this == YAW || this == ROLL;
    }

    public String value(BackOffset offset) {
        return switch (this) {
            case X -> TuningParameter.number(offset.x());
            case Y -> TuningParameter.number(offset.y());
            case Z -> TuningParameter.number(offset.z());
            case PITCH -> TuningParameter.number(offset.pitch()) + "°";
            case YAW -> TuningParameter.number(offset.yaw()) + "°";
            case ROLL -> TuningParameter.number(offset.roll()) + "°";
            case SCALE -> TuningParameter.number(offset.scale());
        };
    }

    public BackOffset adjust(BackOffset o, int steps, boolean fine) {
        float move = steps * (fine ? 0.25F : 1.0F);
        float turn = steps * (fine ? 1.0F : 5.0F);
        BackOffset changed = switch (this) {
            case X -> new BackOffset(o.x() + move, o.y(), o.z(), o.pitch(), o.yaw(), o.roll(), o.scale());
            case Y -> new BackOffset(o.x(), o.y() + move, o.z(), o.pitch(), o.yaw(), o.roll(), o.scale());
            case Z -> new BackOffset(o.x(), o.y(), o.z() + move, o.pitch(), o.yaw(), o.roll(), o.scale());
            case PITCH -> new BackOffset(o.x(), o.y(), o.z(), o.pitch() + turn, o.yaw(), o.roll(), o.scale());
            case YAW -> new BackOffset(o.x(), o.y(), o.z(), o.pitch(), o.yaw() + turn, o.roll(), o.scale());
            case ROLL -> new BackOffset(o.x(), o.y(), o.z(), o.pitch(), o.yaw(), o.roll() + turn, o.scale());
            case SCALE -> new BackOffset(o.x(), o.y(), o.z(), o.pitch(), o.yaw(), o.roll(),
                    Mth.clamp(Math.round((o.scale() + steps * (fine ? 0.01F : 0.1F)) * 100.0F) / 100.0F, 0.05F, 8.0F));
        };
        return changed.sanitized();
    }
}
