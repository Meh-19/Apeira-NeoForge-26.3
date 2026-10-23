package com.apeira.main.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/**
 * Every player's homes, saved with the overworld ({@code data/apeira/homes.dat}): up to {@link #MAX_HOMES} named
 * waypoints each, in the order they were set, which is the order the Homes page shows them in.
 */
public final class HomeData extends SavedData {
    /** The Homes page's grid: three rows of five. */
    public static final int MAX_HOMES = 15;
    public static final int MAX_NAME_LENGTH = 16;
    private static final Codec<HomeData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Home.CODEC.listOf())
            .xmap(HomeData::new, data -> data.homes);
    public static final SavedDataType<HomeData> TYPE = new SavedDataType<>(ApeiraIds.id("homes"), HomeData::new, CODEC);

    private final Map<UUID, List<Home>> homes = new HashMap<>();

    private HomeData() {}

    private HomeData(Map<UUID, List<Home>> homes) {
        homes.forEach((player, list) -> this.homes.put(player, new ArrayList<>(list)));
    }

    public static HomeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Home> homesOf(UUID player) {
        return List.copyOf(homes.getOrDefault(player, List.of()));
    }

    public int indexOf(UUID player, String name) {
        List<Home> list = homes.getOrDefault(player, List.of());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /** Adds {@code home}, or moves the home of the same name there. */
    void set(UUID player, Home home) {
        List<Home> list = homes.computeIfAbsent(player, uuid -> new ArrayList<>());
        int index = indexOf(player, home.name());
        if (index >= 0) {
            list.set(index, home);
        } else {
            list.add(home);
        }
        setDirty();
    }

    void remove(UUID player, int index) {
        homes.get(player).remove(index);
        setDirty();
    }

    void rename(UUID player, int index, String name) {
        List<Home> list = homes.get(player);
        list.set(index, list.get(index).withName(name));
        setDirty();
    }

    /**
     * One home: its name, the dimension and spot it is at, the way the player faced, and when it was set (epoch
     * milliseconds).
     */
    public record Home(String name, Identifier dimension, Vec3 position, float yRot, float xRot, long created) {
        public static final Codec<Home> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Home::name),
                Identifier.CODEC.fieldOf("dimension").forGetter(Home::dimension),
                Vec3.CODEC.fieldOf("position").forGetter(Home::position),
                Codec.FLOAT.fieldOf("y_rot").forGetter(Home::yRot),
                Codec.FLOAT.fieldOf("x_rot").forGetter(Home::xRot),
                Codec.LONG.fieldOf("created").forGetter(Home::created)).apply(instance, Home::new));

        public ResourceKey<Level> levelKey() {
            return ResourceKey.create(Registries.DIMENSION, dimension);
        }

        Home withName(String name) {
            return new Home(name, dimension, position, yRot, xRot, created);
        }
    }
}
