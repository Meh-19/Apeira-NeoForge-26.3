package com.apeira.main.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every realm dimension the server has made, saved with the overworld ({@code data/apeira/realms.dat}): the realms
 * players own, by owner, and the pre-generated pool waiting to be claimed ({@link RealmPool}).
 */
public final class RealmData extends SavedData {
    private static final Codec<RealmData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Realm.CODEC).fieldOf("owners").forGetter(data -> data.owners),
            Realm.CODEC.listOf().fieldOf("pool").forGetter(data -> data.pool)).apply(instance, RealmData::new));
    public static final SavedDataType<RealmData> TYPE = new SavedDataType<>(ApeiraIds.id("realms"), RealmData::new, CODEC);

    private final Map<UUID, Realm> owners = new HashMap<>();
    private final List<Realm> pool = new ArrayList<>();

    private RealmData() {}

    private RealmData(Map<UUID, Realm> owners, List<Realm> pool) {
        this.owners.putAll(owners);
        this.pool.addAll(pool);
    }

    public static RealmData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Realm> realmOf(UUID owner) {
        return Optional.ofNullable(owners.get(owner));
    }

    public Iterable<Realm> owned() {
        return List.copyOf(owners.values());
    }

    public List<Realm> pool() {
        return List.copyOf(pool);
    }

    public long poolCount(RealmPreset preset) {
        return pool.stream().filter(realm -> realm.preset() == preset).count();
    }

    public void addToPool(Realm realm) {
        pool.add(realm);
        setDirty();
    }

    /** Swaps a pool realm for {@code updated}, the same dimension (for example now generated). */
    public void updatePool(Realm updated) {
        pool.replaceAll(realm -> realm.dimension().equals(updated.dimension()) ? updated : realm);
        setDirty();
    }

    /** Takes a pool realm of {@code preset}, a generated one first, and gives it to {@code owner}. */
    public Optional<Realm> claim(UUID owner, RealmPreset preset) {
        Optional<Realm> claimed = pool.stream().filter(realm -> realm.preset() == preset)
                .sorted((a, b) -> Boolean.compare(b.generated(), a.generated())).findFirst();
        claimed.ifPresent(realm -> {
            pool.remove(realm);
            owners.put(owner, realm);
            setDirty();
        });
        return claimed;
    }

    public void own(UUID owner, Realm realm) {
        owners.put(owner, realm);
        setDirty();
    }

    /**
     * One realm dimension: its level key's id, its preset, the seed its terrain was made from, whether the area inside
     * its border has been generated, and where players arrive.
     */
    public record Realm(Identifier dimension, RealmPreset preset, long seed, boolean generated, BlockPos spawn) {
        public static final Codec<Realm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Realm::dimension),
                RealmPreset.CODEC.fieldOf("preset").forGetter(Realm::preset),
                Codec.LONG.fieldOf("seed").forGetter(Realm::seed),
                Codec.BOOL.fieldOf("generated").forGetter(Realm::generated),
                BlockPos.CODEC.fieldOf("spawn").forGetter(Realm::spawn)).apply(instance, Realm::new));

        public ResourceKey<Level> levelKey() {
            return ResourceKey.create(Registries.DIMENSION, dimension);
        }

        public Realm withGenerated(BlockPos spawn) {
            return new Realm(dimension, preset, seed, true, spawn);
        }
    }
}
