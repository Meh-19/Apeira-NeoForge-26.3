package com.apeira.main.farming;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainFarming;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A planter box's menu, laid out as Origin Realms' (a 5-row chest with {@code menu_container.png} over it): the seed,
 * soil and fertilizer slots down the third column, then the player's inventory. The crop, its stage and what it has
 * (soil, water, fertilizer) come through {@link PlanterBoxBlockEntity#data}, which the screen draws as the crop's
 * picture and the lit icons.
 */
public class PlanterBoxMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 44;
    public static final int SLOT_Y = 36;
    private static final int INVENTORY_START = PlanterBoxBlockEntity.SLOTS;
    private static final int INVENTORY_END = INVENTORY_START + 36;
    private static final String[] OUTLINES = {"farming/seeds_outline", "farming/soil_outline", "farming/fertiliser_outline"};

    private final Container stock;
    private final ContainerData data;

    /** The client's menu, opened by the server. */
    public PlanterBoxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(PlanterBoxBlockEntity.SLOTS) {
            @Override
            public int getMaxStackSize() {
                return PlanterBoxBlockEntity.MAX_STACK;
            }
        }, new SimpleContainerData(PlanterBoxBlockEntity.DATA_COUNT));
    }

    public PlanterBoxMenu(int containerId, Inventory inventory, Container stock, ContainerData data) {
        super(MainFarming.PLANTER_BOX_MENU.get(), containerId);
        checkContainerSize(stock, PlanterBoxBlockEntity.SLOTS);
        checkContainerDataCount(data, PlanterBoxBlockEntity.DATA_COUNT);
        this.stock = stock;
        this.data = data;
        stock.startOpen(inventory.player);
        for (int i = 0; i < PlanterBoxBlockEntity.SLOTS; i++) {
            int slot = i;
            addSlot(new Slot(stock, i, SLOT_X, SLOT_Y + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return PlanterBoxBlockEntity.stockSlot(stack) == slot
                            && (slot != PlanterBoxBlockEntity.FERTILIZER || !hasItem() || ItemStack.isSameItem(getItem(), stack));
                }
            }).setBackground(ApeiraIds.id(OUTLINES[i]));
        }
        addStandardInventorySlots(inventory, 8, 18 + 5 * 18 + 13);
        addDataSlots(data);
    }

    public @Nullable FarmCrop crop() {
        return FarmCrop.byOrdinal(data.get(PlanterBoxBlockEntity.DATA_CROP));
    }

    public int stage() {
        return data.get(PlanterBoxBlockEntity.DATA_STAGE);
    }

    public boolean has(int flag) {
        return (data.get(PlanterBoxBlockEntity.DATA_FLAGS) & flag) != 0;
    }

    public @Nullable Fertilizer fertilizer() {
        return Fertilizer.byOrdinal(data.get(PlanterBoxBlockEntity.DATA_FERTILIZER));
    }

    /** Shift-clicking moves stock to the inventory and seeds, soil and fertilizer to their slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = PlanterBoxBlockEntity.stockSlot(stack);
            if (target < 0 || !slots.get(target).mayPlace(stack) || !moveItemStackTo(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        stock.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return stock.stillValid(player);
    }
}
