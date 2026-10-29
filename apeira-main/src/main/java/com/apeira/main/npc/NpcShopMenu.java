package com.apeira.main.npc;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.realm.RealmService;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainNpcs;
import com.apeira.main.ruby.Rubies;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * An NPC's shop ({@link NpcShop}), laid out as Origin Realms' market menu (Ren's capture of Jeckyl's market): a 6-row
 * chest whose rows are trades, {@link #PER_PAGE} a page. In each row the price (rubies, as many as the price up to a
 * stack, else a ruby block) is in column 1, the trade arrow in column 2 and the item in column 3; column 4 is the page
 * buttons, the top three rows the previous page and the bottom three the next. Price and item carry the item's name
 * and Origin Realms' lore: "Buy Price: N [ruby]", "Left-click to buy", "Shift-click to buy stack". The arrow and page
 * cells are empty; {@link com.apeira.main.client.npc.NpcShopScreen} draws them and their tooltips.
 *
 * <p>Clicking a trade's price, arrow or item buys it once; Shift-clicking buys a whole stack's worth, as many of the
 * offer as fit in one stack of its item (a Bag o' Soil offer of 12 makes 5 for 60), or as many as the player can afford.
 * The price comes off the player's ruby balance ({@link Rubies#take}) and the items go into their inventory, dropped when
 * it is full. Only the server acts on a click: nothing can be taken out of or put into the shop's slots. The first
 * purchase in a realm schedules Jacko's arrival ({@link JackoArrival#onPurchase}).
 *
 * <p>Data slots tell the client which shop it is (its index in {@link NpcShop#ALL}, for the portrait), the page and page
 * count, and which rows the player can afford (a bit per row: the row's {@code trade_allow} or {@code trade_deny}).
 */
public class NpcShopMenu extends AbstractContainerMenu {
    public static final int ROWS = 6;
    public static final int COLUMNS = 9;
    /** Trades a page: one a row. */
    public static final int PER_PAGE = ROWS;
    public static final int PRICE_COLUMN = 1;
    public static final int ARROW_COLUMN = 2;
    public static final int ITEM_COLUMN = 3;
    public static final int PAGE_COLUMN = 4;
    /** The inventory's top, as in vanilla's 6-row chest. */
    public static final int INVENTORY_Y = 18 + ROWS * 18 + 13;
    /** How far from the NPC the shop stays open, in blocks. */
    public static final double REACH = 8.0;
    public static final int DATA_SHOP = 0;
    public static final int DATA_PAGE = 1;
    public static final int DATA_PAGES = 2;
    public static final int DATA_AFFORDABLE = 3;
    public static final int DATA_COUNT = 4;

    private static final FontDescription KEYS = new FontDescription.Resource(ApeiraIds.id("tooltip_keys"));
    private static final int LABEL = 0xA7AEBA;
    private static final int BUY = 0x85CC16;
    private static final int BUY_STACK = 0xF49E0B;

    private final SimpleContainer display = new SimpleContainer(ROWS * COLUMNS);
    private final @Nullable NpcShop shop;
    private final @Nullable NpcEntity npc;
    private final Player player;
    private final ContainerData data;

    /** The client's menu, opened by the server; its offers come in the synced slots and data. */
    public NpcShopMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null);
    }

    public NpcShopMenu(int containerId, Inventory inventory, @Nullable NpcShop shop, @Nullable NpcEntity npc) {
        super(MainNpcs.NPC_SHOP_MENU.get(), containerId);
        this.shop = shop;
        this.npc = npc;
        this.player = inventory.player;
        this.data = new SimpleContainerData(DATA_COUNT);
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
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
        if (shop != null) {
            data.set(DATA_SHOP, Math.max(0, NpcShop.ALL.indexOf(shop)));
            data.set(DATA_PAGES, Math.max(1, (shop.offers().size() + PER_PAGE - 1) / PER_PAGE));
            showPage(0);
        }
    }

    public @Nullable NpcShop shop() {
        int index = data.get(DATA_SHOP);
        return index >= 0 && index < NpcShop.ALL.size() ? NpcShop.ALL.get(index) : null;
    }

    public int page() {
        return data.get(DATA_PAGE);
    }

    public int pages() {
        return Math.max(1, data.get(DATA_PAGES));
    }

    /** Whether row {@code row} of this page holds a trade. */
    public boolean hasTrade(int row) {
        return !display.getItem(row * COLUMNS + ITEM_COLUMN).isEmpty();
    }

    /** Whether the player can afford row {@code row}'s trade. */
    public boolean canAfford(int row) {
        return (data.get(DATA_AFFORDABLE) >> row & 1) != 0;
    }

    /** The trade item shown in row {@code row}, for the arrow cell's tooltip. */
    public ItemStack tradeItem(int row) {
        return display.getItem(row * COLUMNS + ITEM_COLUMN);
    }

    private void showPage(int page) {
        if (shop == null) {
            return;
        }
        display.clearContent();
        data.set(DATA_PAGE, page);
        for (int row = 0; row < ROWS; row++) {
            int index = page * PER_PAGE + row;
            if (index >= shop.offers().size()) {
                break;
            }
            NpcShop.Offer offer = shop.offers().get(index);
            ItemStack item = offer.result().get();
            ItemLore lore = lore(offer, item);
            ItemStack price = offer.price() <= 64 ? new ItemStack(MainItems.RUBY.get(), (int) Math.max(1, offer.price()))
                    : new ItemStack(MainBlocks.RUBY_BLOCK.get());
            price.set(DataComponents.CUSTOM_NAME, item.getHoverName().copy().withStyle(style -> style.withItalic(false)));
            price.set(DataComponents.LORE, lore);
            item.set(DataComponents.LORE, lore);
            display.setItem(row * COLUMNS + PRICE_COLUMN, price);
            display.setItem(row * COLUMNS + ITEM_COLUMN, item);
        }
        updateAffordable();
    }

    /** Origin Realms' trade lore. */
    private static ItemLore lore(NpcShop.Offer offer, ItemStack item) {
        List<Component> lines = new ArrayList<>();
        lines.add(plain(Component.translatable("container.apeira.npc_shop.price").withColor(LABEL))
                .append(Component.literal(Rubies.format(offer.price()) + " ꢦ").withColor(0xFFFFFF)));
        lines.add(Component.empty());
        lines.add(plain(key(0xE100)).append(" ").append(Component.translatable("container.apeira.npc_shop.buy").withColor(BUY)));
        if (item.getMaxStackSize() / Math.max(1, item.getCount()) > 1) {
            lines.add(plain(key(0xE203)).append(" ").append(key(0xE102)).append(" ")
                    .append(Component.translatable("container.apeira.npc_shop.buy_stack").withColor(BUY_STACK)));
        }
        return new ItemLore(lines);
    }

    private static MutableComponent key(int codepoint) {
        return Component.literal(Character.toString(codepoint)).withStyle(Style.EMPTY.withFont(KEYS).withColor(0xFFFFFF));
    }

    private static MutableComponent plain(MutableComponent first) {
        return Component.empty().withStyle(style -> style.withColor(0xFFFFFF).withItalic(false)).append(first);
    }

    private void updateAffordable() {
        if (shop == null) {
            return;
        }
        long balance = Rubies.get(player);
        int bits = 0;
        for (int row = 0; row < ROWS; row++) {
            int index = page() * PER_PAGE + row;
            if (index < shop.offers().size() && balance >= shop.offers().get(index).price()) {
                bits |= 1 << row;
            }
        }
        data.set(DATA_AFFORDABLE, bits);
    }

    @Override
    public void broadcastChanges() {
        updateAffordable();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < ROWS * COLUMNS) {
            if (shop != null && player instanceof ServerPlayer serverPlayer
                    && (containerInput == ContainerInput.PICKUP || containerInput == ContainerInput.QUICK_MOVE)) {
                int row = slotIndex / COLUMNS;
                int column = slotIndex % COLUMNS;
                int index = page() * PER_PAGE + row;
                if (column == PAGE_COLUMN) {
                    int page = page() + (row < ROWS / 2 ? -1 : 1);
                    if (page >= 0 && page < pages()) {
                        showPage(page);
                    }
                } else if ((column == PRICE_COLUMN || column == ARROW_COLUMN || column == ITEM_COLUMN) && index < shop.offers().size()) {
                    buy(serverPlayer, shop.offers().get(index), containerInput == ContainerInput.QUICK_MOVE);
                }
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
