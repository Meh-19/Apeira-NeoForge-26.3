package com.apeira.main.registry;

import java.util.List;

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
    /** The Bathroom Crate: the pack's bathroom_crate_full with the Badger Crate's displays, and its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> BATHROOM_CRATE = R.items().registerItem("bathroom_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.BATHROOM), properties -> properties.stacksTo(16));
    /** The Business Crate: the pack's business_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> BUSINESS_CRATE = R.items().registerItem("business_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.BUSINESS), properties -> properties.stacksTo(16));
    /** The Camping Crate: the pack's camping_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> CAMPING_CRATE = R.items().registerItem("camping_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.CAMPING), properties -> properties.stacksTo(16));
    /** The Construction Crate: the pack's construction_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> CONSTRUCTION_CRATE = R.items().registerItem("construction_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.CONSTRUCTION), properties -> properties.stacksTo(16));
    /** The Fast Food Crate: the pack's fast_food_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> FAST_FOOD_CRATE = R.items().registerItem("fast_food_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.FAST_FOOD), properties -> properties.stacksTo(16));

    /** Big enough to always be in view: the model reaches a few blocks around and above its origin. */
    public static final DeferredHolder<EntityType<?>, EntityType<BadgerCrateShowEntity>> SHOW = R.entityType("badger_crate_show",
            BadgerCrateShowEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).noSave().noSummon()
                    .clientTrackingRange(8).updateInterval(1).fireImmune());

    /** Every crate item, in the Crates tab's order. */
    public static List<DeferredItem<BadgerCrateItem>> crates() {
        return List.of(BADGER_CRATE, ANIMAL_CRATE, BATHROOM_CRATE, BUSINESS_CRATE, CAMPING_CRATE, CONSTRUCTION_CRATE, FAST_FOOD_CRATE);
    }

    private MainBadgerCrate() {}

    public static void init(ApeiraRegistrar registrar) {}
}
