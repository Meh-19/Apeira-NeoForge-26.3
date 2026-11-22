package com.apeira.dev.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTuning.Seat;
import com.apeira.decor.furniture.SeatFurnitureBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/**
 * The furniture tuner's editor: the three mode tabs and, for the picked mode, the seats (count, selection, typed
 * positions and sit height), the deleted boxes (restore the last or all; deleting is a right-click on the piece), or the
 * light (off or 0-15) and collision toggle. Every edit goes to the tune log at once. Typed values apply as soon as they
 * read as numbers; the world keeps running behind it.
 */
public final class FurnitureEditorScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int FIELD_WIDTH = 50;
    private static final int ROW = 22;

    private final List<Label> labels = new ArrayList<>();

    public FurnitureEditorScreen() {
        super(Component.translatable("screen.apeira.dev.furniture_editor"));
    }

    @Override
    protected void init() {
        labels.clear();
        FurnitureBlock block = DevSession.block();
        if (block == null) {
            return;
        }
        FurnitureTuning tuning = DevSession.tuning(block);
        int left = width / 2 - 150;
        int y = 30;

        // Mode tabs.
        int x = left;
        for (TunerMode mode : TunerMode.values()) {
            boolean on = mode == DevSession.mode();
            Component text = Component.literal(mode.label()).withStyle(on ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            addRenderableWidget(Button.builder(text, b -> {
                DevSession.setMode(mode);
                rebuildWidgets();
            }).bounds(x, y, 98, 20).build());
            x += 101;
        }
        y += ROW + 8;

        switch (DevSession.mode()) {
            case SEATING -> y = seating(block, tuning, left, y);
            case BOXES -> y = boxes(tuning, left, y);
            case LIGHT -> y = light(tuning, left, y);
        }
        y += 8;

        addRenderableWidget(Button.builder(Component.literal("Open tune log"), b -> openLog()).bounds(left, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset to code"), b -> reset()).bounds(left + 102, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(left + 204, y, 96, 20).build());
    }

    private int seating(FurnitureBlock block, FurnitureTuning tuning, int left, int y) {
        if (!(block instanceof SeatFurnitureBlock)) {
            labels.add(new Label(Component.translatable("hud.apeira.dev.not_a_seat").getString(), left, y + 6, GREY));
            return y + ROW;
        }
        int seat = DevSession.seat();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> selectSeat(seat - 1)).bounds(left, y, 20, 20).build());
        labels.add(new Label("Seat " + TuningParameter.SEAT.value(DevSession.mode(), block, tuning, seat), left + 26, y + 6, WHITE));
        addRenderableWidget(Button.builder(Component.literal(">"), b -> selectSeat(seat + 1)).bounds(left + 90, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Add seat"), b -> seats(1)).bounds(left + 120, y, 85, 20).build());
        Button remove = addRenderableWidget(Button.builder(Component.literal("Remove seat"), b -> seats(-1)).bounds(left + 215, y, 85, 20).build());
        remove.active = tuning.seats().size() > 1;
        y += ROW + 4;
        if (seat < tuning.seats().size()) {
            Seat selected = tuning.seats().get(seat);
            field(left, y, "Seat X", selected.x(), (t, v) -> editSeat(t, s -> new Seat(v, s.y(), s.z())));
            field(left + 150, y, "Seat Z", selected.z(), (t, v) -> editSeat(t, s -> new Seat(s.x(), s.y(), v)));
            y += ROW;
            field(left, y, "Sit height", selected.y(), (t, v) -> editSeat(t, s -> new Seat(s.x(), Math.max(0.0, v), s.z())));
            y += ROW;
        }
        return y;
    }

    private int boxes(FurnitureTuning tuning, int left, int y) {
        labels.add(new Label(Component.translatable("hud.apeira.dev.box_deleter").getString(), left, y, GREY));
        y += 14;
        labels.add(new Label("Deleted boxes: " + tuning.removed().size(), left, y + 6, WHITE));
        Button last = addRenderableWidget(Button.builder(Component.literal("Restore last"), b -> restore(false)).bounds(left + 120, y, 85, 20).build());
        Button all = addRenderableWidget(Button.builder(Component.literal("Restore all"), b -> restore(true)).bounds(left + 215, y, 85, 20).build());
        last.active = all.active = !tuning.removed().isEmpty();
        return y + ROW;
    }

    private int light(FurnitureTuning tuning, int left, int y) {
        boolean lit = tuning.light() >= 0;
        Component lightText = Component.literal(lit ? "Light: on" : "Light: off").withStyle(lit ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        addRenderableWidget(Button.builder(lightText, b -> edit(t -> t.withLight(t.light() >= 0 ? FurnitureTuning.OWN_LIGHT : 15)))
                .bounds(left, y, 96, 20).build());
        if (lit) {
            field(left + 150, y, "Level (0-15)", tuning.light(), (t, v) -> t.withLight((int) Math.round(Math.max(0.0, Math.min(15.0, v)))));
        }
        y += ROW;
        Component collision = Component.literal(tuning.collision() ? "Collision: on" : "Collision: off")
                .withStyle(tuning.collision() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        addRenderableWidget(Button.builder(collision, b -> edit(t -> t.withCollision(!t.collision()))).bounds(left, y, 96, 20).build());
        return y + ROW;
    }

    private FurnitureTuning editSeat(FurnitureTuning tuning, java.util.function.UnaryOperator<Seat> edit) {
        int seat = DevSession.seat();
        return seat < tuning.seats().size() ? tuning.withSeat(seat, edit.apply(tuning.seats().get(seat))) : tuning;
    }

    /** A labelled number field; its value applies as soon as it reads as a number. */
    private void field(int x, int y, String label, double value, BiFunction<FurnitureTuning, Double, FurnitureTuning> apply) {
        labels.add(new Label(label, x, y + 6, WHITE));
        EditBox field = new EditBox(font, x + 150 - FIELD_WIDTH - 8, y, FIELD_WIDTH, 18, Component.literal(label));
        field.setValue(TuningParameter.number(value));
        field.setResponder(text -> {
            Double parsed = parse(text);
            FurnitureBlock block = DevSession.block();
            if (parsed != null && block != null) {
                DevSession.apply(block, apply.apply(DevSession.tuning(block), parsed));
            }
        });
        addRenderableWidget(field);
    }

    private static @Nullable Double parse(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void edit(java.util.function.UnaryOperator<FurnitureTuning> edit) {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            DevSession.apply(block, edit.apply(DevSession.tuning(block)));
            rebuildWidgets();
        }
    }

    private void selectSeat(int index) {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            DevSession.selectSeat(index, DevSession.tuning(block));
            rebuildWidgets();
        }
    }

    private void seats(int steps) {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            FurnitureTuning changed = TuningParameter.seats(DevSession.tuning(block), steps);
            DevSession.apply(block, changed);
            DevSession.selectSeat(steps > 0 ? changed.seats().size() - 1 : DevSession.seat(), changed);
            rebuildWidgets();
        }
    }

    private void restore(boolean all) {
        edit(t -> t.removed().isEmpty() ? t : t.withRemoved(all ? List.of() : t.removed().subList(0, t.removed().size() - 1)));
    }

    private void reset() {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            DevSession.reset(block);
            rebuildWidgets();
        }
    }

    private void openLog() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable("message.apeira.dev.tune_log_file",
                    Component.literal(TuneLog.FILE).withStyle(style -> style.withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenFile(TuneLog.file().toAbsolutePath().toString())))));
        }
    }

    @Override
    public void tick() {
        if (DevSession.block() == null) {
            onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            String state = DevSession.tuned(block) ? " (tuned)" : " (code)";
            graphics.centeredText(font, Component.literal(block.getName().getString() + state), width / 2, 14, WHITE);
        }
        for (Label label : labels) {
            graphics.text(font, label.text, label.x, label.y, label.color, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Label(String text, int x, int y, int color) {}
}
