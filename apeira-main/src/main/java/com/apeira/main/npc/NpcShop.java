package com.apeira.main.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.main.farming.CropQuality;
import com.apeira.main.farming.FarmClock;
import com.apeira.main.farming.FarmCrop;
import com.apeira.main.registry.MainFarming;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An NPC's shop: its offers in shop order (which may change with the server's state, such as the farming day), shown
 * and traded in {@link NpcShopMenu}, and its portrait, Origin Realms' {@code custom_ui/markets/<portrait>_portrait.png}
 * as {@code textures/gui/npc_shop/portrait/<portrait>.png}.
 */
public record NpcShop(String portrait, Function<MinecraftServer, List<Offer>> offerList) {
    /**
     * A trade of {@code result} for {@code price} rubies (the {@code apeira:rubies} balance, {@link com.apeira.main.ruby.Rubies}):
     * the player buys it, or, for a {@link #sell} offer, sells one item matching {@code takes} (shown as {@code result}).
     */
    public record Offer(long price, Supplier<ItemStack> result, @Nullable Predicate<ItemStack> takes) {
        public static Offer buy(long price, Supplier<ItemStack> result) {
            return new Offer(price, result, null);
        }

        public static Offer sell(long price, Supplier<ItemStack> result, Predicate<ItemStack> takes) {
            return new Offer(price, result, takes);
        }

        /** Whether the player sells to the NPC: gives an item matching {@link #takes} and gets {@link #price} rubies. */
        public boolean sell() {
            return takes != null;
        }
    }

    /** The offers now, on {@code server}. */
    public List<Offer> offers(MinecraftServer server) {
        return offerList.apply(server);
    }

    /** Jeckyl's farming accessories, in Origin Realms' order and at its prices. */
    public static final NpcShop JECKYL = new NpcShop("jeckyl", server -> List.of(
            Offer.buy(16, () -> new ItemStack(MainFarming.WOODEN_WATERING_CAN.get())),
            Offer.buy(4, () -> new ItemStack(MainFarming.BAG_O_SOIL.get(), 12)),
            Offer.buy(32, () -> new ItemStack(MainFarming.PLANTER_BOX.get())),
            Offer.buy(6, () -> new ItemStack(MainFarming.FERTILIZER_MULTIPLIER.get())),
            Offer.buy(255, () -> new ItemStack(MainFarming.WOODEN_SPRINKLER.get())),
            Offer.buy(1000, () -> new ItemStack(MainFarming.IRON_SPRINKLER.get()))));

    /**
     * What Jacko pays for each crop, by {@link FarmCrop} ordinal: 1, 2 and 3 stars, then golden, from Ren's price sheet
     * ("Jacko Buys").
     */
    private static final long[][] SELL_PRICES = {
            {4, 12, 17, 255}, // apple
            {7, 12, 17, 255}, // banana
            {7, 12, 17, 255}, // mango
            {6, 10, 14, 210}, // corn
            {6, 10, 14, 210}, // pineapple
            {5, 8, 11, 165}, // eggplant
            {4, 6, 8, 80}, // lettuce
            {4, 6, 8, 80}}; // chilli
    /** Jacko's seed prices, one seed each, by {@link FarmCrop} ordinal, from the price sheet ("Jacko Sells"). */
    private static final long[] SEED_PRICES = {4, 4, 4, 3, 3, 2, 1, 1};

    /**
     * Jacko's market, as Ren's captures of it show (two pages): first he buys the crop of the day (one crop a farming
     * day, {@link FarmClock#day} through {@link FarmCrop}'s order) at 1, 2 and 3 stars and golden, then sells one seed of
     * that crop and of every other crop in alphabetical order, at the prices of Ren's price sheet ({@link #SELL_PRICES},
     * {@link #SEED_PRICES}).
     */
    public static final NpcShop JACKO = new NpcShop("jacko", server -> {
        FarmCrop[] crops = FarmCrop.values();
        FarmCrop today = crops[(int) Math.floorMod(FarmClock.get(server).day(), (long) crops.length)];
        List<Offer> offers = new ArrayList<>();
        for (int stars = 1; stars <= CropQuality.MAX_STARS; stars++) {
            int rating = stars;
            offers.add(Offer.sell(sellPrice(today, stars - 1), () -> rated(today, rating),
                    stack -> stack.is(today.produce(false)) && CropQuality.starsOf(stack) == rating));
        }
        Item golden = today.produce(true);
        offers.add(Offer.sell(sellPrice(today, CropQuality.MAX_STARS), () -> new ItemStack(golden), stack -> stack.is(golden)));
        offers.add(seed(today));
        List<FarmCrop> others = new ArrayList<>(List.of(crops));
        others.remove(today);
        others.sort((a, b) -> a.id().compareTo(b.id()));
        others.forEach(crop -> offers.add(seed(crop)));
        return offers;
    });

    /** Every shop, by the index the menu syncs to the client. */
    public static final List<NpcShop> ALL = List.of(JECKYL, JACKO);

    private static long sellPrice(FarmCrop crop, int tier) {
        return SELL_PRICES[crop.ordinal()][tier];
    }

    private static ItemStack rated(FarmCrop crop, int stars) {
        ItemStack stack = new ItemStack(crop.produce(false));
        stack.set(MainFarming.CROP_QUALITY.get(), stars);
        return stack;
    }

    private static Offer seed(FarmCrop crop) {
        return Offer.buy(SEED_PRICES[crop.ordinal()], () -> new ItemStack(MainFarming.seed(crop)));
    }
}
