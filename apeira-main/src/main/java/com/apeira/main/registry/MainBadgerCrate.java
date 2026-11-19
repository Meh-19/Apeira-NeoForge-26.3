package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.crate.BadgerCrateItem;
import com.apeira.main.crate.BadgerCrateShowEntity;
import com.apeira.main.crate.CrateKind;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Badger Crate: the item ({@code apeira:badger_crate}, held 2.5 seconds to open, never placed) and the entity that
 * plays its opening in the opener's own instance ({@code apeira:badger_crate_show}). See {@code com.apeira.main.crate}.
 */
public final class MainBadgerCrate {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public static final DeferredItem<BadgerCrateItem> BADGER_CRATE = R.items().registerItem("badger_crate", properties -> new BadgerCrateItem(properties, CrateKind.BADGER),
            properties -> properties.stacksTo(16));
    /** The Animal Crate: the Badger Crate's opening with its own lid, base and loot ({@code crate_loot/animal.json}). */
    public static final DeferredItem<BadgerCrateItem> ANIMAL_CRATE = R.items().registerItem("animal_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.ANIMAL), properties -> properties.stacksTo(16));

    /** Big enough to always be in view: the model reaches a few blocks around and above its origin. */
    public static final DeferredHolder<EntityType<?>, EntityType<BadgerCrateShowEntity>> SHOW = R.entityType("badger_crate_show",
            BadgerCrateShowEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).noSave().noSummon()
                    .clientTrackingRange(8).updateInterval(1).fireImmune());

    private MainBadgerCrate() {}

    public static void init(ApeiraRegistrar registrar) {}
}
