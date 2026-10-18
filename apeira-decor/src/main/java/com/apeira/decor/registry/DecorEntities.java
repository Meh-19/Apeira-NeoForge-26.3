package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.entity.ApeiraSeatEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Entities of the decor module. Their renderers are registered in {@link com.apeira.decor.client.ApeiraDecorClient}. */
public final class DecorEntities {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** The invisible seat players ride while sitting on a chair. It never moves, so its position is never resent. */
    public static final DeferredHolder<EntityType<?>, EntityType<ApeiraSeatEntity>> SEAT = R.entityType("seat", ApeiraSeatEntity::new,
            MobCategory.MISC, builder -> builder.sized(0.0F, 0.0F).noLootTable().noSummon().fireImmune()
                    .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE));

    private DecorEntities() {}

    public static void init(ApeiraRegistrar registrar) {}
}
