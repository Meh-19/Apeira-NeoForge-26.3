package com.apeira.main.npc;

import java.util.List;
import java.util.function.Supplier;

import com.apeira.main.registry.MainFarming;

import net.minecraft.world.item.ItemStack;

/**
 * An NPC's shop: its offers in shop order, each an item stack for a price in rubies (the {@code apeira:rubies} balance,
 * {@link com.apeira.main.ruby.Rubies}), shown and bought in {@link NpcShopMenu}, and its portrait, Origin Realms'
 * {@code custom_ui/markets/<portrait>_portrait.png} as {@code textures/gui/npc_shop/portrait/<portrait>.png}.
 */
public record NpcShop(String portrait, List<Offer> offers) {
    /** {@code result} for {@code price} rubies. */
    public record Offer(long price, Supplier<ItemStack> result) {}

    /** Jeckyl's farming accessories, in Origin Realms' order and at its prices. */
    public static final NpcShop JECKYL = new NpcShop("jeckyl", List.of(
            new Offer(16, () -> new ItemStack(MainFarming.WOODEN_WATERING_CAN.get())),
            new Offer(4, () -> new ItemStack(MainFarming.BAG_O_SOIL.get(), 12)),
            new Offer(32, () -> new ItemStack(MainFarming.PLANTER_BOX.get())),
            new Offer(6, () -> new ItemStack(MainFarming.FERTILIZER_MULTIPLIER.get())),
            new Offer(255, () -> new ItemStack(MainFarming.WOODEN_SPRINKLER.get())),
            new Offer(1000, () -> new ItemStack(MainFarming.IRON_SPRINKLER.get()))));

    /**
     * Jacko's seeds, on the same market menu with his portrait (Origin Realms' {@code jacko_portrait.png}). No capture
     * of his market was taken, so the offers and prices are Apeira's: the quicker a crop, the cheaper its seeds.
     */
    public static final NpcShop JACKO = new NpcShop("jacko", List.of(
            new Offer(4, () -> new ItemStack(MainFarming.LETTUCE_SEEDS.get(), 4)),
            new Offer(4, () -> new ItemStack(MainFarming.CHILLI_SEEDS.get(), 4)),
            new Offer(6, () -> new ItemStack(MainFarming.EGG_PLANT_SEEDS.get(), 4)),
            new Offer(8, () -> new ItemStack(MainFarming.CORN_SEEDS.get(), 4)),
            new Offer(8, () -> new ItemStack(MainFarming.PINEAPPLE_SEEDS.get(), 4)),
            new Offer(12, () -> new ItemStack(MainFarming.APPLE_SEEDS.get(), 4)),
            new Offer(12, () -> new ItemStack(MainFarming.BANANA_SEEDS.get(), 4)),
            new Offer(12, () -> new ItemStack(MainFarming.MANGO_PIT.get(), 4))));

    /** Every shop, by the index the menu syncs to the client. */
    public static final List<NpcShop> ALL = List.of(JECKYL, JACKO);
}
