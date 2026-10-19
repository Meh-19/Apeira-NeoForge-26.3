package com.apeira.main.registry;

import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.entity.CrocodileEntity;
import com.apeira.main.entity.GrizzlyBearEntity;
import com.apeira.main.entity.TurkeyEntity;
import com.apeira.main.entity.WraithEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Xenon's GeckoLib mobs, with Xenon's sizes. Attributes are set in {@link com.apeira.main.entity.MainEntityEvents},
 * renderers in {@link com.apeira.main.client.ApeiraClient}.
 */
public final class MainEntities {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public static final DeferredHolder<EntityType<?>, EntityType<TurkeyEntity>> TURKEY = R.entityType("turkey", TurkeyEntity::new,
            MobCategory.CREATURE, builder -> builder.sized(0.6F, 0.9F));
    public static final DeferredHolder<EntityType<?>, EntityType<GrizzlyBearEntity>> GRIZZLY_BEAR = R.entityType("grizzly_bear",
            GrizzlyBearEntity::new, MobCategory.CREATURE, builder -> builder.sized(1.4F, 1.4F));
    public static final DeferredHolder<EntityType<?>, EntityType<CrocodileEntity>> CROCODILE = R.entityType("crocodile", CrocodileEntity::new,
            MobCategory.CREATURE, builder -> builder.sized(1.4F, 0.6F));
    public static final DeferredHolder<EntityType<?>, EntityType<WraithEntity>> WRAITH = R.entityType("wraith", WraithEntity::new,
            MobCategory.MONSTER, builder -> builder.sized(0.8F, 2.2F).fireImmune());

    private MainEntities() {}

    /** Every mob registered here. */
    public static List<EntityType<?>> all() {
        return List.of(TURKEY.get(), GRIZZLY_BEAR.get(), CROCODILE.get(), WRAITH.get());
    }

    public static void init(ApeiraRegistrar registrar) {}
}
