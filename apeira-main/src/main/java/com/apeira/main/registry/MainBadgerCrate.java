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
    /** The Cosmetic Crate: the pack's cosmetic_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> COSMETIC_CRATE = R.items().registerItem("cosmetic_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.COSMETIC), properties -> properties.stacksTo(16));
    /** The Fun Fair Crate: the pack's fun_fair_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> FUN_FAIR_CRATE = R.items().registerItem("fun_fair_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.FUN_FAIR), properties -> properties.stacksTo(16));
    /** The Garden Crate: the pack's garden_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> GARDEN_CRATE = R.items().registerItem("garden_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.GARDEN), properties -> properties.stacksTo(16));
    /** The Kitchen Crate: the pack's kitchen_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> KITCHEN_CRATE = R.items().registerItem("kitchen_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.KITCHEN), properties -> properties.stacksTo(16));
    /** The Medieval Crate: the pack's medieval_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> MEDIEVAL_CRATE = R.items().registerItem("medieval_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.MEDIEVAL), properties -> properties.stacksTo(16));
    /** The Music Crate: the pack's music_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> MUSIC_CRATE = R.items().registerItem("music_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.MUSIC), properties -> properties.stacksTo(16));
    /** The Space Crate: the pack's space_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> SPACE_CRATE = R.items().registerItem("space_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.SPACE), properties -> properties.stacksTo(16));
    /** The Mining Crate: the pack's mining_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> MINING_CRATE = R.items().registerItem("mining_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.MINING), properties -> properties.stacksTo(16));
    /** The Western Crate: the pack's western_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> WESTERN_CRATE = R.items().registerItem("western_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.WESTERN), properties -> properties.stacksTo(16));
    /** The Wizardry Crate: the pack's wizardry_crate_full with the Badger Crate's displays, its lid and base on the shared geometry. */
    public static final DeferredItem<BadgerCrateItem> WIZARDRY_CRATE = R.items().registerItem("wizardry_crate",
            properties -> new BadgerCrateItem(properties, CrateKind.WIZARDRY), properties -> properties.stacksTo(16));

    /** Big enough to always be in view: the model reaches a few blocks around and above its origin. */
    public static final DeferredHolder<EntityType<?>, EntityType<BadgerCrateShowEntity>> SHOW = R.entityType("badger_crate_show",
            BadgerCrateShowEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).noSave().noSummon()
                    .clientTrackingRange(8).updateInterval(1).fireImmune());

    /** Every crate item, in the Crates tab's order. */
    public static List<DeferredItem<BadgerCrateItem>> crates() {
        return List.of(BADGER_CRATE, ANIMAL_CRATE, BATHROOM_CRATE, BUSINESS_CRATE, CAMPING_CRATE, CONSTRUCTION_CRATE, FAST_FOOD_CRATE, COSMETIC_CRATE, FUN_FAIR_CRATE, GARDEN_CRATE,
                KITCHEN_CRATE, MEDIEVAL_CRATE, MUSIC_CRATE, SPACE_CRATE, WESTERN_CRATE, WIZARDRY_CRATE, MINING_CRATE);
    }

    private MainBadgerCrate() {}

    public static void init(ApeiraRegistrar registrar) {}
}
