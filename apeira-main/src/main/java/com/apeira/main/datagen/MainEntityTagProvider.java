package com.apeira.main.datagen;

import java.util.Arrays;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/** Entity type tags: Xenon's capture net lists, with Xenon's mobs. */
public final class MainEntityTagProvider extends EntityTypeTagsProvider {
    public MainEntityTagProvider(ApeiraDataGenerators.Context context) {
        super(context.output(), context.lookup(), context.namespace());
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        add(MainTags.CAPTURABLE,
                EntityTypes.ALLAY, EntityTypes.ARMADILLO, EntityTypes.AXOLOTL, EntityTypes.BAT, EntityTypes.BEE, EntityTypes.CAMEL,
                EntityTypes.CAT, EntityTypes.CHICKEN, EntityTypes.COD, EntityTypes.COW, EntityTypes.DOLPHIN, EntityTypes.DONKEY,
                EntityTypes.FOX, EntityTypes.FROG, EntityTypes.GLOW_SQUID, EntityTypes.GOAT, EntityTypes.HOGLIN, EntityTypes.HORSE,
                EntityTypes.LLAMA, EntityTypes.MOOSHROOM, EntityTypes.MULE, EntityTypes.OCELOT, EntityTypes.PANDA, EntityTypes.PARROT,
                EntityTypes.PIG, EntityTypes.POLAR_BEAR, EntityTypes.PUFFERFISH, EntityTypes.RABBIT, EntityTypes.SALMON, EntityTypes.SHEEP,
                EntityTypes.SKELETON_HORSE, EntityTypes.SNIFFER, EntityTypes.SNOW_GOLEM, EntityTypes.SQUID, EntityTypes.STRIDER,
                EntityTypes.TADPOLE, EntityTypes.TRADER_LLAMA, EntityTypes.TROPICAL_FISH, EntityTypes.TURTLE, EntityTypes.VILLAGER,
                EntityTypes.WANDERING_TRADER, EntityTypes.WOLF, EntityTypes.ZOMBIE_HORSE, EntityTypes.BLAZE, EntityTypes.CAVE_SPIDER,
                EntityTypes.CREEPER, EntityTypes.DROWNED, EntityTypes.ENDERMITE, EntityTypes.EVOKER, EntityTypes.GHAST, EntityTypes.HUSK,
                EntityTypes.ILLUSIONER, EntityTypes.MAGMA_CUBE, EntityTypes.PHANTOM, EntityTypes.PIGLIN, EntityTypes.PILLAGER,
                EntityTypes.SHULKER, EntityTypes.SILVERFISH, EntityTypes.SKELETON, EntityTypes.SLIME, EntityTypes.SPIDER, EntityTypes.STRAY,
                EntityTypes.VEX, EntityTypes.VINDICATOR, EntityTypes.WITCH, EntityTypes.WITHER_SKELETON, EntityTypes.ZOGLIN,
                EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIFIED_PIGLIN);
        add(MainTags.CAPTURABLE_WHEN_WEAKENED,
                EntityTypes.ELDER_GUARDIAN, EntityTypes.ENDERMAN, EntityTypes.GUARDIAN, EntityTypes.IRON_GOLEM, EntityTypes.PIGLIN_BRUTE,
                EntityTypes.RAVAGER, EntityTypes.WARDEN, EntityTypes.WITHER, EntityTypes.ENDER_DRAGON, EntityTypes.BREEZE,
                 MainEntities.GRIZZLY_BEAR.get());
    }

    @SuppressWarnings("unchecked")
    private void add(TagKey<EntityType<?>> tag, EntityType<?>... types) {
        tag(tag).add(Arrays.stream(types).map(type -> type.builtInRegistryHolder().key()).toArray(ResourceKey[]::new));
    }
}
