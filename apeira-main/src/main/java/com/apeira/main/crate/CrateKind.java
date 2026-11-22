package com.apeira.main.crate;

import java.util.Locale;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainBadgerCrate;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * The crates that play the shared crate opening: the same template world, session, camera, animation and HUD, each
 * with its own instance suffix ({@code apeira:<uuid>_badgercrate}, {@code _animalcrate}, {@code _bathroomcrate}, {@code _businesscrate}, {@code _campingcrate}, {@code _funfaircrate}, {@code _gardencrate} ...), loot file
 * ({@code data/apeira/crate_loot/<id>.json}), GeckoLib model ({@code geckolib/models/<id>_crate.geo.json}, the Badger
 * Crate's bones with the crate's own lid and base cubes) and texture ({@code textures/entity/badger_crate/<id>_crate.png}).
 * The animation ({@code badger_crate.animation.json}) and VFX layout are shared.
 */
public enum CrateKind {
    BADGER,
    ANIMAL,
    BATHROOM,
    BUSINESS,
    CAMPING,
    CONSTRUCTION,
    FAST_FOOD,
    COSMETIC,
    FUN_FAIR,
    GARDEN;

    /** {@code badger}, {@code animal}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The instance dimension's path suffix, {@code _badgercrate}, {@code _animalcrate} or {@code _funfaircrate} (no underscores in the id). */
    public String suffix() {
        return "_" + id().replace("_", "") + "crate";
    }

    /** {@code apeira:badger_crate}, {@code apeira:animal_crate}: the GeckoLib model. */
    public Identifier model() {
        return ApeiraIds.id(id() + "_crate");
    }

    /** The recorded atlas with this crate's lid and base in its crate region. */
    public Identifier texture() {
        return ApeiraIds.id("textures/entity/badger_crate/" + id() + "_crate.png");
    }

    public Item item() {
        return switch (this) {
            case BADGER -> MainBadgerCrate.BADGER_CRATE.get();
            case ANIMAL -> MainBadgerCrate.ANIMAL_CRATE.get();
            case BATHROOM -> MainBadgerCrate.BATHROOM_CRATE.get();
            case BUSINESS -> MainBadgerCrate.BUSINESS_CRATE.get();
            case CAMPING -> MainBadgerCrate.CAMPING_CRATE.get();
            case CONSTRUCTION -> MainBadgerCrate.CONSTRUCTION_CRATE.get();
            case FAST_FOOD -> MainBadgerCrate.FAST_FOOD_CRATE.get();
            case COSMETIC -> MainBadgerCrate.COSMETIC_CRATE.get();
            case FUN_FAIR -> MainBadgerCrate.FUN_FAIR_CRATE.get();
            case GARDEN -> MainBadgerCrate.GARDEN_CRATE.get();
        };
    }

    public static CrateKind byId(int ordinal) {
        CrateKind[] kinds = values();
        return kinds[Math.floorMod(ordinal, kinds.length)];
    }
}
