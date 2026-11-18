package com.apeira.main.crate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * The Badger Crate's rewards, {@code data/apeira/badger_crate/loot.json}: item ids for each {@link BadgerTier}. An
 * opening rolls three rewards, each its own tier by {@link BadgerTier#weight}; an empty tier falls back to the next lower
 * one, then to the next higher. The three are different items while the lists allow it.
 */
public record BadgerLoot(Map<BadgerTier, List<Identifier>> items) {
    public static final BadgerLoot EMPTY = new BadgerLoot(Map.of());
    private static final Codec<List<Identifier>> LIST = Identifier.CODEC.listOf();
    public static final Codec<BadgerLoot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LIST.optionalFieldOf("common").forGetter(loot -> Optional.of(loot.of(BadgerTier.COMMON))),
            LIST.optionalFieldOf("rare").forGetter(loot -> Optional.of(loot.of(BadgerTier.RARE))),
            LIST.optionalFieldOf("epic").forGetter(loot -> Optional.of(loot.of(BadgerTier.EPIC))),
            LIST.optionalFieldOf("legendary").forGetter(loot -> Optional.of(loot.of(BadgerTier.LEGENDARY))))
            .apply(instance, (common, rare, epic, legendary) -> {
                Map<BadgerTier, List<Identifier>> map = new EnumMap<>(BadgerTier.class);
                map.put(BadgerTier.COMMON, common.orElse(List.of()));
                map.put(BadgerTier.RARE, rare.orElse(List.of()));
                map.put(BadgerTier.EPIC, epic.orElse(List.of()));
                map.put(BadgerTier.LEGENDARY, legendary.orElse(List.of()));
                return new BadgerLoot(map);
            }));
    public static final Identifier FILE = ApeiraIds.id("loot");

    private static volatile BadgerLoot current = EMPTY;

    public List<Identifier> of(BadgerTier tier) {
        return items.getOrDefault(tier, List.of());
    }

    public static BadgerLoot get() {
        return current;
    }

    /** One rolled reward. */
    public record Reward(ItemStack stack, BadgerTier tier) {}

    /** Three rewards; an empty crate file rolls three common stone, so an opening never breaks. */
    public List<Reward> roll(RandomSource random) {
        List<Reward> rewards = new ArrayList<>(3);
        List<Item> taken = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BadgerTier tier = rollTier(random);
            BadgerTier available = available(tier);
            Item item = pick(available, random, taken);
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

    private BadgerTier available(BadgerTier tier) {
        for (int i = tier.ordinal(); i >= 0; i--) {
            if (!resolved(BadgerTier.values()[i]).isEmpty()) {
                return BadgerTier.values()[i];
            }
        }
        for (int i = tier.ordinal() + 1; i < BadgerTier.values().length; i++) {
            if (!resolved(BadgerTier.values()[i]).isEmpty()) {
                return BadgerTier.values()[i];
            }
        }
        return BadgerTier.COMMON;
    }

    private Item pick(BadgerTier tier, RandomSource random, List<Item> taken) {
        List<Item> pool = resolved(tier);
        if (pool.isEmpty()) {
            return net.minecraft.world.item.Items.STONE;
        }
        List<Item> fresh = pool.stream().filter(item -> !taken.contains(item)).toList();
        List<Item> from = fresh.isEmpty() ? pool : fresh;
        return from.get(random.nextInt(from.size()));
    }

    private List<Item> resolved(BadgerTier tier) {
        return of(tier).stream().map(id -> BuiltInRegistries.ITEM.getOptional(id)).flatMap(Optional::stream).toList();
    }

    /** Loads {@code data/<namespace>/badger_crate/loot.json}. */
    public static final class Manager extends SimpleJsonResourceReloadListener<BadgerLoot> {
        private Manager() {
            super(CODEC, FileToIdConverter.json("badger_crate"));
        }

        public static void addListener(AddServerReloadListenersEvent event) {
            event.addListener(ApeiraIds.id("badger_crate_loot"), new Manager());
        }

        @Override
        protected void apply(Map<Identifier, BadgerLoot> prepared, ResourceManager manager, ProfilerFiller profiler) {
            current = prepared.getOrDefault(FILE, EMPTY);
        }
    }
}
