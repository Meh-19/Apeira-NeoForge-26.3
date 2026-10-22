package com.apeira.main.realm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import com.apeira.main.world.MainBiomes;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

/**
 * The five regions a new realm can be made from, in the onboarding screen's order (keys 1 to 5). Each mixes vanilla
 * biomes with Xenon's: lowland biomes cover eroded, flatter ground and highland biomes the rugged, low-erosion ground,
 * so mountains get the mountain biomes while the realm keeps the overworld's terrain.
 */
public enum RealmPreset implements StringRepresentable {
    LUSH("lush",
            List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, MainBiomes.MAPLE_FOREST),
            List.of(Biomes.MEADOW, Biomes.CHERRY_GROVE)),
    ARID("arid",
            List.of(Biomes.DESERT, Biomes.SAVANNA, Biomes.BADLANDS, Biomes.WOODED_BADLANDS),
            List.of(Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.ERODED_BADLANDS)),
    COLD("cold",
            List.of(Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, MainBiomes.COLD_MAPLE_FOREST,
                    MainBiomes.COLD_BIRCH_FOREST),
            List.of(MainBiomes.FOOTHILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST)),
    ARCTIC("arctic",
            List.of(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA),
            List.of(Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS)),
    TROPICAL("tropical",
            List.of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.MANGROVE_SWAMP, Biomes.SWAMP),
            List.of(Biomes.BAMBOO_JUNGLE));

    public static final Codec<RealmPreset> CODEC = StringRepresentable.fromEnum(RealmPreset::values);
    private static final IntFunction<RealmPreset> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, RealmPreset> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    /** Erosion splits lowlands from highlands; vanilla's mountain biomes sit below about -0.35. */
    private static final float HIGHLAND_EROSION = -0.35F;

    private final String name;
    private final List<ResourceKey<Biome>> lowlands;
    private final List<ResourceKey<Biome>> highlands;

    RealmPreset(String name, List<ResourceKey<Biome>> lowlands, List<ResourceKey<Biome>> highlands) {
        this.name = name;
        this.lowlands = lowlands;
        this.highlands = highlands;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The preset after this one, wrapping from tropical back to lush. */
    public RealmPreset next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /**
     * A multi-noise source holding only this preset's biomes. Each group's biomes are spread on a circle of temperature
     * and humidity, so the overworld's climate noise picks between them as it would between vanilla's.
     */
    public BiomeSource biomeSource(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries = new ArrayList<>();
        place(entries, biomes, lowlands, Climate.Parameter.span(HIGHLAND_EROSION, 1.0F));
        place(entries, biomes, highlands, Climate.Parameter.span(-1.0F, HIGHLAND_EROSION));
        return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(entries));
    }

    private static void place(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries, HolderGetter<Biome> biomes,
            List<ResourceKey<Biome>> keys, Climate.Parameter erosion) {
        Climate.Parameter full = Climate.Parameter.span(-1.0F, 1.0F);
        for (int i = 0; i < keys.size(); i++) {
            double angle = Math.PI * 2.0 * i / keys.size();
            Climate.Parameter temperature = Climate.Parameter.point((float) (Math.cos(angle) * 0.5));
            Climate.Parameter humidity = Climate.Parameter.point((float) (Math.sin(angle) * 0.5));
            entries.add(Pair.of(Climate.parameters(temperature, humidity, full, erosion, full, full, 0.0F), biomes.getOrThrow(keys.get(i))));
        }
    }
}
