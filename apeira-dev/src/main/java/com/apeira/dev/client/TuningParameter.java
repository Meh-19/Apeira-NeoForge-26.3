package com.apeira.dev.client;

import java.util.Locale;

import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTuning.Box;

import net.minecraft.util.Mth;

/**
 * The values the dev wand changes with Ctrl + scroll, in the HUD's order. Lengths step by a pixel, or a quarter pixel
 * with Shift held; scale by 0.1, or 0.01.
 */
public enum TuningParameter {
    BOX("Box"),
    MIN_X("Min X"),
    MIN_Y("Min Y"),
    MIN_Z("Min Z"),
    MAX_X("Max X"),
    MAX_Y("Max Y"),
    MAX_Z("Max Z"),
    SCALE("Scale"),
    OFFSET_X("Offset X"),
    OFFSET_Y("Offset Y"),
    OFFSET_Z("Offset Z"),
    ROTATION("Rotation"),
    SEAT_HEIGHT("Seat height");

    private final String label;

    TuningParameter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean appliesTo(FurnitureBlock block) {
        return this != SEAT_HEIGHT || block instanceof FurnitureBlock.Seat;
    }

    /** The value as the HUD shows it, for the selected {@code box}. */
    public String value(FurnitureTuning tuning, int box) {
        Box selected = box < tuning.boxes().size() ? tuning.boxes().get(box) : null;
        return switch (this) {
            case BOX -> tuning.boxes().isEmpty() ? "none" : (box + 1) + " / " + tuning.boxes().size();
            case MIN_X -> selected == null ? "-" : number(selected.minX());
            case MIN_Y -> selected == null ? "-" : number(selected.minY());
            case MIN_Z -> selected == null ? "-" : number(selected.minZ());
            case MAX_X -> selected == null ? "-" : number(selected.maxX());
            case MAX_Y -> selected == null ? "-" : number(selected.maxY());
            case MAX_Z -> selected == null ? "-" : number(selected.maxZ());
            case SCALE -> number(tuning.scale());
            case OFFSET_X -> number(tuning.offsetX());
            case OFFSET_Y -> number(tuning.offsetY());
            case OFFSET_Z -> number(tuning.offsetZ());
            case ROTATION -> "+" + tuning.rotation() * 45 + "°";
            case SEAT_HEIGHT -> tuning.seat() ? number(tuning.seatHeight()) : "-";
        };
    }

    /** Changes the value by {@code steps}; selecting another box is the session's, so {@link #BOX} returns it unchanged. */
    public FurnitureTuning adjust(FurnitureTuning tuning, int box, int steps, boolean fine) {
        double length = steps * (fine ? 0.25 : 1.0);
        Box selected = box < tuning.boxes().size() ? tuning.boxes().get(box) : null;
        return switch (this) {
            case BOX -> tuning;
            case MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z -> selected == null ? tuning : tuning.withBox(box, moveFace(selected, length));
            case SCALE -> tuning.withScale(Mth.clamp(Math.round((tuning.scale() + steps * (fine ? 0.01F : 0.1F)) * 100.0F) / 100.0F, 0.05F, 16.0F));
            case OFFSET_X -> tuning.withOffset((float) (tuning.offsetX() + length), tuning.offsetY(), tuning.offsetZ());
            case OFFSET_Y -> tuning.withOffset(tuning.offsetX(), (float) (tuning.offsetY() + length), tuning.offsetZ());
            case OFFSET_Z -> tuning.withOffset(tuning.offsetX(), tuning.offsetY(), (float) (tuning.offsetZ() + length));
            case ROTATION -> tuning.withRotation(tuning.rotation() + steps);
            case SEAT_HEIGHT -> tuning.withSeatHeight(Mth.clamp(Math.max(0.0, tuning.seatHeight()) + length, 0.0, FurnitureTuning.MAX_HEIGHT));
        };
    }

    /** Moves one face of {@code box}, keeping it inside the block and at least a quarter pixel thick. */
    private Box moveFace(Box box, double length) {
        double minX = box.minX(), minY = box.minY(), minZ = box.minZ(), maxX = box.maxX(), maxY = box.maxY(), maxZ = box.maxZ();
        double thin = 0.25;
        switch (this) {
            case MIN_X -> minX = Mth.clamp(minX + length, 0.0, maxX - thin);
            case MIN_Y -> minY = Mth.clamp(minY + length, 0.0, maxY - thin);
            case MIN_Z -> minZ = Mth.clamp(minZ + length, 0.0, maxZ - thin);
            case MAX_X -> maxX = Mth.clamp(maxX + length, minX + thin, 16.0);
            case MAX_Y -> maxY = Mth.clamp(maxY + length, minY + thin, FurnitureTuning.MAX_HEIGHT);
            case MAX_Z -> maxZ = Mth.clamp(maxZ + length, minZ + thin, 16.0);
            default -> {}
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** A number with up to two decimals and no trailing zeros, as the catalogue writes them. */
    public static String number(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        text = text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
        return text.equals("-0") ? "0" : text;
    }
}
