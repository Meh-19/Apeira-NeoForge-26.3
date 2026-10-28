package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Realm NPCs ({@link NpcEntity}), drawn from Bedrock models by {@code client/npc/BedrockNpcRenderer} (registered in
 * {@link com.apeira.main.client.ApeiraClient}). Their size is their collision box, which the anchor cursor uses too.
 */
public final class MainNpcs {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /** Origin Realms' balloon conductor, about three blocks tall with his hat; placed by a Lush realm's starter balloon. */
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> CONDUCTOR_CARLO = R.entityType("conductor_carlo", NpcEntity::new,
            MobCategory.MISC, builder -> builder.sized(1.0F, 3.0F).fireImmune().clientTrackingRange(10));

    private MainNpcs() {}

    public static void init(ApeiraRegistrar registrar) {}
}
