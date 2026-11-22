package com.apeira.dev.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTuning.Seat;
import com.apeira.decor.furniture.SeatFurnitureBlock;

/**
 * The values the furniture tuner changes with Ctrl + scroll, in the HUD's order: the mode, then the mode's own values.
 * Seat positions step by a pixel, or a quarter pixel with Shift held.
 */
public enum TuningParameter {
    MODE("Mode", null),
    SEATS("Seats", TunerMode.SEATING),
    SEAT("Seat", TunerMode.SEATING),
    SEAT_X("Seat X", TunerMode.SEATING),
    SEAT_Y("Sit height", TunerMode.SEATING),
    SEAT_Z("Seat Z", TunerMode.SEATING),
    RESTORE("Deleted boxes", TunerMode.BOXES),
    LIGHT("Light", TunerMode.LIGHT),
    COLLISION("Collision", TunerMode.LIGHT);

    private final String label;
    private final @Nullable TunerMode mode;

    TuningParameter(String label, @Nullable TunerMode mode) {
        this.label = label;
        this.mode = mode;
    }

    public String label() {
        return label;
    }

    /** Whether the parameter is shown in {@code mode} for {@code block}: seat values only for seat pieces. */
    public boolean appliesTo(TunerMode mode, FurnitureBlock block) {
        if (this.mode == null) {
            return true;
        }
        return this.mode == mode && (mode != TunerMode.SEATING || block instanceof SeatFurnitureBlock);
    }

    /** The parameters shown in {@code mode} for {@code block}. */
    public static List<TuningParameter> shown(TunerMode mode, FurnitureBlock block) {
        List<TuningParameter> shown = new ArrayList<>();
        for (TuningParameter parameter : values()) {
            if (parameter.appliesTo(mode, block)) {
                shown.add(parameter);
            }
        }
        return shown;
    }

    /** The value as the HUD shows it, for the selected {@code seat}. */
    public String value(TunerMode mode, FurnitureBlock block, FurnitureTuning tuning, int seat) {
        Seat selected = seat < tuning.seats().size() ? tuning.seats().get(seat) : null;
        return switch (this) {
            case MODE -> mode.label();
            case SEATS -> String.valueOf(tuning.seats().size());
            case SEAT -> tuning.seats().isEmpty() ? "none" : (seat + 1) + " / " + tuning.seats().size();
            case SEAT_X -> selected == null ? "-" : number(selected.x());
            case SEAT_Y -> selected == null ? "-" : number(selected.y());
            case SEAT_Z -> selected == null ? "-" : number(selected.z());
            case RESTORE -> tuning.removed().size() + (tuning.removed().isEmpty() ? "" : " (scroll to restore the last)");
            case LIGHT -> tuning.light() < 0 ? "off (own: " + block.defaultBlockState().getLightEmission() + ")" : String.valueOf(tuning.light());
            case COLLISION -> tuning.collision() ? "on" : "off";
        };
    }

    /**
     * Changes the value by {@code steps}. The mode and the selected seat are the session's, so {@link #MODE} and
     * {@link #SEAT} return the tuning unchanged.
     */
    public FurnitureTuning adjust(FurnitureTuning tuning, int seat, int steps, boolean fine) {
        double length = steps * (fine ? 0.25 : 1.0);
        Seat selected = seat < tuning.seats().size() ? tuning.seats().get(seat) : null;
        return switch (this) {
            case MODE, SEAT -> tuning;
            case SEATS -> seats(tuning, steps);
            case SEAT_X -> selected == null ? tuning : tuning.withSeat(seat, new Seat(selected.x() + length, selected.y(), selected.z()));
            case SEAT_Y -> selected == null ? tuning
                    : tuning.withSeat(seat, new Seat(selected.x(), Math.max(0.0, selected.y() + length), selected.z()));
            case SEAT_Z -> selected == null ? tuning : tuning.withSeat(seat, new Seat(selected.x(), selected.y(), selected.z() + length));
            case RESTORE -> tuning.removed().isEmpty() ? tuning : tuning.withRemoved(tuning.removed().subList(0, tuning.removed().size() - 1));
            case LIGHT -> tuning.withLight(tuning.light() + steps);
            case COLLISION -> tuning.withCollision(!tuning.collision());
        };
    }

    /** Adds seats (copies of the last, a seat at the block's centre at 8 pixels for none) or removes the last; at least one stays. */
    static FurnitureTuning seats(FurnitureTuning tuning, int steps) {
        List<Seat> seats = new ArrayList<>(tuning.seats());
        if (steps > 0 && seats.size() < FurnitureTuning.MAX_ENTRIES) {
            seats.add(seats.isEmpty() ? new Seat(8, 8, 8) : seats.getLast());
        } else if (steps < 0 && seats.size() > 1) {
            seats.removeLast();
        }
        return tuning.withSeats(seats);
    }

    /** A number with up to two decimals and no trailing zeros. */
    public static String number(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        text = text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
        return text.equals("-0") ? "0" : text;
    }
}
