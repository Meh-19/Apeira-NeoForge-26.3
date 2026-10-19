package com.apeira.dev.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurniturePlacement;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTuning.Box;
import com.apeira.dev.ApeiraDev;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/**
 * The dev wand's editor: the bound piece's boxes, scale, offsets, rotation, seat and placements as typed values and
 * toggles, and the exporter. Typed values apply as soon as they read as numbers; the world keeps running behind it.
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
        int box = DevSession.box();
        int left = width / 2 - 150;
        int y = 34;

        // Boxes: pick, add, remove.
        addRenderableWidget(Button.builder(Component.literal("<"), b -> selectBox(box - 1)).bounds(left, y, 20, 20).build());
        labels.add(new Label(TuningParameter.BOX.label() + " " + TuningParameter.BOX.value(tuning, box), left + 26, y + 6, WHITE));
        addRenderableWidget(Button.builder(Component.literal(">"), b -> selectBox(box + 1)).bounds(left + 90, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Add box"), b -> addBox()).bounds(left + 120, y, 85, 20).build());
        Button remove = addRenderableWidget(Button.builder(Component.literal("Remove box"), b -> removeBox()).bounds(left + 215, y, 85, 20).build());
        remove.active = box < tuning.boxes().size();
        y += ROW + 4;

        // The selected box, min on the left and max on the right.
        if (box < tuning.boxes().size()) {
            Box selected = tuning.boxes().get(box);
            y = boxRow(left, y, "X", selected.minX(), selected.maxX(), (b, v) -> new Box(v, b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()),
                    (b, v) -> new Box(b.minX(), b.minY(), b.minZ(), v, b.maxY(), b.maxZ()));
            y = boxRow(left, y, "Y", selected.minY(), selected.maxY(), (b, v) -> new Box(b.minX(), v, b.minZ(), b.maxX(), b.maxY(), b.maxZ()),
                    (b, v) -> new Box(b.minX(), b.minY(), b.minZ(), b.maxX(), v, b.maxZ()));
            y = boxRow(left, y, "Z", selected.minZ(), selected.maxZ(), (b, v) -> new Box(b.minX(), b.minY(), v, b.maxX(), b.maxY(), b.maxZ()),
                    (b, v) -> new Box(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), v));
        }

        // The model.
        field(left, y, "Scale", tuning.scale(), (t, v) -> t.withScale(v.floatValue()));
        field(left + 150, y, "Rotation (x45°)", tuning.rotation(), (t, v) -> t.withRotation((int) Math.round(v)));
        y += ROW;
        field(left, y, "Offset X", tuning.offsetX(), (t, v) -> t.withOffset(v.floatValue(), t.offsetY(), t.offsetZ()));
        field(left + 150, y, "Offset Y", tuning.offsetY(), (t, v) -> t.withOffset(t.offsetX(), v.floatValue(), t.offsetZ()));
        y += ROW;
        field(left, y, "Offset Z", tuning.offsetZ(), (t, v) -> t.withOffset(t.offsetX(), t.offsetY(), v.floatValue()));
        if (block instanceof FurnitureBlock.Seat) {
            field(left + 150, y, "Seat height", Math.max(0.0, tuning.seatHeight()), (t, v) -> t.withSeatHeight(v));
        }
        y += ROW + 4;

        // Placements.
        labels.add(new Label("Placement", left, y - 10, GREY));
        int x = left;
        for (FurniturePlacement placement : FurniturePlacement.values()) {
            boolean on = placement.in(tuning.placements());
            Component text = Component.literal(placement.name()).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
            addRenderableWidget(Button.builder(text, b -> togglePlacement(placement)).bounds(x, y, 72, 20).build());
            x += 76;
        }
        y += ROW + 8;

        addRenderableWidget(Button.builder(Component.literal("Export"), b -> export()).bounds(left, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset to catalogue"), b -> reset()).bounds(left + 102, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(left + 204, y, 96, 20).build());
    }

    private int boxRow(int left, int y, String axis, double min, double max, BiFunction<Box, Double, Box> setMin, BiFunction<Box, Double, Box> setMax) {
        field(left, y, "Min " + axis, min, (t, v) -> editBox(t, b -> setMin.apply(b, v)));
        field(left + 150, y, "Max " + axis, max, (t, v) -> editBox(t, b -> setMax.apply(b, v)));
        return y + ROW;
    }

    private FurnitureTuning editBox(FurnitureTuning tuning, Function<Box, Box> edit) {
        int box = DevSession.box();
        if (box >= tuning.boxes().size()) {
            return tuning;
        }
        Box edited = edit.apply(tuning.boxes().get(box)).clamped();
        return edited != null ? tuning.withBox(box, edited) : tuning;
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

    private void selectBox(int index) {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            DevSession.selectBox(index, DevSession.tuning(block));
            rebuildWidgets();
        }
    }

    /** Adds a box in the middle of the block, to be pulled into place. */
    private void addBox() {
        FurnitureBlock block = DevSession.block();
        FurnitureTuning tuning = block != null ? DevSession.tuning(block) : null;
        if (block == null || tuning.boxes().size() >= FurnitureTuning.MAX_BOXES) {
            return;
        }
        List<Box> boxes = new ArrayList<>(tuning.boxes());
        boxes.add(new Box(4, 0, 4, 12, 8, 12));
        DevSession.apply(block, tuning.withBoxes(boxes));
        DevSession.selectBox(boxes.size() - 1, DevSession.tuning(block));
        rebuildWidgets();
    }

    private void removeBox() {
        FurnitureBlock block = DevSession.block();
        if (block == null) {
            return;
        }
        FurnitureTuning tuning = DevSession.tuning(block);
        if (DevSession.box() < tuning.boxes().size()) {
            List<Box> boxes = new ArrayList<>(tuning.boxes());
            boxes.remove(DevSession.box());
            DevSession.apply(block, tuning.withBoxes(boxes));
            rebuildWidgets();
        }
    }

    private void togglePlacement(FurniturePlacement placement) {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            FurnitureTuning tuning = DevSession.tuning(block);
            int placements = tuning.placements() ^ placement.bit();
            // A piece must go somewhere.
            if (placements != 0) {
                DevSession.apply(block, tuning.withPlacements(placements));
            }
            rebuildWidgets();
        }
    }

    private void reset() {
        FurnitureBlock block = DevSession.block();
        if (block != null) {
            DevSession.reset(block);
            rebuildWidgets();
        }
    }

    private void export() {
        FurnitureBlock block = DevSession.block();
        if (block == null || minecraft == null || minecraft.player == null) {
            return;
        }
        try {
            Path file = FurnitureExporter.export(block, DevSession.tuning(block));
            minecraft.player.sendSystemMessage(Component.translatable("message.apeira.dev.exported", block.getName(),
                    Component.literal(FurnitureExporter.FILE).withStyle(style -> style.withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenFile(file.toAbsolutePath().toString())))));
        } catch (IOException e) {
            ApeiraDev.LOGGER.error("Furniture export failed", e);
            minecraft.player.sendSystemMessage(Component.translatable("message.apeira.dev.export_failed", e.getMessage()));
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
            String state = DevSession.tuned(block) ? " (tuned)" : " (catalogue)";
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
