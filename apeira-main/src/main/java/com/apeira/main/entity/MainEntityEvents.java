package com.apeira.main.entity;

import com.apeira.main.registry.MainEntities;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Mod bus events for Xenon's mobs: their attributes and natural spawn rules. */
public final class MainEntityEvents {
    private MainEntityEvents() {}

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MainEntities.TURKEY.get(), TurkeyEntity.createAttributes().build());
        event.put(MainEntities.GRIZZLY_BEAR.get(), GrizzlyBearEntity.createAttributes().build());
        event.put(MainEntities.CROCODILE.get(), CrocodileEntity.createAttributes().build());
        event.put(MainEntities.WRAITH.get(), WraithEntity.createAttributes().build());
    }

    /**
     * Where natural spawns may put Xenon's mobs (their biomes list them, {@code world/MainBiomes}): turkeys and grizzly
     * bears on the ground like other animals, wraiths on the ground below y 0 in the dark ({@link WraithEntity#canSpawnInDepths}).
     * Xenon gave the bear no rule, which let it spawn anywhere in the foothills, mid-air or under water included. The
     * crocodile has no natural spawns, as in Xenon.
     */
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(MainEntities.TURKEY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MainEntities.GRIZZLY_BEAR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MainEntities.WRAITH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WraithEntity::canSpawnInDepths, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
