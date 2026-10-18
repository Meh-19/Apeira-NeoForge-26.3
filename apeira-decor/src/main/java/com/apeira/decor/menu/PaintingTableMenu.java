package com.apeira.decor.menu;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.Paint;
import com.apeira.decor.paint.PaintColor;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorMenus;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The painting table: put a paintbrush or a paintable furniture item in the input slot, pick a colour and shade or a
 * finish, and take one painted copy from the output slot, which uses up one input item.
 *
 * <p>The screen sends its clicks as vanilla menu button clicks ({@link #clickMenuButton}), and the selection is synced
 * back to it through data slots, so the menu needs no payloads of its own.
 */
public class PaintingTableMenu extends AbstractContainerMenu {
    // Data slots
    private static final int DATA_COLOR = 0;
    private static final int DATA_SHADE = 1;
    private static final int DATA_FINISH_MODE = 2;
    private static final int DATA_FINISH = 3;
    private static final int DATA_PAGE = 4;
    private static final int DATA_COUNT = 5;

    // Button ids: colours 0-8, then shades 9-15, then the finish controls.
    private static final int SHADE_BUTTONS = PaintColor.values().length;
    /** Switches between colours and finishes. */
    public static final int BUTTON_FINISH_TOGGLE = SHADE_BUTTONS + PaintColor.SHADES;
    public static final int BUTTON_PAGE_LEFT = BUTTON_FINISH_TOGGLE + 1;
    public static final int BUTTON_PAGE_RIGHT = BUTTON_FINISH_TOGGLE + 2;
    /** The first finish on the current page; the page's other finishes follow. */
    public static final int BUTTON_FINISH = BUTTON_FINISH_TOGGLE + 3;

    /** Finishes in the order the screen shows them, {@link #FINISHES_PER_PAGE} to a page. */
    public static final FinishType[] FINISH_ORDER = {
            FinishType.RAINBOW, FinishType.GOLD, FinishType.MOLTEN, FinishType.BUBBLE, FinishType.FLORAL, FinishType.GALAXY,
            FinishType.MATRIX,
            FinishType.CHROME, FinishType.GLITCH, FinishType.CUBED, FinishType.SCANNER, FinishType.PEARL, FinishType.PHANTOM,
            FinishType.SNOW};
    public static final int FINISHES_PER_PAGE = 7;
    public static final int LAST_PAGE = (FINISH_ORDER.length - 1) / FINISHES_PER_PAGE;

    private static final int INPUT_SLOT = 0;
    private static final int RESULT_SLOT = 1;
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private long lastSoundTime;

    /** The client's menu, opened by the server. */
    public PaintingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public PaintingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(DecorMenus.PAINTING_TABLE.get(), containerId);
        this.access = access;
        this.data = new SimpleContainerData(DATA_COUNT);
        clearSelection();
        addDataSlots(data);

        addSlot(new Slot(input, 0, 26, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Paint.canPaint(stack);
            }
        });
        addSlot(new Slot(result, 0, 134, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                input.removeItem(0, 1);
                access.execute((level, pos) -> {
                    if (lastSoundTime != level.getGameTime()) {
                        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        lastSoundTime = level.getGameTime();
                    }
                });
                super.onTake(player, carried);
            }
        });
        addStandardInventorySlots(inventory, 8, 119);
    }

    // Selection, read by the screen

    public boolean hasInput() {
        return Paint.canPaint(input.getItem(0));
    }

    public boolean isFinishMode() {
        return data.get(DATA_FINISH_MODE) == 1;
    }

    /** The selected colour, or {@code null}. */
    public @Nullable PaintColor selectedColor() {
        int index = data.get(DATA_COLOR);
        return index >= 0 && index < PaintColor.values().length ? PaintColor.values()[index] : null;
    }

    /** The selected shade of {@link #selectedColor()}, or -1. */
    public int selectedShade() {
        return data.get(DATA_SHADE);
    }

    /** The current page of finishes. */
    public int finishPage() {
        return data.get(DATA_PAGE);
    }

    /** The finish shown in {@code slot} (0-6) of the current page, or {@code null} past the last finish. */
    public @Nullable FinishType finishInSlot(int slot) {
        int index = finishPage() * FINISHES_PER_PAGE + slot;
        return slot >= 0 && slot < FINISHES_PER_PAGE && index < FINISH_ORDER.length ? FINISH_ORDER[index] : null;
    }

    /** The slot (0-6) of the selected finish on the current page, or -1 when it is on another page or none is selected. */
    public int selectedFinishSlot() {
        int finish = data.get(DATA_FINISH);
        int slot = finish - finishPage() * FINISHES_PER_PAGE;
        return finish >= 0 && slot >= 0 && slot < FINISHES_PER_PAGE ? slot : -1;
    }

    // Clicks

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < SHADE_BUTTONS) {
            selectColor(PaintColor.values()[id]);
        } else if (id >= SHADE_BUTTONS && id < BUTTON_FINISH_TOGGLE) {
            selectShade(id - SHADE_BUTTONS);
        } else if (id == BUTTON_FINISH_TOGGLE) {
            toggleFinishMode();
        } else if (id == BUTTON_PAGE_LEFT) {
            data.set(DATA_PAGE, Math.max(0, finishPage() - 1));
        } else if (id == BUTTON_PAGE_RIGHT) {
            data.set(DATA_PAGE, Math.min(LAST_PAGE, finishPage() + 1));
        } else if (id >= BUTTON_FINISH && id < BUTTON_FINISH + FINISHES_PER_PAGE) {
            selectFinish(id - BUTTON_FINISH);
        } else {
            return false;
        }
        return true;
    }

    private void selectColor(PaintColor color) {
        if (!hasInput()) {
            return;
        }
        data.set(DATA_FINISH_MODE, 0);
        data.set(DATA_FINISH, -1);
        data.set(DATA_COLOR, color.ordinal());
        data.set(DATA_SHADE, color.defaultShade());
        updateResult();
    }

    private void selectShade(int shade) {
        if (!hasInput() || selectedColor() == null) {
            return;
        }
        data.set(DATA_FINISH_MODE, 0);
        data.set(DATA_FINISH, -1);
        data.set(DATA_SHADE, shade);
        updateResult();
    }

    private void toggleFinishMode() {
        if (!hasInput()) {
            return;
        }
        if (isFinishMode()) {
            data.set(DATA_FINISH_MODE, 0);
            data.set(DATA_FINISH, -1);
        } else {
            data.set(DATA_FINISH_MODE, 1);
            data.set(DATA_COLOR, -1);
            data.set(DATA_SHADE, -1);
        }
        updateResult();
    }

    private void selectFinish(int slot) {
        if (!hasInput() || finishInSlot(slot) == null) {
            return;
        }
        data.set(DATA_FINISH_MODE, 1);
        data.set(DATA_FINISH, finishPage() * FINISHES_PER_PAGE + slot);
        data.set(DATA_COLOR, -1);
        data.set(DATA_SHADE, -1);
        updateResult();
    }

    private void clearSelection() {
        data.set(DATA_COLOR, -1);
        data.set(DATA_SHADE, -1);
        data.set(DATA_FINISH_MODE, 0);
        data.set(DATA_FINISH, -1);
        data.set(DATA_PAGE, 0);
    }

    // Result

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == input) {
            if (!hasInput()) {
                clearSelection();
            }
            updateResult();
        }
    }

    /** The paint the current selection applies, or -1 when nothing is selected. */
    private int selectedPaint() {
        if (isFinishMode()) {
            int finish = data.get(DATA_FINISH);
            return finish >= 0 && finish < FINISH_ORDER.length ? FINISH_ORDER[finish].paintValue() : -1;
        }
        PaintColor color = selectedColor();
        int shade = selectedShade();
        return color != null && shade >= 0 && shade < PaintColor.SHADES ? color.shade(shade) : -1;
    }

    private void updateResult() {
        ItemStack stack = input.getItem(0);
        int paint = selectedPaint();
        if (!Paint.canPaint(stack) || paint < 0) {
            result.setItem(0, ItemStack.EMPTY);
        } else {
            ItemStack painted = stack.copyWithCount(1);
            Paint.set(painted, paint);
            result.setItem(0, painted);
        }
    }

    // Closing and shift-clicking

    @Override
    public void removed(Player player) {
        super.removed(player);
        result.removeItemNoUpdate(0);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, DecorBlocks.PAINTING_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != result && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex == RESULT_SLOT || slotIndex == INPUT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            if (slotIndex == RESULT_SLOT) {
                slot.onQuickCraft(stack, original);
            }
        } else if (!Paint.canPaint(stack) || !moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        slot.setChanged();
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        // Taking the result uses up an input item, as taking it by hand does.
        slot.onTake(player, stack);
        broadcastChanges();
        return original;
    }
}
