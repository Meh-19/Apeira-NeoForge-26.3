package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.crate.CrateBlock;
import com.apeira.main.Apeira;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The cosmetic crates of the Crates reference mod that have a GeckoLib model, as {@link CrateBlock}s. Each crate
 * {@code <name>} has its GeckoLib model and animations in {@code assets/apeira/geckolib/{models,animations}/crate/},
 * its texture in {@code textures/entity/crate/}, its item model at {@code models/item/<name>.json} (the reference's
 * crate item) and its loot in {@code data/apeira/crate_loot/<name>.json}. Crate items stack to 16, as in CosmoLib.
 */
public final class MainCrates {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredBlock<CrateBlock>> ALL = new ArrayList<>();

    public static final DeferredBlock<CrateBlock> AETHER_CRATE = crate("aether_crate");
    public static final DeferredBlock<CrateBlock> ANIMAL_CRATE = crate("animal_crate");
    public static final DeferredBlock<CrateBlock> AQUATIC_CRATE = crate("aquatic_crate");
    public static final DeferredBlock<CrateBlock> ATLANTIS_CRATE = crate("atlantis_crate");
    public static final DeferredBlock<CrateBlock> BATHROOM_CRATE = crate("bathroom_crate");
    public static final DeferredBlock<CrateBlock> BEACH_CRATE = crate("beach_crate");
    public static final DeferredBlock<CrateBlock> BUSINESS_CRATE = crate("business_crate");
    public static final DeferredBlock<CrateBlock> CAMPING_CRATE = crate("camping_crate");
    public static final DeferredBlock<CrateBlock> CONSTRUCTION_CRATE = crate("construction_crate");
    public static final DeferredBlock<CrateBlock> EASTER_CRATE = crate("easter_crate");
    public static final DeferredBlock<CrateBlock> FAST_FOOD_CRATE = crate("fast_food_crate");
    public static final DeferredBlock<CrateBlock> FESTIVE_CRATE = crate("festive_crate");
    public static final DeferredBlock<CrateBlock> FUN_FAIR_CRATE = crate("fun_fair_crate");
    public static final DeferredBlock<CrateBlock> ZODIAC_CRATE = crate("zodiac_crate");

    private MainCrates() {}

    /** Every crate, in registration order. */
    public static List<DeferredBlock<CrateBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Registers a crate with wooden properties and an item stacking to 16. */
    private static DeferredBlock<CrateBlock> crate(String name) {
        DeferredBlock<CrateBlock> crate = R.blocks().registerBlock(name, CrateBlock::new,
                properties -> properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD).noOcclusion());
        R.items().registerSimpleBlockItem(crate, properties -> properties.stacksTo(16));
        ALL.add(crate);
        return crate;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
