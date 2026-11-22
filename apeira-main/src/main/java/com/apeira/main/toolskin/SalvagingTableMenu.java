package com.apeira.main.toolskin;

import com.apeira.decor.toolskin.ToolSkins;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainToolSkins;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The salvaging table's menu, laid out as Origin Realms' "Salvage" menu (Ren's captures {@code 20261122-1116*}/{@code 1117*}:
 * a 3-row chest with the server's {@code menus/salvaging/background.png} over it). A skinned tool goes in the input slot (chest
 * slot 11, at 44, 36); the lower output (chest slot 24, at 116, 54) then shows its skin, and taking the skin from there takes
 * it off the tool, which stays in the input without it, every other component as it was. The upper output (116, 18) is left
 * empty, as in every capture. Closing the menu gives the input back.
 */
public class SalvagingTableMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 44;
    public static final int INPUT_Y = 36;
    public static final int SKIN_X = 116;
    public static final int SKIN_Y = 54;
    private static final int INPUT = 0;
    private static final int SKIN = 1;
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final SimpleContainer skin = new SimpleContainer(1);
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            skin.setItem(0, ToolSkins.token(getItem(0)));
        }
    };

    /** The client's menu, opened by the server. */
    public SalvagingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SalvagingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MainToolSkins.SALVAGING_TABLE_MENU.get(), containerId);
        this.access = access;
        addSlot(new Slot(input, 0, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ToolSkins.isSkinned(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(skin, 0, SKIN_X, SKIN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                input.setItem(0, ToolSkins.strip(input.getItem(0)));
                super.onTake(player, stack);
            }
        });
        addStandardInventorySlots(inventory, 8, 18 + 3 * 18 + 13);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex == SKIN) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, original);
            return original;
        }
        if (slotIndex == INPUT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!ToolSkins.isSkinned(stack) || slots.get(INPUT).hasItem() || !moveItemStackTo(stack, INPUT, INPUT + 1, false)) {
            return ItemStack.EMPTY;
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
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, MainBuildingBlocks.SALVAGING_TABLE.get());
    }
}
