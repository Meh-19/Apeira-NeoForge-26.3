package com.apeira.main.npc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.main.realm.RealmService;
import com.apeira.main.registry.MainNpcs;
import com.apeira.main.ruby.Rubies;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * An NPC's shop ({@link NpcShop}), laid out as a 3-row chest: its offers in the middle row, in order and centred (an even
 * number leaves the centre cell empty), then the player's inventory. Each offer shows its item with its price and how to
 * buy it in its lore, which the server writes, so the client needs nothing but the slots vanilla syncs.
 *
 * <p>Clicking an offer buys it once; Shift-clicking buys a whole stack's worth, as many of the offer as fit in one stack
 * of its item (a Bag o' Soil offer of 12 makes 5 for 60), or as many as the player can afford. The price comes off the
 * player's ruby balance ({@link Rubies#take}) and the items go into their inventory, dropped when it is full. Only the
 * server acts on a click: nothing can be taken out of or put into the offer slots. The first purchase in a realm
 * schedules Jacko's arrival ({@link JackoArrival#onPurchase}).
 */
public class NpcShopMenu extends AbstractContainerMenu {
    public static final int ROWS = 3;
    public static final int COLUMNS = 9;
    /** How far from the NPC the shop stays open, in blocks. */
    public static final double REACH = 8.0;
    private static final int OFFER_ROW = 1;

    private final SimpleContainer display = new SimpleContainer(ROWS * COLUMNS);
    private final @Nullable NpcShop shop;
    private final @Nullable NpcEntity npc;
    /** For each display slot, the offer it shows, or -1. */
    private final int[] offerAt = new int[ROWS * COLUMNS];

    /** The client's menu, opened by the server; its offers come in the synced slots. */
    public NpcShopMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null);
    }

    public NpcShopMenu(int containerId, Inventory inventory, @Nullable NpcShop shop, @Nullable NpcEntity npc) {
        super(MainNpcs.NPC_SHOP_MENU.get(), containerId);
        this.shop = shop;
        this.npc = npc;
        Arrays.fill(offerAt, -1);
        if (shop != null) {
            List<Integer> cells = cells(shop.offers().size());
            for (int i = 0; i < cells.size(); i++) {
                offerAt[cells.get(i)] = i;
                display.setItem(cells.get(i), shown(shop.offers().get(i)));
            }
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(display, row * COLUMNS + column, 8 + column * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }
        addStandardInventorySlots(inventory, 8, 18 + ROWS * 18 + 13);
    }

    /** The middle row's cells for {@code count} offers: centred, skipping the centre cell for an even count. */
    static List<Integer> cells(int count) {
        List<Integer> cells = new ArrayList<>();
        int shown = Math.min(count, COLUMNS);
        boolean gap = shown % 2 == 0 && shown < COLUMNS;
        int start = (COLUMNS - shown - (gap ? 1 : 0)) / 2;
        for (int column = start; cells.size() < shown; column++) {
            if (gap && column == COLUMNS / 2) {
                continue;
            }
            cells.add(OFFER_ROW * COLUMNS + column);
        }
        return cells;
    }

    /** The offer's item with its price and the click lines in its lore. */
    private static ItemStack shown(NpcShop.Offer offer) {
        ItemStack stack = offer.result().get();
        int perStack = Math.max(1, stack.getMaxStackSize() / stack.getCount());
        List<Component> lore = new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        lore.add(Component.empty());
        lore.add(Component.translatable("container.apeira.npc_shop.price", Rubies.format(offer.price()))
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));
        lore.add(Component.translatable("container.apeira.npc_shop.buy").withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
        if (perStack > 1) {
            lore.add(Component.translatable("container.apeira.npc_shop.buy_stack", perStack * stack.getCount(), Rubies.format(perStack * offer.price()))
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < ROWS * COLUMNS) {
            int offer = offerAt[slotIndex];
            if (offer >= 0 && shop != null && player instanceof ServerPlayer serverPlayer
                    && (containerInput == ContainerInput.PICKUP || containerInput == ContainerInput.QUICK_MOVE)) {
                buy(serverPlayer, shop.offers().get(offer), containerInput == ContainerInput.QUICK_MOVE);
            }
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /** Buys {@code offer} once, or a stack's worth when {@code stack}, as far as the balance goes. */
    private void buy(ServerPlayer player, NpcShop.Offer offer, boolean stack) {
        ItemStack result = offer.result().get();
        int wanted = stack ? Math.max(1, result.getMaxStackSize() / result.getCount()) : 1;
        long affordable = offer.price() <= 0 ? wanted : Rubies.get(player) / offer.price();
        int times = (int) Math.min(wanted, affordable);
        if (times <= 0 || !Rubies.take(player, offer.price() * times)) {
            RealmService.fail(player, "npc.shop.too_poor", Rubies.format(offer.price()));
            return;
        }
        int total = result.getCount() * times;
        while (total > 0) {
            int count = Math.min(total, result.getMaxStackSize());
            player.getInventory().placeItemBackInInventory(result.copyWithCount(count), Prediction.SERVER_ONLY);
            total -= count;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        if (npc != null) {
            JackoArrival.onPurchase(player, npc);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != display && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public boolean stillValid(Player player) {
        return npc == null || npc.isAlive() && npc.level() == player.level() && npc.distanceToSqr(player) <= REACH * REACH;
    }
}
