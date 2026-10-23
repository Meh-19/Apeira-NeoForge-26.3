package com.apeira.main.ruby;

import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainRubies;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The ruby deposit's menu, laid out as Origin Realms' (a 4-row chest, {@code ruby_deposit_menu.png} over it): five
 * deposit slots in the second row, columns 3 to 7, then the player's inventory. The slots take only rubies (not ruby
 * blocks, bricks or pillars), and whatever lands in them is used up at once on the server, one ruby for one ruby of
 * balance, with the ruby gain sound. The rubies come in through vanilla's container click packet, so the client never
 * says how many it deposited, and nothing can be taken back out.
 */
public class ApeiraRubyDepositMenu extends AbstractContainerMenu {
    public static final int DEPOSIT_SLOTS = 5;
    /** Where the first deposit slot is, in the 176x186 frame; the others follow 18 pixels apart. */
    public static final int DEPOSIT_X = 44;
    public static final int DEPOSIT_Y = 36;
    private static final int INVENTORY_START = DEPOSIT_SLOTS;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer deposit = new SimpleContainer(DEPOSIT_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            consume();
        }
    };

    /** The client's menu, opened by the server. */
    public ApeiraRubyDepositMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ApeiraRubyDepositMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MainRubies.RUBY_DEPOSIT_MENU.get(), containerId);
        this.access = access;
        this.player = inventory.player;
        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            addSlot(new Slot(deposit, i, DEPOSIT_X + i * 18, DEPOSIT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isRuby(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 18 + 4 * 18 + 13);
    }

    public static boolean isRuby(ItemStack stack) {
        return stack.is(MainItems.RUBY);
    }

    /**
     * Turns every ruby in the deposit slots into balance (server side). Clearing the slots calls this again, which then
     * finds nothing.
     */
    private void consume() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long rubies = 0;
        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            ItemStack stack = deposit.getItem(i);
            if (isRuby(stack)) {
                rubies += stack.getCount();
            }
        }
        if (rubies > 0) {
            deposit.clearContent();
            Rubies.give(serverPlayer, rubies);
        }
    }

    /** Shift-clicking rubies in the inventory deposits them; nothing else moves. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slotIndex < INVENTORY_START || slotIndex >= INVENTORY_END || !slot.hasItem() || !isRuby(slot.getItem())) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (!moveItemStackTo(stack, 0, DEPOSIT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, MainRubies.RUBY_DEPOSIT.get());
    }
}
