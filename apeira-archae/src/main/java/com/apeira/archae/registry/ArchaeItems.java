package com.apeira.archae.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apeira.archae.ApeiraArchae;
import com.apeira.archae.item.FragmentItem;
import com.apeira.core.registry.ApeiraRegistrar;

import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Argon's 106 fragments ({@code net.meh.argon.item.ModItems}), with Argon's ids. Argon's {@code ETHEREAL_SHERD} has the
 * id {@code ethereal_shard}, which is kept. The assembly table puts them together into {@link ArchaeArtifacts}.
 */
public final class ArchaeItems {
    private static final List<DeferredItem<FragmentItem>> ALL = new ArrayList<>();

    public static final DeferredItem<FragmentItem> BLADE_SHARD = fragment("blade_shard");
    public static final DeferredItem<FragmentItem> BUNDLED_STICKS = fragment("bundled_sticks");
    public static final DeferredItem<FragmentItem> ARCANE_GOLD = fragment("arcane_gold");
    public static final DeferredItem<FragmentItem> GLOOMY_LUMBER = fragment("gloomy_lumber");
    public static final DeferredItem<FragmentItem> CHOPPED_LUMBER = fragment("chopped_lumber");
    public static final DeferredItem<FragmentItem> ARCHAIC_GOLD = fragment("archaic_gold");
    public static final DeferredItem<FragmentItem> ANCIENT_OIL = fragment("ancient_oil");
    public static final DeferredItem<FragmentItem> WORN_LEATHER = fragment("worn_leather");
    public static final DeferredItem<FragmentItem> FOREGONE_FRAGMENT = fragment("foregone_fragment");
    public static final DeferredItem<FragmentItem> PRIMAL_SILK = fragment("primal_silk");
    public static final DeferredItem<FragmentItem> TEXTILE_CLOTH = fragment("textile_cloth");
    public static final DeferredItem<FragmentItem> LOST_JEWELS = fragment("lost_jewels");
    public static final DeferredItem<FragmentItem> FOSSILIZED_BONES = fragment("fossilized_bones");
    public static final DeferredItem<FragmentItem> DECREPIT_STONES = fragment("decrepit_stones");
    public static final DeferredItem<FragmentItem> VOLATILE_BREW = fragment("volatile_brew");
    public static final DeferredItem<FragmentItem> BUSTED_WHEEL = fragment("busted_wheel");
    public static final DeferredItem<FragmentItem> CERAMIC_SHERD = fragment("ceramic_sherd");
    public static final DeferredItem<FragmentItem> AGED_FABRIC = fragment("aged_fabric");
    public static final DeferredItem<FragmentItem> PRIMITIVE_PAPER = fragment("primitive_paper");
    public static final DeferredItem<FragmentItem> SPLINTERED_WOOD = fragment("splintered_wood");
    public static final DeferredItem<FragmentItem> OBSCURE_BONES = fragment("obscure_bones");
    public static final DeferredItem<FragmentItem> OLD_BRICKS = fragment("old_bricks");
    public static final DeferredItem<FragmentItem> TORN_PAGES = fragment("torn_pages");
    public static final DeferredItem<FragmentItem> ILL_FRAGMENT = fragment("ill_fragment");
    public static final DeferredItem<FragmentItem> OMINOUS_SLATE = fragment("ominous_slate");
    public static final DeferredItem<FragmentItem> RAVAGED_HIDE = fragment("ravaged_hide");
    public static final DeferredItem<FragmentItem> SILVER_SHARD = fragment("silver_shard");
    public static final DeferredItem<FragmentItem> SINISTEEL_PIECE = fragment("sinisteel_piece");
    public static final DeferredItem<FragmentItem> DEEPSLATE_PIECE = fragment("deepslate_piece");
    public static final DeferredItem<FragmentItem> ECHO_CLUSTER = fragment("echo_cluster");
    public static final DeferredItem<FragmentItem> FORGOTTEN_REMNANT = fragment("forgotten_remnant");
    public static final DeferredItem<FragmentItem> HOLLOW_BONES = fragment("hollow_bones");
    public static final DeferredItem<FragmentItem> SILENT_SILVER = fragment("silent_silver");
    public static final DeferredItem<FragmentItem> BRISK_STRAW = fragment("brisk_straw");
    public static final DeferredItem<FragmentItem> BROKEN_HULL = fragment("broken_hull");
    public static final DeferredItem<FragmentItem> CRYSTALLINE_SHARDS = fragment("crystalline_shards");
    public static final DeferredItem<FragmentItem> FRIGID_ALLOY = fragment("frigid_alloy");
    public static final DeferredItem<FragmentItem> FROSTBITE_PIECES = fragment("frostbite_pieces");
    public static final DeferredItem<FragmentItem> FROSTED_FABRIC = fragment("frosted_fabric");
    public static final DeferredItem<FragmentItem> FROZEN_FRAGMENT = fragment("frozen_fragment");
    public static final DeferredItem<FragmentItem> KYANITE_SLATE = fragment("kyanite_slate");
    public static final DeferredItem<FragmentItem> PRIMITIVE_FUR = fragment("primitive_fur");
    public static final DeferredItem<FragmentItem> SEVERED_HORN = fragment("severed_horn");
    public static final DeferredItem<FragmentItem> DOLOMITE_SHERD = fragment("dolomite_sherd");
    public static final DeferredItem<FragmentItem> SUNKEN_STEEL = fragment("sunken_steel");
    public static final DeferredItem<FragmentItem> CORRODED_BOARD = fragment("corroded_board");
    public static final DeferredItem<FragmentItem> FRACTURED_PRISMARINE = fragment("fractured_prismarine");
    public static final DeferredItem<FragmentItem> BEAST_HORN = fragment("beast_horn");
    public static final DeferredItem<FragmentItem> FORTUNATE_GOLD = fragment("fortunate_gold");
    public static final DeferredItem<FragmentItem> SIMMERING_GUNPOWDER = fragment("simmering_gunpowder");
    public static final DeferredItem<FragmentItem> TERRACOTTA_SHERD = fragment("terracotta_sherd");
    public static final DeferredItem<FragmentItem> WORN_TWINE = fragment("worn_twine");
    public static final DeferredItem<FragmentItem> CURSED_BONES = fragment("cursed_bones");
    public static final DeferredItem<FragmentItem> CURSED_FABRIC = fragment("cursed_fabric");
    public static final DeferredItem<FragmentItem> MALEVOLENT_MINERAL = fragment("malevolent_mineral");
    public static final DeferredItem<FragmentItem> POSSESSED_HORN = fragment("possessed_horn");
    public static final DeferredItem<FragmentItem> PUTRID_SEEDS = fragment("putrid_seeds");
    public static final DeferredItem<FragmentItem> RUSTED_SCRAP = fragment("rusted_scrap");
    public static final DeferredItem<FragmentItem> ANCESTOR_TOOTH = fragment("ancestor_tooth");
    public static final DeferredItem<FragmentItem> ANCESTRAL_QUILT = fragment("ancestral_quilt");
    public static final DeferredItem<FragmentItem> ARTISANT_REMNANT = fragment("artisant_remnant");
    public static final DeferredItem<FragmentItem> BAOBAB_CARVING = fragment("baobab_carving");
    public static final DeferredItem<FragmentItem> STRATA_PIECE = fragment("strata_piece");
    public static final DeferredItem<FragmentItem> TATTERED_SPEARHEAD = fragment("tattered_spearhead");
    public static final DeferredItem<FragmentItem> TRIBAL_TIMBER = fragment("tribal_timber");
    public static final DeferredItem<FragmentItem> ABNORMAL_SLAB = fragment("abnormal_slab");
    public static final DeferredItem<FragmentItem> DESERTED_GOLD = fragment("deserted_gold");
    public static final DeferredItem<FragmentItem> DRIED_FABRIC = fragment("dried_fabric");
    public static final DeferredItem<FragmentItem> DUSTY_SHERD = fragment("dusty_sherd");
    public static final DeferredItem<FragmentItem> LIMESTONE_PIECE = fragment("limestone_piece");
    public static final DeferredItem<FragmentItem> OASAL_SEEDS = fragment("oasal_seeds");
    public static final DeferredItem<FragmentItem> SOVEREIGN_TREASURE = fragment("sovereign_treasure");
    public static final DeferredItem<FragmentItem> SPIRITUAL_SAND = fragment("spiritual_sand");
    public static final DeferredItem<FragmentItem> NETHERITE_PIECE = fragment("netherite_piece");
    public static final DeferredItem<FragmentItem> SOOTY_DEBRIS = fragment("sooty_debris");
    public static final DeferredItem<FragmentItem> BASALT_SLAG = fragment("basalt_slag");
    public static final DeferredItem<FragmentItem> ASHEN_SCRAP = fragment("ashen_scrap");
    public static final DeferredItem<FragmentItem> GILDED_METAL = fragment("gilded_metal");
    public static final DeferredItem<FragmentItem> PIGGISH_GOLD = fragment("piggish_gold");
    public static final DeferredItem<FragmentItem> NOXIOUS_SPORES = fragment("noxious_spores");
    public static final DeferredItem<FragmentItem> SEARING_SPORES = fragment("searing_spores");
    public static final DeferredItem<FragmentItem> SWINISH_BONES = fragment("swinish_bones");
    public static final DeferredItem<FragmentItem> BOORISH_RIBS = fragment("boorish_ribs");
    public static final DeferredItem<FragmentItem> HOGLIN_TUSK = fragment("hoglin_tusk");
    public static final DeferredItem<FragmentItem> HOG_HIDE = fragment("hog_hide");
    public static final DeferredItem<FragmentItem> PARCHED_PELT = fragment("parched_pelt");
    public static final DeferredItem<FragmentItem> MOLTEN_METAL = fragment("molten_metal");
    public static final DeferredItem<FragmentItem> INFERNAL_FABRIC = fragment("infernal_fabric");
    public static final DeferredItem<FragmentItem> NETHERRACK_SHARD = fragment("netherrack_shard");
    public static final DeferredItem<FragmentItem> FORSAKEN_SLATE = fragment("forsaken_slate");
    public static final DeferredItem<FragmentItem> WHISPERING_WAX = fragment("whispering_wax");
    public static final DeferredItem<FragmentItem> REVERENT_RUBBLE = fragment("reverent_rubble");
    public static final DeferredItem<FragmentItem> ETHEREAL_SHARD = fragment("ethereal_shard");
    public static final DeferredItem<FragmentItem> GLISTENING_CURIO = fragment("glistening_curio");
    public static final DeferredItem<FragmentItem> CLOUD_CLOTH = fragment("cloud_cloth");
    public static final DeferredItem<FragmentItem> TWINING_ROOTS = fragment("twining_roots");
    public static final DeferredItem<FragmentItem> MYSTICAL_MINERAL = fragment("mystical_mineral");
    public static final DeferredItem<FragmentItem> SWIFT_CERAMIC = fragment("swift_ceramic");
    public static final DeferredItem<FragmentItem> HOLY_STONES = fragment("holy_stones");
    public static final DeferredItem<FragmentItem> DIVINE_DEW = fragment("divine_dew");
    public static final DeferredItem<FragmentItem> AETHERIC_GOLD = fragment("aetheric_gold");
    public static final DeferredItem<FragmentItem> ICESTONE_PEBBLES = fragment("icestone_pebbles");
    public static final DeferredItem<FragmentItem> FATED_FRAGMENTS = fragment("fated_fragments");
    public static final DeferredItem<FragmentItem> GRIMY_GOLD = fragment("grimy_gold");
    public static final DeferredItem<FragmentItem> MOSSY_REMNANTS = fragment("mossy_remnants");
    public static final DeferredItem<FragmentItem> SWASHED_RAGS = fragment("swashed_rags");

    private ArchaeItems() {}

    private static DeferredItem<FragmentItem> fragment(String name) {
        DeferredItem<FragmentItem> item = ApeiraArchae.REGISTRAR.items().registerItem(name, FragmentItem::new);
        ALL.add(item);
        return item;
    }

    /** Every fragment, in Argon's order. */
    public static List<DeferredItem<FragmentItem>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
