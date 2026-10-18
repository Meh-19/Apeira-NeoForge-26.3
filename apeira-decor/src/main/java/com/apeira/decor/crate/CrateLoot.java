package com.apeira.decor.crate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticRarity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a crate can roll, sorted by rarity: {@code data/<namespace>/crate_loot/<crate>.json}, keyed by the crate block's
 * id and loaded by {@link CrateLootManager}. Each tier is a list of item ids; ids of items that are not registered are
 * skipped when rolling.
 *
 * <pre>{@code
 * { "common": ["apeira:spartan_helmet"], "rare": ["apeira:cloud_wings"], "epic": [], "legendary": [], "limited": [] }
 * }</pre>
 *
 * <p>A roll is three items. The first is always rare or better; the others use the full weights. A rarity is picked
 * with CosmoLib's weights (60, 25, 10, 4 and 1 percent), with the opener's {@linkplain CrateLuck luck} moved from
 * common to rare (common never drops below 5). An empty tier falls back to the nearest lower tier (never below rare for
 * the first item), then the nearest higher one.
 */
public record CrateLoot(Map<CosmeticRarity, List<Identifier>> tiers) {
    public static final float COMMON_WEIGHT = 60.0F;
    public static final float RARE_WEIGHT = 25.0F;
    public static final float EPIC_WEIGHT = 10.0F;
    public static final float LEGENDARY_WEIGHT = 4.0F;
    public static final float LIMITED_WEIGHT = 1.0F;
    /** Luck never lowers the common weight below this. */
    public static final float MIN_COMMON_WEIGHT = 5.0F;
    public static final int ITEMS_PER_ROLL = 3;

    public static final CrateLoot EMPTY = new CrateLoot(Map.of());

    private static final Codec<List<Identifier>> TIER = Identifier.CODEC.listOf();
    public static final Codec<CrateLoot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TIER.optionalFieldOf("common", List.of()).forGetter(loot -> loot.tier(CosmeticRarity.COMMON)),
            TIER.optionalFieldOf("rare", List.of()).forGetter(loot -> loot.tier(CosmeticRarity.RARE)),
            TIER.optionalFieldOf("epic", List.of()).forGetter(loot -> loot.tier(CosmeticRarity.EPIC)),
            TIER.optionalFieldOf("legendary", List.of()).forGetter(loot -> loot.tier(CosmeticRarity.LEGENDARY)),
            TIER.optionalFieldOf("limited", List.of()).forGetter(loot -> loot.tier(CosmeticRarity.LIMITED)))
            .apply(instance, CrateLoot::of));

    public CrateLoot {
        tiers = Map.copyOf(tiers);
    }

    private static CrateLoot of(List<Identifier> common, List<Identifier> rare, List<Identifier> epic, List<Identifier> legendary,
            List<Identifier> limited) {
        Map<CosmeticRarity, List<Identifier>> tiers = new EnumMap<>(CosmeticRarity.class);
        tiers.put(CosmeticRarity.COMMON, List.copyOf(common));
        tiers.put(CosmeticRarity.RARE, List.copyOf(rare));
        tiers.put(CosmeticRarity.EPIC, List.copyOf(epic));
        tiers.put(CosmeticRarity.LEGENDARY, List.copyOf(legendary));
        tiers.put(CosmeticRarity.LIMITED, List.copyOf(limited));
        return new CrateLoot(tiers);
    }

    /** The item ids of one tier. */
    public List<Identifier> tier(CosmeticRarity rarity) {
        return tiers.getOrDefault(rarity, List.of());
    }

    /** Whether no tier lists a registered item, so the crate cannot roll anything. */
    public boolean isEmpty() {
        for (CosmeticRarity rarity : CosmeticRarity.values()) {
            if (!items(rarity).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Rolls {@value #ITEMS_PER_ROLL} items, the first rare or better. Returns empty stacks if the crate has no items. */
    public List<ItemStack> roll(RandomSource random, float luck) {
        List<ItemStack> result = new ArrayList<>(ITEMS_PER_ROLL);
        result.add(pick(random, rollRareOrHigher(random, luck), true));
        for (int i = 1; i < ITEMS_PER_ROLL; i++) {
            result.add(pick(random, rollRarity(random, luck), false));
        }
        return result;
    }

    /** A cosmetic's own rarity, or the tier that lists the item, or common. */
    public CosmeticRarity rarityOf(ItemStack stack) {
        if (stack.getItem() instanceof CosmeticItem cosmetic) {
            return cosmetic.rarity();
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (CosmeticRarity rarity : CosmeticRarity.values()) {
            if (tier(rarity).contains(id)) {
                return rarity;
            }
        }
        return CosmeticRarity.COMMON;
    }

    /** Picks a rarity with the luck-adjusted weights. */
    public static CosmeticRarity rollRarity(RandomSource random, float luck) {
        float common = Math.max(MIN_COMMON_WEIGHT, COMMON_WEIGHT - luck);
        float rare = RARE_WEIGHT + (COMMON_WEIGHT - common);
        return pick(random, common, rare);
    }

    /** Picks a rarity other than common, with the remaining luck-adjusted weights. */
    public static CosmeticRarity rollRareOrHigher(RandomSource random, float luck) {
        float common = Math.max(MIN_COMMON_WEIGHT, COMMON_WEIGHT - luck);
        float rare = RARE_WEIGHT + (COMMON_WEIGHT - common);
        return pick(random, 0.0F, rare);
    }

    private static CosmeticRarity pick(RandomSource random, float common, float rare) {
        float roll = random.nextFloat() * (common + rare + EPIC_WEIGHT + LEGENDARY_WEIGHT + LIMITED_WEIGHT);
        if ((roll -= common) < 0) {
            return CosmeticRarity.COMMON;
        }
        if ((roll -= rare) < 0) {
            return CosmeticRarity.RARE;
        }
        if ((roll -= EPIC_WEIGHT) < 0) {
            return CosmeticRarity.EPIC;
        }
        if (roll - LEGENDARY_WEIGHT < 0) {
            return CosmeticRarity.LEGENDARY;
        }
        return CosmeticRarity.LIMITED;
    }

    private ItemStack pick(RandomSource random, CosmeticRarity rarity, boolean rareOrHigher) {
        List<Item> pool = pool(rarity, rareOrHigher);
        return pool.isEmpty() ? ItemStack.EMPTY : new ItemStack(pool.get(random.nextInt(pool.size())));
    }

    /** The rolled tier's items, or the nearest lower tier's (not below rare when required), or the nearest higher one's. */
    private List<Item> pool(CosmeticRarity rarity, boolean rareOrHigher) {
        CosmeticRarity[] order = CosmeticRarity.values();
        int lowest = rareOrHigher ? CosmeticRarity.RARE.ordinal() : 0;
        for (int i = rarity.ordinal(); i >= lowest; i--) {
            List<Item> items = items(order[i]);
            if (!items.isEmpty()) {
                return items;
            }
        }
        for (int i = rarity.ordinal() + 1; i < order.length; i++) {
            List<Item> items = items(order[i]);
            if (!items.isEmpty()) {
                return items;
            }
        }
        // Only common items left, though the slot asked for rare or better.
        return rareOrHigher ? pool(CosmeticRarity.COMMON, false) : List.of();
    }

    private List<Item> items(CosmeticRarity rarity) {
        List<Item> items = new ArrayList<>();
        for (Identifier id : tier(rarity)) {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
            item.ifPresent(items::add);
        }
        return items;
    }
}
