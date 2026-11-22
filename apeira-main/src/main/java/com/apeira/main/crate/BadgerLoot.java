package com.apeira.main.crate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.furniture.AnimatedFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * A crate's rewards, {@code data/apeira/crate_loot/<kind>.json} ({@link CrateKind#id}): item ids for each
 * {@link BadgerTier}. An opening rolls three rewards, each its own tier by the shared odds ({@link BadgerTier#weight}:
 * common 55, rare 27, epic 13, legendary 5), then an item of that tier with equal weight, so each item's chance is its
 * tier's split evenly ({@link #chance}; two legendaries are 2.5% each). An empty tier falls back to the next lower one,
 * then to the next higher. The three are different items while the lists allow it.
 *
 * <p>Every crate's lists are filtered the same way ({@link #resolved}): a furniture variant (a wall, ceiling or water
 * piece, which drops its base piece) counts as its base piece, so a list naming both rolls it once, and an item listed
 * twice counts once. A crate whose file sets {@code "exclude_animated": true} (the Camping Crate) never rolls an
 * animated furniture piece ({@link AnimatedFurnitureBlock}).
 *
 * <p>A crate's tool skins ({@code "tool_skins"}) are one sub-pool of the common tier: each skin weighs as one common item
 * (the Medieval Crate's six commons and Knight Shield Skin are 55 / 7 = 7.86% each, as captured), and once one of the
 * three rewards is a skin the other two never are, so an opening gives at most one tool skin.
 */
public record BadgerLoot(Map<BadgerTier, List<Identifier>> items, List<Identifier> toolSkins, boolean excludeAnimated) {
    public static final BadgerLoot EMPTY = new BadgerLoot(Map.of(), List.of(), false);
    private static final Codec<List<Identifier>> LIST = Identifier.CODEC.listOf();
    public static final Codec<BadgerLoot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LIST.optionalFieldOf("common").forGetter(loot -> Optional.of(loot.of(BadgerTier.COMMON))),
            LIST.optionalFieldOf("rare").forGetter(loot -> Optional.of(loot.of(BadgerTier.RARE))),
            LIST.optionalFieldOf("epic").forGetter(loot -> Optional.of(loot.of(BadgerTier.EPIC))),
            LIST.optionalFieldOf("legendary").forGetter(loot -> Optional.of(loot.of(BadgerTier.LEGENDARY))),
            LIST.optionalFieldOf("tool_skins", List.of()).forGetter(BadgerLoot::toolSkins),
            Codec.BOOL.optionalFieldOf("exclude_animated", false).forGetter(BadgerLoot::excludeAnimated))
            .apply(instance, (common, rare, epic, legendary, toolSkins, excludeAnimated) -> {
                Map<BadgerTier, List<Identifier>> map = new EnumMap<>(BadgerTier.class);
                map.put(BadgerTier.COMMON, common.orElse(List.of()));
                map.put(BadgerTier.RARE, rare.orElse(List.of()));
                map.put(BadgerTier.EPIC, epic.orElse(List.of()));
                map.put(BadgerTier.LEGENDARY, legendary.orElse(List.of()));
                return new BadgerLoot(map, toolSkins, excludeAnimated);
            }));
    private static volatile Map<CrateKind, BadgerLoot> current = Map.of();

    public List<Identifier> of(BadgerTier tier) {
        return items.getOrDefault(tier, List.of());
    }

    public static BadgerLoot get(CrateKind kind) {
        return current.getOrDefault(kind, EMPTY);
    }

    /**
     * One roll's chance of each item of {@code tier}, in percent: the tier's share split evenly among its items (before
     * the empty-tier fallback and the no-repeats rule of the second and third rewards).
     */
    public double chance(BadgerTier tier) {
        int size = resolved(tier).size() + (tier == BadgerTier.COMMON ? skins().size() : 0);
        return size == 0 ? 0.0 : (double) tier.weight() / size;
    }

    /** One rolled reward. */
    public record Reward(ItemStack stack, BadgerTier tier) {}

    /** Three rewards; an empty crate file rolls three common stone, so an opening never breaks. */
    public List<Reward> roll(RandomSource random) {
        List<Reward> rewards = new ArrayList<>(3);
        List<Item> taken = new ArrayList<>();
        List<Item> skins = skins();
        for (int i = 0; i < 3; i++) {
            BadgerTier tier = rollTier(random);
            boolean skinOpen = taken.stream().noneMatch(skins::contains);
            BadgerTier available = available(tier, skinOpen ? skins : List.of());
            Item item = pick(available, random, taken, skinOpen ? skins : List.of());
            taken.add(item);
            rewards.add(new Reward(new ItemStack(item), available));
        }
        return rewards;
    }

    private static BadgerTier rollTier(RandomSource random) {
        int roll = random.nextInt(100);
        for (BadgerTier tier : BadgerTier.values()) {
            roll -= tier.weight();
            if (roll < 0) {
                return tier;
            }
        }
        return BadgerTier.COMMON;
    }

    private BadgerTier available(BadgerTier tier, List<Item> skins) {
        for (int i = tier.ordinal(); i >= 0; i--) {
            if (!pool(BadgerTier.values()[i], skins).isEmpty()) {
                return BadgerTier.values()[i];
            }
        }
        for (int i = tier.ordinal() + 1; i < BadgerTier.values().length; i++) {
            if (!pool(BadgerTier.values()[i], skins).isEmpty()) {
                return BadgerTier.values()[i];
            }
        }
        return BadgerTier.COMMON;
    }

    private Item pick(BadgerTier tier, RandomSource random, List<Item> taken, List<Item> skins) {
        List<Item> pool = pool(tier, skins);
        if (pool.isEmpty()) {
            return net.minecraft.world.item.Items.STONE;
        }
        List<Item> fresh = pool.stream().filter(item -> !taken.contains(item)).toList();
        List<Item> from = fresh.isEmpty() ? pool : fresh;
        return from.get(random.nextInt(from.size()));
    }

    /** The tier's items, the common tier with the still-open tool skins, each weighing as one item. */
    private List<Item> pool(BadgerTier tier, List<Item> skins) {
        List<Item> items = resolved(tier);
        if (tier != BadgerTier.COMMON || skins.isEmpty()) {
            return items;
        }
        List<Item> pool = new ArrayList<>(items);
        skins.stream().filter(skin -> !pool.contains(skin)).forEach(pool::add);
        return pool;
    }

    /** The crate's tool skins, each once and not also in a tier list. */
    List<Item> skins() {
        List<Item> listed = new ArrayList<>();
        for (BadgerTier tier : BadgerTier.values()) {
            listed.addAll(resolved(tier));
        }
        return toolSkins.stream().map(id -> BuiltInRegistries.ITEM.getOptional(id)).flatMap(Optional::stream)
                .filter(item -> !listed.contains(item)).distinct().toList();
    }

    /** The tier's items: variants as their base piece, each once, animated furniture left out when the crate excludes it. */
    List<Item> resolved(BadgerTier tier) {
        return of(tier).stream().map(id -> BuiltInRegistries.ITEM.getOptional(id)).flatMap(Optional::stream)
                .map(BadgerLoot::base)
                .filter(item -> !excludeAnimated || !(item instanceof BlockItem block && block.getBlock() instanceof AnimatedFurnitureBlock))
                .distinct().toList();
    }

    /** A furniture variant's base piece's item, else the item itself. */
    private static Item base(Item item) {
        if (item instanceof BlockItem block && block.getBlock() instanceof FurnitureBlock furniture && furniture.dropBlock() != furniture) {
            Item base = furniture.dropBlock().asItem();
            return base == net.minecraft.world.item.Items.AIR ? item : base;
        }
        return item;
    }

    /** Loads {@code data/apeira/crate_loot/<kind>.json} for every {@link CrateKind}. */
    public static final class Manager extends SimpleJsonResourceReloadListener<BadgerLoot> {
        private Manager() {
            super(CODEC, FileToIdConverter.json("crate_loot"));
        }

        public static void addListener(AddServerReloadListenersEvent event) {
            event.addListener(ApeiraIds.id("crate_loot"), new Manager());
        }

        @Override
        protected void apply(Map<Identifier, BadgerLoot> prepared, ResourceManager manager, ProfilerFiller profiler) {
            Map<CrateKind, BadgerLoot> loaded = new EnumMap<>(CrateKind.class);
            for (CrateKind kind : CrateKind.values()) {
                loaded.put(kind, prepared.getOrDefault(ApeiraIds.id(kind.id()), EMPTY));
            }
            current = loaded;
        }
    }
}
