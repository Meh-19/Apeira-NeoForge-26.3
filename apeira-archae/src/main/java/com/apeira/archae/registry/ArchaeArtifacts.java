package com.apeira.archae.registry;

import static com.apeira.archae.registry.ArtifactProperties.CANDLE;
import static com.apeira.archae.registry.ArtifactProperties.DECORATION;
import static com.apeira.archae.registry.ArtifactProperties.DECORATION_COLLISION;
import static com.apeira.archae.registry.ArtifactProperties.DECORATION_LUMINANCE;
import static com.apeira.archae.registry.ArtifactProperties.DEFAULT;
import static com.apeira.archae.registry.ArtifactProperties.DEFAULT_LUMINANCE;
import static com.apeira.archae.registry.ArtifactProperties.DEFAULT_NO_COLLISION;
import static com.apeira.archae.registry.ArtifactProperties.HARD_WOODEN;
import static com.apeira.archae.registry.ArtifactProperties.LANTERN;
import static com.apeira.archae.registry.ArtifactProperties.POT;
import static com.apeira.archae.registry.ArtifactProperties.POT_COLLISION;
import static com.apeira.archae.registry.ArtifactProperties.SILK;
import static com.apeira.archae.registry.ArtifactProperties.TORCH;
import static com.apeira.archae.registry.ArtifactProperties.WOODEN;
import static com.apeira.archae.registry.ArtifactProperties.WOODEN_NO_COLLISION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.ApeiraArchae;
import com.apeira.decor.furniture.AnimatedFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock.Form;
import com.apeira.decor.furniture.CeilingFurnitureBlock;
import com.apeira.decor.furniture.DisplayMode;
import com.apeira.decor.furniture.FacingFurnitureBlock;
import com.apeira.decor.furniture.SeatFurnitureBlock;
import com.apeira.decor.furniture.WaterloggedFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlockItem;
import com.apeira.decor.furniture.FurnitureFootprints;
import com.apeira.archae.item.FragmentItem;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.cosmetic.CosmeticItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.component.ItemLore;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Argon's 386 artifacts ({@code net.meh.argon.block.ModBlocks}), with Argon's ids, properties, fragments and costs, in
 * Argon's order and regions. Each has an {@link FurnitureBlockItem} of the same name.
 *
 * <p>{@link #artifact} registers an artifact the assembly table makes from {@code cost} of its fragment;
 * {@link #variant} registers a wall, ceiling or water variant, which has no recipe, is left out of the creative tab and
 * drops its base artifact. Factories refer to other artifacts through {@code ArchaeArtifacts.X}, a supplier, because
 * the variants are declared after the artifacts that place them.
 */
public final class ArchaeArtifacts {
    private static final List<Artifact> ALL = new ArrayList<>();

    static {
        // Artifacts are decor furniture; their footprints are this module's table, needed as they register.
        FurnitureFootprints.addTable("/apeira_archae/artifact_footprints.txt");
    }
    /** Argon's gold "Archeology Exclusive" line under every artifact's name. */
    private static final ItemLore EXCLUSIVE_LORE = new ItemLore(List.of(Component.translatable("tooltip.apeira.exclusive.archaeology")
            .withStyle(Style.EMPTY.withColor(CosmeticItem.EXCLUSIVE_COLOR).withItalic(false))));

    // General / Underground
    public static final DeferredBlock<FurnitureBlock> LOG_BARRICADE = artifact("log_barricade", WOODEN,
            ArchaeItems.CHOPPED_LUMBER, 4, p -> new FurnitureBlock(p, false, Form.TWO_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> SMALL_LOG_BARRICADE = artifact("small_log_barricade", WOODEN,
            ArchaeItems.CHOPPED_LUMBER, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> LUMBER_STACK = artifact("lumber_stack", WOODEN,
            ArchaeItems.CHOPPED_LUMBER, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> COIN_PILE = artifact("coin_pile", DECORATION,
            ArchaeItems.ARCHAIC_GOLD, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> GOLDEN_CHALICE = artifact("golden_chalice", DECORATION,
            ArchaeItems.ARCHAIC_GOLD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> ABANDONED_BLADE = artifact("abandoned_blade", DEFAULT,
            ArchaeItems.BLADE_SHARD, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> WOODEN_STAKES = artifact("wooden_stakes", WOODEN,
            ArchaeItems.BUNDLED_STICKS, 3, p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<SeatFurnitureBlock> RUGGED_STAND = artifact("rugged_stand", WOODEN,
            ArchaeItems.BUNDLED_STICKS, 2, p -> new SeatFurnitureBlock(p, false, 16));
    public static final DeferredBlock<SeatFurnitureBlock> RUGGED_STOOL = artifact("rugged_stool", WOODEN,
            ArchaeItems.BUNDLED_STICKS, 2, p -> new SeatFurnitureBlock(p, false, 11));
    public static final DeferredBlock<FacingFurnitureBlock> RUGGED_TABLE = artifact("rugged_table", WOODEN,
            ArchaeItems.BUNDLED_STICKS, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> ROYAL_RUG = artifact("royal_rug", DECORATION,
            ArchaeItems.ARCANE_GOLD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> OIL_LANTERN = artifact("oil_lantern", LANTERN,
            ArchaeItems.ANCIENT_OIL, 2,
            p -> new FurnitureBlock(p, false, Form.FLOOR, null, ArchaeArtifacts.OIL_LANTERN_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> OIL_LANTERN_CEILING = variant("oil_lantern_ceiling", LANTERN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.OIL_LANTERN));
    public static final DeferredBlock<FacingFurnitureBlock> IRON_TORCH_WALL = variant("iron_torch_wall", LANTERN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.IRON_TORCH));
    public static final DeferredBlock<FurnitureBlock> IRON_TORCH = artifact("iron_torch", LANTERN,
            ArchaeItems.ANCIENT_OIL, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.IRON_TORCH_WALL, null, null));
    public static final DeferredBlock<FurnitureBlock> LARGE_RAGGED_WALL_TARP = artifact("large_ragged_wall_tarp", DECORATION,
            ArchaeItems.WORN_LEATHER, 3, p -> new FurnitureBlock(p, true, Form.BLOCK_UP));
    public static final DeferredBlock<FurnitureBlock> SMALL_RAGGED_WALL_TARP = artifact("small_ragged_wall_tarp", DECORATION,
            ArchaeItems.WORN_LEATHER, 2, p -> new FurnitureBlock(p, true, Form.BLOCK_UP));
    public static final DeferredBlock<FurnitureBlock> RAGGED_TENT = artifact("ragged_tent", DECORATION,
            ArchaeItems.WORN_LEATHER, 5, p -> new FurnitureBlock(p, true, Form.FULL_DECORATION));
    public static final DeferredBlock<FurnitureBlock> LEATHER_SACK = artifact("leather_sack", DECORATION,
            ArchaeItems.WORN_LEATHER, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_PEACEFUL_STATUE = artifact("large_peaceful_statue", DEFAULT,
            ArchaeItems.FOREGONE_FRAGMENT, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> SMALL_PEACEFUL_STATUE = artifact("small_peaceful_statue", DEFAULT,
            ArchaeItems.FOREGONE_FRAGMENT, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> LARGE_ANCIENT_WEB = artifact("large_ancient_web", SILK,
            ArchaeItems.PRIMAL_SILK, 3,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, null, ArchaeArtifacts.LARGE_ANCIENT_WEB_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_ANCIENT_WEB_CEILING = variant("large_ancient_web_ceiling", SILK,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.LARGE_ANCIENT_WEB));
    public static final DeferredBlock<FurnitureBlock> SMALL_ANCIENT_WEB = artifact("small_ancient_web", SILK,
            ArchaeItems.PRIMAL_SILK, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, null, ArchaeArtifacts.SMALL_ANCIENT_WEB_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> SMALL_ANCIENT_WEB_CEILING = variant("small_ancient_web_ceiling", SILK,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SMALL_ANCIENT_WEB));
    public static final DeferredBlock<FurnitureBlock> SILK_COCOON = artifact("silk_cocoon", SILK,
            ArchaeItems.PRIMAL_SILK, 3,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, null, ArchaeArtifacts.SILK_COCOON_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> SILK_COCOON_CEILING = variant("silk_cocoon_ceiling", SILK,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SILK_COCOON));
    public static final DeferredBlock<FacingFurnitureBlock> TEXTILE_FATE_WALL = variant("textile_fate_wall", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.TEXTILE_FATE_FLOOR));
    public static final DeferredBlock<FacingFurnitureBlock> TEXTILE_FATE_CEILING = variant("textile_fate_ceiling", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.TEXTILE_FATE_FLOOR));
    public static final DeferredBlock<FurnitureBlock> TEXTILE_FATE_FLOOR = artifact("textile_fate_floor", DECORATION,
            ArchaeItems.TEXTILE_CLOTH, 2,
            p -> new FurnitureBlock(p, true, Form.DECORATION, ArchaeArtifacts.TEXTILE_FATE_WALL, ArchaeArtifacts.TEXTILE_FATE_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> TEXTILE_GRIEF_WALL = variant("textile_grief_wall", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.TEXTILE_GRIEF_FLOOR));
    public static final DeferredBlock<FacingFurnitureBlock> TEXTILE_GRIEF_CEILING = variant("textile_grief_ceiling", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.TEXTILE_GRIEF_FLOOR));
    public static final DeferredBlock<FurnitureBlock> TEXTILE_GRIEF_FLOOR = artifact("textile_grief_floor", DECORATION,
            ArchaeItems.TEXTILE_CLOTH, 3,
            p -> new FurnitureBlock(p, true, Form.DECORATION, ArchaeArtifacts.TEXTILE_GRIEF_WALL, ArchaeArtifacts.TEXTILE_GRIEF_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> JEWELRY_PILE = artifact("jewelry_pile", DECORATION,
            ArchaeItems.LOST_JEWELS, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> GLIDER_SKELETON = artifact("glider_skeleton", DEFAULT,
            ArchaeItems.FOSSILIZED_BONES, 16,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, null, ArchaeArtifacts.GLIDER_SKELETON_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> GLIDER_SKELETON_CEILING = variant("glider_skeleton_ceiling", DEFAULT,
            p -> new FurnitureBlock(p, false, Form.BLOCK_UP, null, null, ArchaeArtifacts.GLIDER_SKELETON));
    public static final DeferredBlock<FacingFurnitureBlock> LOST_TOME = artifact("lost_tome", DEFAULT,
            ArchaeItems.DECREPIT_STONES, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> STONE_PEDESTAL = artifact("stone_pedestal", DEFAULT,
            ArchaeItems.DECREPIT_STONES, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> STONE_TOMB = artifact("stone_tomb", DEFAULT,
            ArchaeItems.DECREPIT_STONES, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> POTION_ASSORTMENT = artifact("potion_assortment", DECORATION,
            ArchaeItems.VOLATILE_BREW, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<SeatFurnitureBlock> WOODEN_WHEELBARROW = artifact("wooden_wheelbarrow", WOODEN,
            ArchaeItems.BUSTED_WHEEL, 2, p -> new SeatFurnitureBlock(p, false, 10));
    public static final DeferredBlock<SeatFurnitureBlock> WOODEN_MINECART = artifact("wooden_minecart", WOODEN,
            ArchaeItems.BUSTED_WHEEL, 3, p -> new SeatFurnitureBlock(p, false, 8));
    public static final DeferredBlock<FacingFurnitureBlock> CERAMIC_POT = artifact("ceramic_pot", POT,
            ArchaeItems.CERAMIC_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> CERAMIC_VASE = artifact("ceramic_vase", POT,
            ArchaeItems.CERAMIC_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> CERAMIC_POT_SMALL = artifact("ceramic_pot_small", POT,
            ArchaeItems.CERAMIC_SHERD, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<AnimatedFurnitureBlock> AGED_FLAG = artifact("aged_flag", WOODEN,
            ArchaeItems.AGED_FABRIC, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.AGED_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> AGED_FLAG_WALL = variant("aged_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.AGED_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> AGED_BANNER_LARGE_WALL = variant("aged_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.AGED_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> AGED_BANNER_LARGE_CEILING = variant("aged_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.AGED_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> AGED_BANNER_LARGE_POST = artifact("aged_banner_large_post", WOODEN,
            ArchaeItems.AGED_FABRIC, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL_FLOOR, ArchaeArtifacts.AGED_BANNER_LARGE_WALL, ArchaeArtifacts.AGED_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> AGED_BANNER = artifact("aged_banner", WOODEN,
            ArchaeItems.AGED_FABRIC, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.AGED_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> AGED_BANNER_CEILING = variant("aged_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.AGED_BANNER));
    public static final DeferredBlock<FurnitureBlock> LARGE_MAP = artifact("large_map", DECORATION,
            ArchaeItems.PRIMITIVE_PAPER, 3, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> SMALL_MAP = artifact("small_map", DECORATION,
            ArchaeItems.PRIMITIVE_PAPER, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> ANCIENT_SCROLL = artifact("ancient_scroll", DECORATION,
            ArchaeItems.PRIMITIVE_PAPER, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> BARREL = artifact("barrel", HARD_WOODEN,
            ArchaeItems.SPLINTERED_WOOD, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> BATTERED_BARREL = artifact("battered_barrel", HARD_WOODEN,
            ArchaeItems.SPLINTERED_WOOD, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> KEG = artifact("keg", HARD_WOODEN,
            ArchaeItems.SPLINTERED_WOOD, 5, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> BONE_REMAINS = artifact("bone_remains", DECORATION,
            ArchaeItems.OBSCURE_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> OPEN_SKULL = artifact("open_skull", DECORATION,
            ArchaeItems.OBSCURE_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> SKULL = artifact("skull", DECORATION,
            ArchaeItems.OBSCURE_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> TILTED_SKULL = artifact("tilted_skull", DECORATION,
            ArchaeItems.OBSCURE_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> SMALL_BRICK_STACK = artifact("small_brick_stack", DECORATION,
            ArchaeItems.OLD_BRICKS, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_BRICK_STACK = artifact("large_brick_stack", DECORATION,
            ArchaeItems.OLD_BRICKS, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> OLD_WELL = artifact("old_well", DEFAULT,
            ArchaeItems.OLD_BRICKS, 9, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> ANCIENT_BOOK = artifact("ancient_book", DECORATION,
            ArchaeItems.TORN_PAGES, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> ANCIENT_JOURNAL = artifact("ancient_journal", DECORATION,
            ArchaeItems.TORN_PAGES, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> ANCIENT_SPELLBOOK = artifact("ancient_spellbook", DECORATION,
            ArchaeItems.TORN_PAGES, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_ANCIENT_BOOK_ROW = artifact("large_ancient_book_row", DECORATION_COLLISION,
            ArchaeItems.TORN_PAGES, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> SMALL_ANCIENT_BOOK_ROW = artifact("small_ancient_book_row", DECORATION_COLLISION,
            ArchaeItems.TORN_PAGES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_ANCIENT_BOOK_STACK = artifact("large_ancient_book_stack", DECORATION_COLLISION,
            ArchaeItems.TORN_PAGES, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> SMALL_ANCIENT_BOOK_STACK = artifact("small_ancient_book_stack", DECORATION_COLLISION,
            ArchaeItems.TORN_PAGES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> OPEN_ANCIENT_BOOK = artifact("open_ancient_book", DECORATION,
            ArchaeItems.TORN_PAGES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));

    // Illager
    public static final DeferredBlock<FurnitureBlock> FORBIDDEN_TOME = artifact("forbidden_tome", DEFAULT,
            ArchaeItems.ILL_FRAGMENT, 3, p -> new FurnitureBlock(p, true, Form.TWO_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> ILL_IDOL = artifact("ill_idol", DEFAULT,
            ArchaeItems.ILL_FRAGMENT, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> ILL_ANCESTOR_STATUE = artifact("ill_ancestor_statue", DEFAULT,
            ArchaeItems.OMINOUS_SLATE, 8, p -> new FurnitureBlock(p, false, Form.FIVE_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> IRK_GARGOYLE_WALL = variant("irk_gargoyle_wall", DEFAULT,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.IRK_GARGOYLE));
    public static final DeferredBlock<FurnitureBlock> IRK_GARGOYLE = artifact("irk_gargoyle", DEFAULT,
            ArchaeItems.OMINOUS_SLATE, 2,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.IRK_GARGOYLE_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_DARK_OAK_SAPLING = artifact("potted_dark_oak_sapling", DEFAULT,
            ArchaeItems.OMINOUS_SLATE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> ILLAGER_FLAG = artifact("illager_flag", WOODEN,
            ArchaeItems.RAVAGED_HIDE, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.ILLAGER_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> ILLAGER_FLAG_WALL = variant("illager_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ILLAGER_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> ILLAGER_BANNER_LARGE_WALL = variant("illager_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ILLAGER_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> ILLAGER_BANNER_LARGE_CEILING = variant("illager_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ILLAGER_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> ILLAGER_BANNER_LARGE_POST = artifact("illager_banner_large_post", WOODEN,
            ArchaeItems.RAVAGED_HIDE, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.ILLAGER_BANNER_LARGE_WALL, ArchaeArtifacts.ILLAGER_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> ILLAGER_BANNER = artifact("illager_banner", WOODEN,
            ArchaeItems.RAVAGED_HIDE, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.ILLAGER_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> ILLAGER_BANNER_CEILING = variant("illager_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ILLAGER_BANNER));
    public static final DeferredBlock<CeilingFurnitureBlock> SILVER_CHANDELIER = artifact("silver_chandelier", LANTERN,
            ArchaeItems.SILVER_SHARD, 5, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FurnitureBlock> SILVER_CANDELABRA = artifact("silver_candelabra", LANTERN,
            ArchaeItems.SILVER_SHARD, 3, p -> new FurnitureBlock(p, false, Form.TWO_TALL));
    public static final DeferredBlock<FurnitureBlock> BATTERING_RAM = artifact("battering_ram", WOODEN,
            ArchaeItems.GLOOMY_LUMBER, 8,
            p -> new FurnitureBlock(p, true, Form.TWO_TALL, null, ArchaeArtifacts.BATTERING_RAM_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> BATTERING_RAM_CEILING = variant("battering_ram_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.BATTERING_RAM));
    public static final DeferredBlock<FurnitureBlock> TARGET_DUMMY = artifact("target_dummy", WOODEN,
            ArchaeItems.GLOOMY_LUMBER, 2, p -> new FurnitureBlock(p, true, Form.TWO_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> WEAPON_RACK_WALL = variant("weapon_rack_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.WEAPON_RACK));
    public static final DeferredBlock<FurnitureBlock> WEAPON_RACK = artifact("weapon_rack", WOODEN,
            ArchaeItems.SINISTEEL_PIECE, 3,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.WEAPON_RACK_WALL, null, null));

    // Depths
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_DEEPSLATE_RUBBLE = artifact("large_deepslate_rubble", DEFAULT,
            ArchaeItems.DEEPSLATE_PIECE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> DEEPSLATE_RUBBLE = artifact("deepslate_rubble", DEFAULT,
            ArchaeItems.DEEPSLATE_PIECE, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> ANCIENT_VESSEL = artifact("ancient_vessel", DEFAULT,
            ArchaeItems.DEEPSLATE_PIECE, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> LOST_ARK = artifact("lost_ark", DEFAULT,
            ArchaeItems.DEEPSLATE_PIECE, 5, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> REINFORCED_PEDESTAL = artifact("reinforced_pedestal", DEFAULT,
            ArchaeItems.DEEPSLATE_PIECE, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<CeilingFurnitureBlock> ECHO_CHIMES = artifact("echo_chimes", DEFAULT,
            ArchaeItems.ECHO_CLUSTER, 2, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FurnitureBlock> SCULK_CANDELABRA = artifact("sculk_candelabra", DEFAULT,
            ArchaeItems.ECHO_CLUSTER, 3, p -> new FurnitureBlock(p, true, Form.TWO_TALL));
    public static final DeferredBlock<FurnitureBlock> SCULK_CANDLES = artifact("sculk_candles", DEFAULT,
            ArchaeItems.ECHO_CLUSTER, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> GATEWAY_SCULPTURE = artifact("gateway_sculpture", DEFAULT,
            ArchaeItems.FORGOTTEN_REMNANT, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> GECHO_GARGOYLE_WALL = variant("gecho_gargoyle_wall", DEFAULT,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.GECHO_GARGOYLE));
    public static final DeferredBlock<FurnitureBlock> GECHO_GARGOYLE = artifact("gecho_gargoyle", DEFAULT,
            ArchaeItems.FORGOTTEN_REMNANT, 4,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.GECHO_GARGOYLE_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> MUTE_MONOLITH = artifact("mute_monolith", DEFAULT,
            ArchaeItems.FORGOTTEN_REMNANT, 8, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> ANTLERED_SILENCED_SKULL = artifact("antlered_silenced_skull", DECORATION,
            ArchaeItems.HOLLOW_BONES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> HORNED_SILENCED_SKULL = artifact("horned_silenced_skull", DECORATION,
            ArchaeItems.HOLLOW_BONES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> PROLONGED_SILENCED_SKULL = artifact("prolonged_silenced_skull", DECORATION,
            ArchaeItems.HOLLOW_BONES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> RESOUNDING_HORN = artifact("resounding_horn", DEFAULT,
            ArchaeItems.HOLLOW_BONES, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> RUINED_HUSHER = artifact("ruined_husher", DEFAULT,
            ArchaeItems.SILENT_SILVER, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> ECHO_BELL = artifact("echo_bell", DEFAULT,
            ArchaeItems.SILENT_SILVER, 6, p -> new AnimatedFurnitureBlock(p, "archaeology", false, "bell_sway", null));

    // Arctic
    public static final DeferredBlock<FacingFurnitureBlock> SHORT_NOMAD_BASKET = artifact("short_nomad_basket", DEFAULT,
            ArchaeItems.BRISK_STRAW, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> SMALL_NOMAD_BASKET = artifact("small_nomad_basket", DEFAULT,
            ArchaeItems.BRISK_STRAW, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> TALL_NOMAD_BASKET = artifact("tall_nomad_basket", DEFAULT,
            ArchaeItems.BRISK_STRAW, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<SeatFurnitureBlock> NOMAD_BOAT = artifact("nomad_boat", DEFAULT,
            ArchaeItems.BROKEN_HULL, 8, p -> new SeatFurnitureBlock(p, true, 6));
    public static final DeferredBlock<FacingFurnitureBlock> CRYSTALLINE_LANTERN_WALL = variant("crystalline_lantern_wall", LANTERN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.CRYSTALLINE_LANTERN));
    public static final DeferredBlock<FacingFurnitureBlock> CRYSTALLINE_LANTERN_CEILING = variant("crystalline_lantern_ceiling", LANTERN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.CRYSTALLINE_LANTERN));
    public static final DeferredBlock<FurnitureBlock> CRYSTALLINE_LANTERN = artifact("crystalline_lantern", LANTERN,
            ArchaeItems.CRYSTALLINE_SHARDS, 2,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.CRYSTALLINE_LANTERN_WALL, ArchaeArtifacts.CRYSTALLINE_LANTERN_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> CRYSTALLINE_CANDELABRA = artifact("crystalline_candelabra", LANTERN,
            ArchaeItems.CRYSTALLINE_SHARDS, 3, p -> new FurnitureBlock(p, true, Form.TWO_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> STASIS_HAMMER = artifact("stasis_hammer", DEFAULT,
            ArchaeItems.FRIGID_ALLOY, 5, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> FROST_FLAG = artifact("frost_flag", DECORATION_COLLISION,
            ArchaeItems.FROSTED_FABRIC, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.FROST_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> FROST_FLAG_WALL = variant("frost_flag_wall", DECORATION_COLLISION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.FROST_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> FROST_BANNER_LARGE_WALL = variant("frost_banner_large_wall", DECORATION_COLLISION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.FROST_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> FROST_BANNER_LARGE_CEILING = variant("frost_banner_large_ceiling", DECORATION_COLLISION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.FROST_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> FROST_BANNER_LARGE_POST = artifact("frost_banner_large_post", DECORATION_COLLISION,
            ArchaeItems.FROSTED_FABRIC, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.FROST_BANNER_LARGE_WALL, ArchaeArtifacts.FROST_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> FROST_BANNER = artifact("frost_banner", DECORATION_COLLISION,
            ArchaeItems.FROSTED_FABRIC, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.FROST_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> FROST_BANNER_CEILING = variant("frost_banner_ceiling", DECORATION_COLLISION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.FROST_BANNER));
    public static final DeferredBlock<FurnitureBlock> SMALL_ICE_BRICK_STACK = artifact("small_ice_brick_stack", DECORATION,
            ArchaeItems.FROZEN_FRAGMENT, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_ICE_BRICK_STACK = artifact("large_ice_brick_stack", DECORATION_COLLISION,
            ArchaeItems.FROZEN_FRAGMENT, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> FROZEN_WARRIOR = artifact("frozen_warrior", DEFAULT,
            ArchaeItems.FROZEN_FRAGMENT, 9, p -> new FurnitureBlock(p, false, Form.TWO_TALL));
    public static final DeferredBlock<FurnitureBlock> FROST_STATUE = artifact("frost_statue", DEFAULT,
            ArchaeItems.KYANITE_SLATE, 3, p -> new FurnitureBlock(p, false, Form.TWO_TALL));
    public static final DeferredBlock<FurnitureBlock> NOMAD_WARRIOR_STATUE = artifact("nomad_warrior_statue", DEFAULT,
            ArchaeItems.KYANITE_SLATE, 6, p -> new FurnitureBlock(p, false, Form.TWO_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> FUR_BUNDLE = artifact("fur_bundle", DEFAULT,
            ArchaeItems.PRIMITIVE_FUR, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> FUR_PELT_WALL = variant("fur_pelt_wall", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.FUR_PELT_FLOOR));
    public static final DeferredBlock<FacingFurnitureBlock> FUR_PELT_CEILING = variant("fur_pelt_ceiling", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.FUR_PELT_FLOOR));
    public static final DeferredBlock<FurnitureBlock> FUR_PELT_FLOOR = artifact("fur_pelt_floor", DECORATION,
            ArchaeItems.PRIMITIVE_FUR, 2,
            p -> new FurnitureBlock(p, true, Form.DECORATION, ArchaeArtifacts.FUR_PELT_WALL, ArchaeArtifacts.FUR_PELT_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> FUR_SLEEPING_BAG = artifact("fur_sleeping_bag", DECORATION,
            ArchaeItems.PRIMITIVE_FUR, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> NOMAD_WAR_HORN = artifact("nomad_war_horn", DEFAULT,
            ArchaeItems.SEVERED_HORN, 4, p -> new FacingFurnitureBlock(p, false, null));

    // Ocean
    public static final DeferredBlock<FacingFurnitureBlock> MARINE_VASE = artifact("marine_vase", DEFAULT,
            ArchaeItems.DOLOMITE_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> TALL_MARINE_VASE = artifact("tall_marine_vase", DEFAULT,
            ArchaeItems.DOLOMITE_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> TALL_DOLOMITE_PILLAR_WALL = variant("tall_dolomite_pillar_wall", DEFAULT,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.TALL_DOLOMITE_PILLAR));
    public static final DeferredBlock<FurnitureBlock> TALL_DOLOMITE_PILLAR = artifact("tall_dolomite_pillar", DEFAULT,
            ArchaeItems.DOLOMITE_SHERD, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.TALL_DOLOMITE_PILLAR_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> SHORT_DOLOMITE_PILLAR = artifact("short_dolomite_pillar", DEFAULT,
            ArchaeItems.DOLOMITE_SHERD, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<WaterloggedFurnitureBlock> WHALE_STATUE_WATER = variant("whale_statue_water", DEFAULT,
            p -> new WaterloggedFurnitureBlock(p, false, DisplayMode.FLOOR, ArchaeArtifacts.WHALE_STATUE, null));
    public static final DeferredBlock<WaterloggedFurnitureBlock> WHALE_STATUE = artifact("whale_statue", DEFAULT,
            ArchaeItems.DOLOMITE_SHERD, 5,
            p -> new WaterloggedFurnitureBlock(p, false, DisplayMode.FLOOR, null, ArchaeArtifacts.WHALE_STATUE_WATER));
    public static final DeferredBlock<FacingFurnitureBlock> DAMAGED_CANNON = artifact("damaged_cannon", DEFAULT,
            ArchaeItems.SUNKEN_STEEL, 5, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_ANCHOR_WALL = variant("sunken_anchor_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SUNKEN_ANCHOR));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_ANCHOR_CEILING = variant("sunken_anchor_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SUNKEN_ANCHOR));
    public static final DeferredBlock<FurnitureBlock> SUNKEN_ANCHOR = artifact("sunken_anchor", WOODEN,
            ArchaeItems.SUNKEN_STEEL, 4,
            p -> new FurnitureBlock(p, false, Form.FLOOR, ArchaeArtifacts.SUNKEN_ANCHOR_WALL, ArchaeArtifacts.SUNKEN_ANCHOR_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_CANNONBALLS_WALL = variant("sunken_cannonballs_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SUNKEN_CANNONBALLS));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_CANNONBALLS_CEILING = variant("sunken_cannonballs_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SUNKEN_CANNONBALLS));
    public static final DeferredBlock<FurnitureBlock> SUNKEN_CANNONBALLS = artifact("sunken_cannonballs", WOODEN,
            ArchaeItems.SUNKEN_STEEL, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.SUNKEN_CANNONBALLS_WALL, ArchaeArtifacts.SUNKEN_CANNONBALLS_CEILING, null));
    public static final DeferredBlock<WaterloggedFurnitureBlock> LOST_TREASURE_CHEST_WATER = variant("lost_treasure_chest_water", HARD_WOODEN,
            p -> new WaterloggedFurnitureBlock(p, false, DisplayMode.TOP_FACE, ArchaeArtifacts.LOST_TREASURE_CHEST, null));
    public static final DeferredBlock<WaterloggedFurnitureBlock> LOST_TREASURE_CHEST = artifact("lost_treasure_chest", HARD_WOODEN,
            ArchaeItems.CORRODED_BOARD, 3,
            p -> new WaterloggedFurnitureBlock(p, false, DisplayMode.TOP_FACE, null, ArchaeArtifacts.LOST_TREASURE_CHEST_WATER));
    public static final DeferredBlock<FacingFurnitureBlock> BATTERED_HELM_WALL = variant("battered_helm_wall", HARD_WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.BATTERED_HELM));
    public static final DeferredBlock<FurnitureBlock> BATTERED_HELM = artifact("battered_helm", HARD_WOODEN,
            ArchaeItems.CORRODED_BOARD, 4,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.BATTERED_HELM_WALL, null, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> SUNKEN_FLAG = artifact("sunken_flag", WOODEN,
            ArchaeItems.CORRODED_BOARD, 4,
            p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.SUNKEN_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_FLAG_WALL = variant("sunken_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.SUNKEN_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_BANNER_LARGE_WALL = variant("sunken_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.SUNKEN_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_BANNER_LARGE_CEILING = variant("sunken_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.SUNKEN_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> SUNKEN_BANNER_LARGE_POST = artifact("sunken_banner_large_post", WOODEN,
            ArchaeItems.CORRODED_BOARD, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.SUNKEN_BANNER_LARGE_WALL, ArchaeArtifacts.SUNKEN_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> SUNKEN_BANNER = artifact("sunken_banner", WOODEN,
            ArchaeItems.CORRODED_BOARD, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.SUNKEN_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> SUNKEN_BANNER_CEILING = variant("sunken_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.SUNKEN_BANNER));
    public static final DeferredBlock<FacingFurnitureBlock> SWASHED_PLANKS_WALL = variant("swashed_planks_wall", DECORATION,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SWASHED_PLANKS));
    public static final DeferredBlock<FurnitureBlock> SWASHED_PLANKS = artifact("swashed_planks", DECORATION,
            ArchaeItems.CORRODED_BOARD, 1,
            p -> new FurnitureBlock(p, false, Form.FLOOR, ArchaeArtifacts.SWASHED_PLANKS_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> BUBBLE_BRAZIER = artifact("bubble_brazier", DEFAULT,
            ArchaeItems.FRACTURED_PRISMARINE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<WaterloggedFurnitureBlock> COLLAPSED_CONSTRUCT_WATER = variant("collapsed_construct_water", HARD_WOODEN,
            p -> new WaterloggedFurnitureBlock(p, true, DisplayMode.TOP_FACE, ArchaeArtifacts.COLLAPSED_CONSTRUCT, null));
    public static final DeferredBlock<WaterloggedFurnitureBlock> COLLAPSED_CONSTRUCT = artifact("collapsed_construct", HARD_WOODEN,
            ArchaeItems.FRACTURED_PRISMARINE, 2,
            p -> new WaterloggedFurnitureBlock(p, true, DisplayMode.TOP_FACE, null, ArchaeArtifacts.COLLAPSED_CONSTRUCT_WATER));
    public static final DeferredBlock<FacingFurnitureBlock> CORAL_PLANTER = artifact("coral_planter", DEFAULT,
            ArchaeItems.FRACTURED_PRISMARINE, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SENTINEL_STATUE = artifact("sentinel_statue", DEFAULT,
            ArchaeItems.FRACTURED_PRISMARINE, 3, p -> new FacingFurnitureBlock(p, false, null));

    // Badlands
    public static final DeferredBlock<FurnitureBlock> BEAST_SKULL = artifact("beast_skull", DECORATION,
            ArchaeItems.BEAST_HORN, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> GOLD_BAR_PILE_LARGE = artifact("gold_bar_pile_large", DEFAULT,
            ArchaeItems.FORTUNATE_GOLD, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> GOLD_BAR_PILE_SMALL = artifact("gold_bar_pile_small", DECORATION,
            ArchaeItems.FORTUNATE_GOLD, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> GOLDEN_HORSESHOE = artifact("golden_horseshoe", DECORATION,
            ArchaeItems.FORTUNATE_GOLD, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> GOLDEN_HORSESHOE_STACK = artifact("golden_horseshoe_stack", DECORATION,
            ArchaeItems.FORTUNATE_GOLD, 3, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> DYNAMITE_CRATE = artifact("dynamite_crate", DEFAULT,
            ArchaeItems.SIMMERING_GUNPOWDER, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> DYNAMITE_PILE = artifact("dynamite_pile", DECORATION,
            ArchaeItems.SIMMERING_GUNPOWDER, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> TERRACOTTA_AMPHORA = artifact("terracotta_amphora", POT_COLLISION,
            ArchaeItems.TERRACOTTA_SHERD, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> TERRACOTTA_BOWL = artifact("terracotta_bowl", POT,
            ArchaeItems.TERRACOTTA_SHERD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> TERRACOTTA_CUPS = artifact("terracotta_cups", POT,
            ArchaeItems.TERRACOTTA_SHERD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> TERRACOTTA_OIL_LAMP = artifact("terracotta_oil_lamp", POT,
            ArchaeItems.TERRACOTTA_SHERD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> TERRACOTTA_PLATE = artifact("terracotta_plate", POT,
            ArchaeItems.TERRACOTTA_SHERD, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<CeilingFurnitureBlock> HANGING_ROPES = artifact("hanging_ropes", WOODEN_NO_COLLISION,
            ArchaeItems.WORN_TWINE, 2, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FurnitureBlock> ROPE_COIL = artifact("rope_coil", WOODEN,
            ArchaeItems.WORN_TWINE, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> ROPE_WINCH_WALL = variant("rope_winch_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.ROPE_WINCH_FLOOR));
    public static final DeferredBlock<FacingFurnitureBlock> ROPE_WINCH_CEILING = variant("rope_winch_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.ROPE_WINCH_FLOOR));
    public static final DeferredBlock<FurnitureBlock> ROPE_WINCH_FLOOR = artifact("rope_winch_floor", WOODEN,
            ArchaeItems.WORN_TWINE, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.ROPE_WINCH_WALL, ArchaeArtifacts.ROPE_WINCH_CEILING, null));

    // Blight
    public static final DeferredBlock<FurnitureBlock> GRIM_SKULL = artifact("grim_skull", DECORATION,
            ArchaeItems.CURSED_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> SKULL_TORCH = artifact("skull_torch", TORCH,
            ArchaeItems.CURSED_BONES, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> SKULL_CANDLE = artifact("skull_candle", CANDLE,
            ArchaeItems.CURSED_BONES, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<AnimatedFurnitureBlock> BANDIT_FLAG = artifact("bandit_flag", WOODEN,
            ArchaeItems.CURSED_FABRIC, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.BANDIT_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> BANDIT_FLAG_WALL = variant("bandit_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BANDIT_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> BANDIT_BANNER_LARGE_WALL = variant("bandit_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BANDIT_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> BANDIT_BANNER_LARGE_CEILING = variant("bandit_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BANDIT_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> BANDIT_BANNER_LARGE_POST = artifact("bandit_banner_large_post", WOODEN,
            ArchaeItems.CURSED_FABRIC, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.BANDIT_BANNER_LARGE_WALL, ArchaeArtifacts.BANDIT_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> BANDIT_BANNER = artifact("bandit_banner", WOODEN,
            ArchaeItems.CURSED_FABRIC, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.BANDIT_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> BANDIT_BANNER_CEILING = variant("bandit_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BANDIT_BANNER));
    public static final DeferredBlock<FurnitureBlock> BLIGHTED_BRAZIER = artifact("blighted_brazier", LANTERN,
            ArchaeItems.MALEVOLENT_MINERAL, 3, p -> new FurnitureBlock(p, false, Form.FULL_DECORATION));
    public static final DeferredBlock<FurnitureBlock> CURSED_GRIM_SKULL = artifact("cursed_grim_skull", DECORATION,
            ArchaeItems.POSSESSED_HORN, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> CURSED_BEAST_SKULL = artifact("cursed_beast_skull", DECORATION,
            ArchaeItems.BEAST_HORN, 3, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_BARRAGE = artifact("potted_barrage", DEFAULT,
            ArchaeItems.PUTRID_SEEDS, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> SMALL_GEAR_WALL = artifact("small_gear_wall", DEFAULT,
            ArchaeItems.RUSTED_SCRAP, 2, p -> new AnimatedFurnitureBlock(p, "archaeology", false, "gear_spinning", null));
    public static final DeferredBlock<AnimatedFurnitureBlock> LARGE_GEAR_WALL = artifact("large_gear_wall", DEFAULT,
            ArchaeItems.RUSTED_SCRAP, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", false, "gear_spinning_large", null));

    // Savannah
    public static final DeferredBlock<FacingFurnitureBlock> PIKED_ANCESTOR_SKULL = artifact("piked_ancestor_skull", DECORATION_COLLISION,
            ArchaeItems.ANCESTOR_TOOTH, 4, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> ANCESTRAL_FLAG = artifact("ancestral_flag", WOODEN,
            ArchaeItems.ANCESTRAL_QUILT, 4,
            p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.ANCESTRAL_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> ANCESTRAL_FLAG_WALL = variant("ancestral_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ANCESTRAL_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> ANCESTRAL_BANNER_LARGE_WALL = variant("ancestral_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ANCESTRAL_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> ANCESTRAL_BANNER_LARGE_CEILING = variant("ancestral_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ANCESTRAL_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> ANCESTRAL_BANNER_LARGE_POST = artifact("ancestral_banner_large_post", WOODEN,
            ArchaeItems.ANCESTRAL_QUILT, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.ANCESTRAL_BANNER_LARGE_WALL, ArchaeArtifacts.ANCESTRAL_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> ANCESTRAL_BANNER = artifact("ancestral_banner", WOODEN,
            ArchaeItems.ANCESTRAL_QUILT, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.ANCESTRAL_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCESTRAL_BANNER_CEILING = variant("ancestral_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ANCESTRAL_BANNER));
    public static final DeferredBlock<FurnitureBlock> ANCESTRAL_RUG = artifact("ancestral_rug", DECORATION,
            ArchaeItems.ANCESTRAL_QUILT, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> TALL_REPTANT_VASE = artifact("tall_reptant_vase", POT_COLLISION,
            ArchaeItems.ARTISANT_REMNANT, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> SHORT_REPTANT_VASE = artifact("short_reptant_vase", POT,
            ArchaeItems.ARTISANT_REMNANT, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> SMALL_REPTANT_POT = artifact("small_reptant_pot", POT,
            ArchaeItems.ARTISANT_REMNANT, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> IGUANO_ANCESTOR_EFFIGY = artifact("iguano_ancestor_effigy", DEFAULT,
            ArchaeItems.BAOBAB_CARVING, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> IGUANO_DEITY_EFFIGY = artifact("iguano_deity_effigy", DEFAULT,
            ArchaeItems.BAOBAB_CARVING, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> IGUANO_HUNTER_EFFIGY = artifact("iguano_hunter_effigy", DEFAULT,
            ArchaeItems.BAOBAB_CARVING, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> LIZARD_FIGURE = artifact("lizard_figure", WOODEN,
            ArchaeItems.BAOBAB_CARVING, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> STRATA_DEBRIS_PILE_LARGE = artifact("strata_debris_pile_large", DECORATION_COLLISION,
            ArchaeItems.STRATA_PIECE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> STRATA_DEBRIS_PILE_SMALL = artifact("strata_debris_pile_small", DECORATION,
            ArchaeItems.STRATA_PIECE, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> REPTANT_MURAL = artifact("reptant_mural", DEFAULT,
            ArchaeItems.STRATA_PIECE, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SPEAR_RACK_WALL = variant("spear_rack_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.SPEAR_RACK));
    public static final DeferredBlock<FurnitureBlock> SPEAR_RACK = artifact("spear_rack", WOODEN,
            ArchaeItems.TATTERED_SPEARHEAD, 3,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.SPEAR_RACK_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> PIKED_LANTERN_WALL = variant("piked_lantern_wall", LANTERN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.PIKED_LANTERN));
    public static final DeferredBlock<FacingFurnitureBlock> PIKED_LANTERN_CEILING = variant("piked_lantern_ceiling", LANTERN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.PIKED_LANTERN));
    public static final DeferredBlock<FurnitureBlock> PIKED_LANTERN = artifact("piked_lantern", LANTERN,
            ArchaeItems.TATTERED_SPEARHEAD, 3,
            p -> new FurnitureBlock(p, true, Form.FLOOR, ArchaeArtifacts.PIKED_LANTERN_WALL, ArchaeArtifacts.PIKED_LANTERN_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> LARGE_IGUANO_MASK = artifact("large_iguano_mask", DECORATION,
            ArchaeItems.TRIBAL_TIMBER, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> SMALL_IGUANO_MASK = artifact("small_iguano_mask", DECORATION,
            ArchaeItems.TRIBAL_TIMBER, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> TALL_IGUANO_MASK = artifact("tall_iguano_mask", DECORATION,
            ArchaeItems.TRIBAL_TIMBER, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));

    // Desert
    public static final DeferredBlock<AnimatedFurnitureBlock> PYRAMID_RELIC = artifact("pyramid_relic", DEFAULT,
            ArchaeItems.ABNORMAL_SLAB, 16, p -> new AnimatedFurnitureBlock(p, "archaeology", true, "obelisk_rotating", null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCIENT_BRAZIER = artifact("ancient_brazier", TORCH,
            ArchaeItems.DESERTED_GOLD, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<CeilingFurnitureBlock> ANCIENT_LANTERN = artifact("ancient_lantern", TORCH,
            ArchaeItems.DESERTED_GOLD, 2, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<AnimatedFurnitureBlock> HIEROGLYPHIC_FLAG = artifact("hieroglyphic_flag", WOODEN,
            ArchaeItems.DRIED_FABRIC, 4,
            p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.HIEROGLYPHIC_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> HIEROGLYPHIC_FLAG_WALL = variant("hieroglyphic_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.HIEROGLYPHIC_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> HIEROGLYPHIC_BANNER_LARGE_WALL = variant("hieroglyphic_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.HIEROGLYPHIC_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> HIEROGLYPHIC_BANNER_LARGE_CEILING = variant("hieroglyphic_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.HIEROGLYPHIC_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> HIEROGLYPHIC_BANNER_LARGE_POST = artifact("hieroglyphic_banner_large_post", WOODEN,
            ArchaeItems.DRIED_FABRIC, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.HIEROGLYPHIC_BANNER_LARGE_WALL, ArchaeArtifacts.HIEROGLYPHIC_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> HIEROGLYPHIC_BANNER = artifact("hieroglyphic_banner", WOODEN,
            ArchaeItems.DRIED_FABRIC, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.HIEROGLYPHIC_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> HIEROGLYPHIC_BANNER_CEILING = variant("hieroglyphic_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.HIEROGLYPHIC_BANNER));
    public static final DeferredBlock<FacingFurnitureBlock> BROKEN_ANCESTRAL_AMPHORA = artifact("broken_ancestral_amphora", POT_COLLISION,
            ArchaeItems.DUSTY_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCESTRAL_AMPHORA = artifact("ancestral_amphora", POT_COLLISION,
            ArchaeItems.DUSTY_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCESTRAL_VASE = artifact("ancestral_vase", POT_COLLISION,
            ArchaeItems.DUSTY_SHERD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> MUMMY_JARS = artifact("mummy_jars", DECORATION,
            ArchaeItems.DUSTY_SHERD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_LIMESTONE_SLABS = artifact("large_limestone_slabs", DEFAULT,
            ArchaeItems.LIMESTONE_PIECE, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> SMALL_LIMESTONE_SLABS = artifact("small_limestone_slabs", DEFAULT,
            ArchaeItems.LIMESTONE_PIECE, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> ANCIENT_TABLET = artifact("ancient_tablet", DEFAULT,
            ArchaeItems.LIMESTONE_PIECE, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> NAJA_STATUE = artifact("naja_statue", DEFAULT,
            ArchaeItems.LIMESTONE_PIECE, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> MUMMY_STATUE = artifact("mummy_statue", DEFAULT,
            ArchaeItems.LIMESTONE_PIECE, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> SARCOPHAGUS = artifact("sarcophagus", DEFAULT,
            ArchaeItems.LIMESTONE_PIECE, 5, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCIENT_FERN_WALL = variant("ancient_fern_wall", DEFAULT,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.ANCIENT_FERN));
    public static final DeferredBlock<FurnitureBlock> ANCIENT_FERN = artifact("ancient_fern", DEFAULT,
            ArchaeItems.OASAL_SEEDS, 2,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.ANCIENT_FERN_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_CACTUS = artifact("potted_cactus", DEFAULT,
            ArchaeItems.OASAL_SEEDS, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_FAN_PALM = artifact("potted_fan_palm", DEFAULT,
            ArchaeItems.OASAL_SEEDS, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> PHARAOH_HEAD_STATUE = artifact("pharaoh_head_statue", DEFAULT,
            ArchaeItems.SOVEREIGN_TREASURE, 4, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> PHARAOH_STATUE = artifact("pharaoh_statue", DEFAULT,
            ArchaeItems.SOVEREIGN_TREASURE, 8, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SANDS_OF_TIME = artifact("sands_of_time", DEFAULT,
            ArchaeItems.SPIRITUAL_SAND, 4, p -> new FacingFurnitureBlock(p, true, null));

    // Nether
    public static final DeferredBlock<FacingFurnitureBlock> NETHERITE_BRAZIER = artifact("netherite_brazier", DEFAULT_LUMINANCE,
            ArchaeItems.NETHERITE_PIECE, 4, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> NETHERITE_CAGE = artifact("netherite_cage", DECORATION,
            ArchaeItems.NETHERITE_PIECE, 4,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, null, ArchaeArtifacts.NETHERITE_CAGE_HANGING, null));
    public static final DeferredBlock<FacingFurnitureBlock> NETHERITE_CAGE_HANGING = variant("netherite_cage_hanging", DECORATION,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.NETHERITE_CAGE));
    public static final DeferredBlock<FacingFurnitureBlock> NETHERITE_TORCH_WALL = variant("netherite_torch_wall", LANTERN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.NETHERITE_TORCH));
    public static final DeferredBlock<FurnitureBlock> NETHERITE_TORCH = artifact("netherite_torch", LANTERN,
            ArchaeItems.NETHERITE_PIECE, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.NETHERITE_TORCH_WALL, null, null));
    public static final DeferredBlock<CeilingFurnitureBlock> NETHERITE_LANTERN = artifact("netherite_lantern", LANTERN,
            ArchaeItems.NETHERITE_PIECE, 2, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FacingFurnitureBlock> SMOG_CHIMNEY_WALL = artifact("smog_chimney_wall", DEFAULT,
            ArchaeItems.SOOTY_DEBRIS, 1, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SMOG_PIPE = artifact("smog_pipe", DEFAULT,
            ArchaeItems.SOOTY_DEBRIS, 1, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> BASALT_CRATE_SMALL = artifact("basalt_crate_small", DEFAULT,
            ArchaeItems.BASALT_SLAG, 1, p -> new FurnitureBlock(p, false, Form.FULL_DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> BASALT_CRATE = artifact("basalt_crate", DEFAULT,
            ArchaeItems.BASALT_SLAG, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> BASALT_CRATE_TALL = artifact("basalt_crate_tall", DEFAULT,
            ArchaeItems.BASALT_SLAG, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> BASALT_RUBBLE_PILE_LARGE = artifact("basalt_rubble_pile_large", DEFAULT,
            ArchaeItems.BASALT_SLAG, 1, p -> new FurnitureBlock(p, false, Form.FULL_DECORATION));
    public static final DeferredBlock<FurnitureBlock> BASALT_RUBBLE_PILE_SMALL = artifact("basalt_rubble_pile_small", DEFAULT,
            ArchaeItems.BASALT_SLAG, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> GLOWSTONE_BRAZIER = artifact("glowstone_brazier", DEFAULT_LUMINANCE,
            ArchaeItems.ASHEN_SCRAP, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> IMPAIRED_ANVIL = artifact("impaired_anvil", DEFAULT,
            ArchaeItems.ASHEN_SCRAP, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SMOLDERING_FORGE = artifact("smoldering_forge", DEFAULT,
            ArchaeItems.ASHEN_SCRAP, 5, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> NETHERITE_SPIKES = artifact("netherite_spikes", DECORATION,
            ArchaeItems.GILDED_METAL, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> BOAR_HORN_TOWER = artifact("boar_horn_tower", DEFAULT,
            ArchaeItems.GILDED_METAL, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> PIGGISH_FIGURE = artifact("piggish_figure", DECORATION,
            ArchaeItems.PIGGISH_GOLD, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> PIGGISH_IDOL = artifact("piggish_idol", DEFAULT,
            ArchaeItems.PIGGISH_GOLD, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<SeatFurnitureBlock> PIGGISH_THRONE = artifact("piggish_throne", DEFAULT,
            ArchaeItems.PIGGISH_GOLD, 6, p -> new SeatFurnitureBlock(p, false, 11));
    public static final DeferredBlock<FacingFurnitureBlock> BLIGHTED_SHROOM_LANTERN_WALL = variant("blighted_shroom_lantern_wall", DECORATION_LUMINANCE,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.BLIGHTED_SHROOM_LANTERN));
    public static final DeferredBlock<FacingFurnitureBlock> BLIGHTED_SHROOM_LANTERN_CEILING = variant("blighted_shroom_lantern_ceiling", DECORATION_LUMINANCE,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.BLIGHTED_SHROOM_LANTERN));
    public static final DeferredBlock<FurnitureBlock> BLIGHTED_SHROOM_LANTERN = artifact("blighted_shroom_lantern", DECORATION_LUMINANCE,
            ArchaeItems.NOXIOUS_SPORES, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.BLIGHTED_SHROOM_LANTERN_WALL, ArchaeArtifacts.BLIGHTED_SHROOM_LANTERN_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_WARPED_FUNGUS = artifact("potted_warped_fungus", DECORATION,
            ArchaeItems.NOXIOUS_SPORES, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> SHROOM_LANTERN_WALL = variant("shroom_lantern_wall", DECORATION_LUMINANCE,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SHROOM_LANTERN));
    public static final DeferredBlock<FacingFurnitureBlock> SHROOM_LANTERN_CEILING = variant("shroom_lantern_ceiling", DECORATION_LUMINANCE,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.SHROOM_LANTERN));
    public static final DeferredBlock<FurnitureBlock> SHROOM_LANTERN = artifact("shroom_lantern", DECORATION_LUMINANCE,
            ArchaeItems.SEARING_SPORES, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.SHROOM_LANTERN_WALL, ArchaeArtifacts.SHROOM_LANTERN_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> CRIMSON_SPORE_SACK = artifact("crimson_spore_sack", DEFAULT,
            ArchaeItems.SEARING_SPORES, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> NETHER_WART_PLANTER_WALL = variant("nether_wart_planter_wall", DECORATION,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.NETHER_WART_PLANTER));
    public static final DeferredBlock<FurnitureBlock> NETHER_WART_PLANTER = artifact("nether_wart_planter", DECORATION,
            ArchaeItems.SEARING_SPORES, 1,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.NETHER_WART_PLANTER_WALL, null, null));
    public static final DeferredBlock<FurnitureBlock> PIGLIN_SKULL = artifact("piglin_skull", DECORATION,
            ArchaeItems.SWINISH_BONES, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> SMALL_PIGLIN_SKULLS = artifact("small_piglin_skulls", DECORATION,
            ArchaeItems.SWINISH_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> TALL_PIGLIN_SKULL = artifact("tall_piglin_skull", DECORATION,
            ArchaeItems.SWINISH_BONES, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> HOG_HUNT_EFFIGY = artifact("hog_hunt_effigy", DEFAULT,
            ArchaeItems.BOORISH_RIBS, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> HOGLIN_CARCASS = artifact("hoglin_carcass", DEFAULT,
            ArchaeItems.BOORISH_RIBS, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> HOGLIN_SKULL = artifact("hoglin_skull", DECORATION,
            ArchaeItems.BOORISH_RIBS, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> TUSK_BARRICADE = artifact("tusk_barricade", DEFAULT,
            ArchaeItems.HOGLIN_TUSK, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> TUSK_TORCH = artifact("tusk_torch", DECORATION_LUMINANCE,
            ArchaeItems.HOGLIN_TUSK, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<CeilingFurnitureBlock> TUSK_LANTERN = artifact("tusk_lantern", LANTERN,
            ArchaeItems.HOGLIN_TUSK, 2, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FurnitureBlock> HOGLIN_HIDE = artifact("hoglin_hide", DECORATION,
            ArchaeItems.HOG_HIDE, 2, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> HOGLIN_LEATHER_WALL = variant("hoglin_leather_wall", DECORATION,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.HOGLIN_LEATHER));
    public static final DeferredBlock<FacingFurnitureBlock> HOGLIN_LEATHER_CEILING = variant("hoglin_leather_ceiling", DECORATION,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.HOGLIN_LEATHER));
    public static final DeferredBlock<FurnitureBlock> HOGLIN_LEATHER = artifact("hoglin_leather", DECORATION,
            ArchaeItems.HOG_HIDE, 2,
            p -> new FurnitureBlock(p, false, Form.DECORATION, ArchaeArtifacts.HOGLIN_LEATHER_WALL, ArchaeArtifacts.HOGLIN_LEATHER_CEILING, null));
    public static final DeferredBlock<AnimatedFurnitureBlock> BASTION_HORDE_FLAG = artifact("bastion_horde_flag", WOODEN,
            ArchaeItems.PARCHED_PELT, 4,
            p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.BASTION_HORDE_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> BASTION_HORDE_FLAG_WALL = variant("bastion_horde_flag_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BASTION_HORDE_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> BASTION_HORDE_BANNER_LARGE_WALL = variant("bastion_horde_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BASTION_HORDE_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> BASTION_HORDE_BANNER_LARGE_CEILING = variant("bastion_horde_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BASTION_HORDE_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> BASTION_HORDE_BANNER_LARGE_POST = artifact("bastion_horde_banner_large_post", WOODEN,
            ArchaeItems.PARCHED_PELT, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.BASTION_HORDE_BANNER_LARGE_WALL, ArchaeArtifacts.BASTION_HORDE_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> BASTION_HORDE_BANNER = artifact("bastion_horde_banner", WOODEN,
            ArchaeItems.PARCHED_PELT, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.BASTION_HORDE_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> BASTION_HORDE_BANNER_CEILING = variant("bastion_horde_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.BASTION_HORDE_BANNER));
    public static final DeferredBlock<CeilingFurnitureBlock> IGNITED_CHANDELIER = artifact("ignited_chandelier", LANTERN,
            ArchaeItems.MOLTEN_METAL, 4, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FurnitureBlock> IGNITED_CANDELABRA = artifact("ignited_candelabra", LANTERN,
            ArchaeItems.MOLTEN_METAL, 2, p -> new FurnitureBlock(p, false, Form.TWO_TALL));
    public static final DeferredBlock<AnimatedFurnitureBlock> INFERNAL_FLAG = artifact("infernal_flag", TORCH,
            ArchaeItems.INFERNAL_FABRIC, 4,
            p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.INFERNAL_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> INFERNAL_FLAG_WALL = variant("infernal_flag_wall", TORCH,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.INFERNAL_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> INFERNAL_BANNER_LARGE_WALL = variant("infernal_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.INFERNAL_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> INFERNAL_BANNER_LARGE_CEILING = variant("infernal_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.INFERNAL_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> INFERNAL_BANNER_LARGE_POST = artifact("infernal_banner_large_post", TORCH,
            ArchaeItems.INFERNAL_FABRIC, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.INFERNAL_BANNER_LARGE_WALL, ArchaeArtifacts.INFERNAL_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> INFERNAL_BANNER = artifact("infernal_banner", WOODEN,
            ArchaeItems.INFERNAL_FABRIC, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.INFERNAL_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> INFERNAL_BANNER_CEILING = variant("infernal_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.INFERNAL_BANNER));
    public static final DeferredBlock<FurnitureBlock> NETHERRACK_POT = artifact("netherrack_pot", POT,
            ArchaeItems.NETHERRACK_SHARD, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> NETHERRACK_URN_LARGE = artifact("netherrack_urn_large", POT,
            ArchaeItems.NETHERRACK_SHARD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> NETHERRACK_URN = artifact("netherrack_urn", POT,
            ArchaeItems.NETHERRACK_SHARD, 2, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> NETHER_BRICK_STACK_LARGE = artifact("nether_brick_stack_large", POT,
            ArchaeItems.NETHERRACK_SHARD, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> NETHER_BRICK_STACK_SMALL = artifact("nether_brick_stack_small", POT,
            ArchaeItems.NETHERRACK_SHARD, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> LIFELESS_COFFIN = artifact("lifeless_coffin", DEFAULT,
            ArchaeItems.FORSAKEN_SLATE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> LIFELESS_TOMB = artifact("lifeless_tomb", DEFAULT,
            ArchaeItems.FORSAKEN_SLATE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> GRAVE_SLATE_TALL = artifact("grave_slate_tall", DEFAULT,
            ArchaeItems.FORSAKEN_SLATE, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> GRAVE_SLATE_SHORT = artifact("grave_slate_short", DEFAULT,
            ArchaeItems.FORSAKEN_SLATE, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> SOUL_CANDLES = artifact("soul_candles", CANDLE,
            ArchaeItems.WHISPERING_WAX, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> SPIRIT_BRAZIER = artifact("spirit_brazier", DEFAULT_LUMINANCE,
            ArchaeItems.WHISPERING_WAX, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> SPIRIT_CANDELABRA = artifact("spirit_candelabra", LANTERN,
            ArchaeItems.WHISPERING_WAX, 2, p -> new FurnitureBlock(p, true, Form.TWO_TALL));
    public static final DeferredBlock<FacingFurnitureBlock> SPIRIT_TORCH_WALL = variant("spirit_torch_wall", LANTERN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.SPIRIT_TORCH));
    public static final DeferredBlock<FurnitureBlock> SPIRIT_TORCH = artifact("spirit_torch", LANTERN,
            ArchaeItems.WHISPERING_WAX, 2,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, ArchaeArtifacts.SPIRIT_TORCH_WALL, null, null));
    public static final DeferredBlock<CeilingFurnitureBlock> SPIRIT_LANTERN = artifact("spirit_lantern", LANTERN,
            ArchaeItems.WHISPERING_WAX, 2, p -> new CeilingFurnitureBlock(p, true));

    // Aether
    public static final DeferredBlock<FacingFurnitureBlock> HOLYSTONE_COLUMN_RUINED = artifact("holystone_column_ruined", DEFAULT,
            ArchaeItems.REVERENT_RUBBLE, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> HOLYSTONE_BRICK_RUINS_SMALL = artifact("holystone_brick_ruins_small", DECORATION,
            ArchaeItems.REVERENT_RUBBLE, 1, p -> new FurnitureBlock(p, false, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> HOLYSTONE_BRICK_RUINS_LARGE = artifact("holystone_brick_ruins_large", DECORATION,
            ArchaeItems.REVERENT_RUBBLE, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> HOLYSTONE_RELIEF = artifact("holystone_relief", DEFAULT,
            ArchaeItems.REVERENT_RUBBLE, 8,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, null, ArchaeArtifacts.HOLYSTONE_RELIEF_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> HOLYSTONE_RELIEF_CEILING = variant("holystone_relief_ceiling", DEFAULT,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.HOLYSTONE_RELIEF));
    public static final DeferredBlock<FacingFurnitureBlock> ETHEREAL_AMPHORA = artifact("ethereal_amphora", DEFAULT,
            ArchaeItems.ETHEREAL_SHARD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> ETHEREAL_ARYBALLOS = artifact("ethereal_aryballos", DEFAULT,
            ArchaeItems.ETHEREAL_SHARD, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> ETHEREAL_KRATER = artifact("ethereal_krater", DEFAULT,
            ArchaeItems.ETHEREAL_SHARD, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> ETHEREAL_KYLIX = artifact("ethereal_kylix", DEFAULT,
            ArchaeItems.ETHEREAL_SHARD, 1, p -> new FurnitureBlock(p, true, Form.FULL_DECORATION));
    public static final DeferredBlock<FurnitureBlock> ETHEREAL_LEKYTHOS = artifact("ethereal_lekythos", DEFAULT,
            ArchaeItems.ETHEREAL_SHARD, 1, p -> new FurnitureBlock(p, true, Form.FULL_DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> GLISTENING_HARP = artifact("glistening_harp", DEFAULT,
            ArchaeItems.GLISTENING_CURIO, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> GLISTENING_LAMP = artifact("glistening_lamp", DEFAULT,
            ArchaeItems.GLISTENING_CURIO, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> GLISTENING_HEAP = artifact("glistening_heap", DEFAULT,
            ArchaeItems.GLISTENING_CURIO, 2, p -> new FurnitureBlock(p, true, Form.FULL_DECORATION));
    public static final DeferredBlock<AnimatedFurnitureBlock> EMPYREAN_FLAG = artifact("empyrean_flag", TORCH,
            ArchaeItems.CLOUD_CLOTH, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", true, "flag_sway", ArchaeArtifacts.EMPYREAN_FLAG_WALL));
    public static final DeferredBlock<FacingFurnitureBlock> EMPYREAN_FLAG_WALL = variant("empyrean_flag_wall", TORCH,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.EMPYREAN_FLAG));
    public static final DeferredBlock<FacingFurnitureBlock> EMPYREAN_BANNER_LARGE_WALL = variant("empyrean_banner_large_wall", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.EMPYREAN_BANNER_LARGE_POST));
    public static final DeferredBlock<FacingFurnitureBlock> EMPYREAN_BANNER_LARGE_CEILING = variant("empyrean_banner_large_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.EMPYREAN_BANNER_LARGE_POST));
    public static final DeferredBlock<FurnitureBlock> EMPYREAN_BANNER_LARGE_POST = artifact("empyrean_banner_large_post", TORCH,
            ArchaeItems.CLOUD_CLOTH, 3,
            p -> new FurnitureBlock(p, true, Form.FOUR_TALL, ArchaeArtifacts.EMPYREAN_BANNER_LARGE_WALL, ArchaeArtifacts.EMPYREAN_BANNER_LARGE_CEILING, null));
    public static final DeferredBlock<FurnitureBlock> EXQUISITE_RUG = artifact("exquisite_rug", DECORATION,
            ArchaeItems.CLOUD_CLOTH, 3, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> EMPYREAN_BANNER = artifact("empyrean_banner", WOODEN,
            ArchaeItems.CLOUD_CLOTH, 2,
            p -> new FurnitureBlock(p, true, Form.WALL_MOUNTED, null, ArchaeArtifacts.EMPYREAN_BANNER_CEILING, null));
    public static final DeferredBlock<FacingFurnitureBlock> EMPYREAN_BANNER_CEILING = variant("empyrean_banner_ceiling", WOODEN,
            p -> new FacingFurnitureBlock(p, true, ArchaeArtifacts.EMPYREAN_BANNER));
    public static final DeferredBlock<FacingFurnitureBlock> BLUEBERRY_BASKET = artifact("blueberry_basket", HARD_WOODEN,
            ArchaeItems.TWINING_ROOTS, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SKYROOT_CRATE = artifact("skyroot_crate", HARD_WOODEN,
            ArchaeItems.TWINING_ROOTS, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> SKYROOT_CRATE_LARGE = artifact("skyroot_crate_large", HARD_WOODEN,
            ArchaeItems.TWINING_ROOTS, 1, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<SeatFurnitureBlock> SKYFARER_TRIREME = artifact("skyfarer_trireme", HARD_WOODEN,
            ArchaeItems.TWINING_ROOTS, 6, p -> new SeatFurnitureBlock(p, true, 6));
    public static final DeferredBlock<AnimatedFurnitureBlock> ELYSIAN_APPARATUS = artifact("elysian_apparatus", DEFAULT_LUMINANCE,
            ArchaeItems.MYSTICAL_MINERAL, 3, p -> new AnimatedFurnitureBlock(p, "archaeology", false, "obelisk_rotating", null));
    public static final DeferredBlock<AnimatedFurnitureBlock> ELYSIAN_PRISM = artifact("elysian_prism", DEFAULT_LUMINANCE,
            ArchaeItems.MYSTICAL_MINERAL, 4, p -> new AnimatedFurnitureBlock(p, "archaeology", false, "obelisk_rotating", null));
    public static final DeferredBlock<FurnitureBlock> MOA_FIGURE = artifact("moa_figure", POT,
            ArchaeItems.SWIFT_CERAMIC, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> PHYG_VESSEL = artifact("phyg_vessel", POT,
            ArchaeItems.SWIFT_CERAMIC, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FurnitureBlock> SHEEPUFF_RHYTON = artifact("sheepuff_rhyton", POT,
            ArchaeItems.SWIFT_CERAMIC, 1, p -> new FurnitureBlock(p, true, Form.DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> HOLYSTONE_COLUMN_WALL = variant("holystone_column_wall", DEFAULT,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.HOLYSTONE_COLUMN));
    public static final DeferredBlock<FurnitureBlock> HOLYSTONE_COLUMN = artifact("holystone_column", DEFAULT,
            ArchaeItems.HOLY_STONES, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.HOLYSTONE_COLUMN_WALL, null, null));
    public static final DeferredBlock<FurnitureBlock> HOLYSTONE_FINIAL = artifact("holystone_finial", DEFAULT,
            ArchaeItems.HOLY_STONES, 2, p -> new FurnitureBlock(p, false, Form.FULL_DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> HOLYSTONE_FOUNTAIN_WALL = variant("holystone_fountain_wall", DEFAULT,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.HOLYSTONE_FOUNTAIN));
    public static final DeferredBlock<FurnitureBlock> HOLYSTONE_FOUNTAIN = artifact("holystone_fountain", DEFAULT,
            ArchaeItems.HOLY_STONES, 4,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.HOLYSTONE_FOUNTAIN_WALL, null, null));
    public static final DeferredBlock<FacingFurnitureBlock> HOLYSTONE_PEDESTAL = artifact("holystone_pedestal", DEFAULT,
            ArchaeItems.HOLY_STONES, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> EMPYREAN_ENVOY_SCULPTURE = artifact("empyrean_envoy_sculpture", DEFAULT,
            ArchaeItems.HOLY_STONES, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> DIVINE_BRAZIER = artifact("divine_brazier", DEFAULT_LUMINANCE,
            ArchaeItems.DIVINE_DEW, 3, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> DIVINE_LAMP = artifact("divine_lamp", DEFAULT_LUMINANCE,
            ArchaeItems.DIVINE_DEW, 1, p -> new FurnitureBlock(p, false, Form.FULL_DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> DIVINE_TORCH_WALL = variant("divine_torch_wall", LANTERN,
            p -> new FacingFurnitureBlock(p, false, ArchaeArtifacts.DIVINE_TORCH));
    public static final DeferredBlock<FurnitureBlock> DIVINE_TORCH = artifact("divine_torch", LANTERN,
            ArchaeItems.DIVINE_DEW, 2,
            p -> new FurnitureBlock(p, false, Form.FURNITURE, ArchaeArtifacts.DIVINE_TORCH_WALL, null, null));
    public static final DeferredBlock<CeilingFurnitureBlock> DIVINE_LANTERN = artifact("divine_lantern", LANTERN,
            ArchaeItems.DIVINE_DEW, 2, p -> new CeilingFurnitureBlock(p, false));
    public static final DeferredBlock<FacingFurnitureBlock> BLUEBERRY_PLANTER = artifact("blueberry_planter", DEFAULT,
            ArchaeItems.ICESTONE_PEBBLES, 2, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> ICESTONE_FINIAL = artifact("icestone_finial", DECORATION,
            ArchaeItems.ICESTONE_PEBBLES, 2, p -> new FurnitureBlock(p, false, Form.FULL_DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_SKYROOT_SAPLING = artifact("potted_skyroot_sapling", DEFAULT,
            ArchaeItems.ICESTONE_PEBBLES, 3, p -> new FacingFurnitureBlock(p, true, null));
    public static final DeferredBlock<FacingFurnitureBlock> SKYFARER_RELIEF = artifact("skyfarer_relief", DEFAULT,
            ArchaeItems.ICESTONE_PEBBLES, 8, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCIENT_ALTAR = artifact("ancient_altar", DEFAULT,
            ArchaeItems.FATED_FRAGMENTS, 6, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> TEMPLE_BONFIRE = artifact("temple_bonfire", DEFAULT,
            ArchaeItems.FATED_FRAGMENTS, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FurnitureBlock> TEMPLE_TOTEM = artifact("temple_totem", DECORATION,
            ArchaeItems.GRIMY_GOLD, 1, p -> new FurnitureBlock(p, true, Form.FULL_DECORATION));
    public static final DeferredBlock<FacingFurnitureBlock> FLOURISHING_MONUMENT = artifact("flourishing_monument", DEFAULT,
            ArchaeItems.GRIMY_GOLD, 8, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> WILD_STATUE = artifact("wild_statue", DEFAULT,
            ArchaeItems.MOSSY_REMNANTS, 4, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> ANCIENT_ARBOR = artifact("ancient_arbor", DEFAULT_NO_COLLISION,
            ArchaeItems.MOSSY_REMNANTS, 10, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<FacingFurnitureBlock> PRIMITIVE_TORCH = artifact("primitive_torch", TORCH,
            ArchaeItems.SWASHED_RAGS, 2, p -> new FacingFurnitureBlock(p, false, null));
    public static final DeferredBlock<CeilingFurnitureBlock> PRIMITIVE_LANTERN = artifact("primitive_lantern", LANTERN,
            ArchaeItems.SWASHED_RAGS, 2, p -> new CeilingFurnitureBlock(p, false));

    private ArchaeArtifacts() {}

    private static <B extends FurnitureBlock> DeferredBlock<B> artifact(String name, Supplier<BlockBehaviour.Properties> properties,
            DeferredItem<FragmentItem> fragment, int cost, Function<BlockBehaviour.Properties, ? extends B> factory) {
        return register(name, properties, fragment, cost, factory);
    }

    private static <B extends FurnitureBlock> DeferredBlock<B> variant(String name, Supplier<BlockBehaviour.Properties> properties,
            Function<BlockBehaviour.Properties, ? extends B> factory) {
        return register(name, properties, null, 0, factory);
    }

    private static <B extends FurnitureBlock> DeferredBlock<B> register(String name, Supplier<BlockBehaviour.Properties> properties,
            @Nullable DeferredItem<FragmentItem> fragment, int cost, Function<BlockBehaviour.Properties, ? extends B> factory) {
        ApeiraRegistrar r = ApeiraArchae.REGISTRAR;
        Supplier<BlockBehaviour.Properties> withParts = () -> FurnitureFootprints.properties(name, properties.get());
        DeferredBlock<B> block = r.blocks().registerBlock(name, factory, withParts);
        DeferredItem<FurnitureBlockItem> item = r.items().registerItem(name, p -> new FurnitureBlockItem(block.get(), p),
                p -> p.useBlockDescriptionPrefix().component(DataComponents.LORE, EXCLUSIVE_LORE));
        ALL.add(new Artifact(block, item, fragment, cost));
        return block;
    }

    /** Every artifact and variant, in Argon's order. */
    public static List<Artifact> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** The artifacts matching {@code filter}. */
    public static List<Artifact> matching(Predicate<Artifact> filter) {
        return ALL.stream().filter(filter).toList();
    }

    public static void init(ApeiraRegistrar registrar) {}

    /**
     * An artifact's block, item and assembly recipe.
     *
     * @param fragment the fragment the assembly table makes it from, or {@code null} for a variant
     * @param cost     how many fragments it takes
     */
    public record Artifact(DeferredBlock<? extends FurnitureBlock> block, DeferredItem<FurnitureBlockItem> item,
            @Nullable DeferredItem<FragmentItem> fragment, int cost) {
        /** Whether this is a wall, ceiling or water variant, which has no recipe and no place in the creative tab. */
        public boolean variant() {
            return fragment == null;
        }
    }
}
