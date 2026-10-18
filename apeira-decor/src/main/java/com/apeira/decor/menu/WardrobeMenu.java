package com.apeira.decor.menu;

import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.decor.cosmetic.Wardrobe;
import com.apeira.decor.registry.DecorAttachments;
import com.apeira.decor.registry.DecorMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The wardrobe: the hat, back and hand cosmetic slots above the player's inventory. Opened with the wardrobe key.
 *
 * <p>On the server the cosmetic slots are the player's {@link Wardrobe} attachment itself, so putting a cosmetic in
 * wears it and taking it out takes it off. CosmoLib's menu instead trusted the client to send the stack it equipped,
 * which let a modified client wear any item it named.
 */
public class WardrobeMenu extends AbstractContainerMenu {
    private static final CosmeticSlot[] SLOTS = CosmeticSlot.values();
    private static final int INVENTORY_START = SLOTS.length;

    private final Player player;

    /** The client's menu, opened by the server. */
    public WardrobeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SLOTS.length));
    }

    /** The server's menu, whose cosmetic slots are the player's wardrobe. */
    public static WardrobeMenu server(int containerId, Inventory inventory) {
        return new WardrobeMenu(containerId, inventory, new WardrobeContainer(inventory.player));
    }

    private WardrobeMenu(int containerId, Inventory inventory, Container wardrobe) {
        super(DecorMenus.WARDROBE.get(), containerId);
        this.player = inventory.player;
        // Positions match CosmoLib's cosmetic_screen.png: hat, back and hand down the left side.
        for (int i = 0; i < SLOTS.length; i++) {
            CosmeticSlot slot = SLOTS[i];
            addSlot(new Slot(wardrobe, i, 17, 10 + 42 * i) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return Wardrobe.fits(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 119);
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = -1;
            for (int i = 0; i < SLOTS.length; i++) {
                if (Wardrobe.fits(SLOTS[i], stack) && !slots.get(i).hasItem()) {
                    target = i;
                }
            }
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) {
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

    /** The player's wardrobe attachment as a three-slot container, in {@link CosmeticSlot} order. */
    private record WardrobeContainer(Player player) implements Container {
        private Wardrobe wardrobe() {
            return player.getData(DecorAttachments.WARDROBE.get());
        }

        @Override
        public int getContainerSize() {
            return SLOTS.length;
        }

        @Override
        public boolean isEmpty() {
            return wardrobe().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return wardrobe().get(SLOTS[slot]);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return count > 0 ? removeItemNoUpdate(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            player.setData(DecorAttachments.WARDROBE.get(), wardrobe().with(SLOTS[slot], stack));
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            // Re-set the wardrobe so a change made to a worn stack in place (by a slot) is synced too.
            player.setData(DecorAttachments.WARDROBE.get(), wardrobe());
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.player && player.isAlive();
        }

        @Override
        public void clearContent() {
            player.setData(DecorAttachments.WARDROBE.get(), Wardrobe.EMPTY);
        }
    }
}
