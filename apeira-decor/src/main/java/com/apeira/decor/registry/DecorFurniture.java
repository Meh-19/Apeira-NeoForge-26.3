package com.apeira.decor.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.furniture.AnimatedCeilingFurnitureBlock;
import com.apeira.decor.furniture.AnimatedFurnitureBlock;
import com.apeira.decor.furniture.AnimatedWallFurnitureBlock;
import com.apeira.decor.furniture.FacingFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock.Form;
import com.apeira.decor.furniture.DisplayMode;
import com.apeira.decor.furniture.FurnitureBlockItem;
import com.apeira.decor.furniture.FurnitureFootprints;
import com.apeira.decor.furniture.SeatFurnitureBlock;
import com.apeira.decor.furniture.WallFurnitureBlock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Decor's furniture pieces, on the one furniture system ({@link FurnitureBlock}, the same as archaeology's artifacts):
 * each a block with a {@link FurnitureBlockItem} of the same name, drawn from {@code models/item/furniture/<name>.json}
 * by {@code FurnitureRenderer} as its {@link Form}'s display mode lays it in, with its footprint from
 * {@code apeira_decor/furniture_footprints.txt}. A piece exclusive to a crate names it in gold under its name
 * ({@code tooltip.apeira.exclusive.<crate>}).
 *
 * <p>Default paints are the uicapture's firework colours: the rocking chair #A0643E, the wedge tent #93BA4F, the sash
 * banner #FA9B5C (the stall cart and windmill are white, so none; the wooden cart is a paper item, so none).
 *
 * <p>The pieces are the Badger Crate's, the Origin Realms server pack's {@code models/custom/furniture/badger/} models
 * (names from Ren's uicapture of the {@code /catalog} "Reward Furniture" pages). Its {@code fixed} display picks the
 * mode: rotation x 90 is drawn on the top face ({@link Form#FURNITURE}), -90 on the floor ({@link Form#FLOOR}), as
 * Argon's models are. A piece takes paint when its model has tint index 1 faces (all but the wooden cart); the
 * windmill's GeckoLib model paints its {@code _tintable} bones (sails and tail fin).
 */
public final class DecorFurniture {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;
    private static final List<DeferredBlock<? extends FurnitureBlock>> ALL = new ArrayList<>();
    /** The bathtub's two seats, x, y, z in pixels at turn 0 (see {@link SeatFurnitureBlock#seats(double...)}). */
    private static final double[] SEATS = {-2, 4, 0, 18, 4, 0};
    /** The garden bench's seats at turn 0 ({@code x, y, z} pixels), one over each of the three blocks its footprint spans. */
    private static final double[] BENCH_SEATS = {-8, 8, 8, 8, 8, 8, 24, 8, 8};

    public static final DeferredBlock<SeatFurnitureBlock> OUTDOOR_ROCKING_CHAIR = piece("outdoor_rocking_chair", "badger_crate", 0xA0643E,
            p -> new SeatFurnitureBlock(p, true, 8));
    /** Offsets are Ren's furniture wand exports. */
    public static final DeferredBlock<FurnitureBlock> WEDGE_TENT = piece("wedge_tent", "badger_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FURNITURE).renderOffset(0, 16, 0));
    /** On the floor, or against a wall as {@link #SASH_BANNER_WALL} (the pack's {@code sash_banner_wall} model). */
    public static final DeferredBlock<FurnitureBlock> SASH_BANNER = piece("sash_banner", "badger_crate", 0xFA9B5C,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.SASH_BANNER_WALL.get(), null, null).renderOffset(0, 32, 0));
    public static final DeferredBlock<FacingFurnitureBlock> SASH_BANNER_WALL = piece("sash_banner_wall", "badger_crate", 0xFA9B5C,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.SASH_BANNER.get()));
    public static final DeferredBlock<FurnitureBlock> WOODEN_CART = piece("wooden_cart", "badger_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> STALL_CART = piece("stall_cart", "badger_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's GeckoLib model, {@code geckolib/models/furniture/wooden_windmill.geo.json}, its sails turning, turned round
     * (the model faces the other way from the rest). */
    public static final DeferredBlock<AnimatedFurnitureBlock> WOODEN_WINDMILL = piece("wooden_windmill", "badger_crate",
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "wooden_windmill", null).modelYaw(180));


    // The Animal Crate's furniture: the server pack's models/custom/furniture/animal/ models, names, rarities and paints
    // from Ren's F9 capture of the Animal Crate's reward page ("Next Cycle"). All lie on the floor (fixed display
    // rotation x -90) but the cat clock, which hangs on a wall, and the monkey's wall and ceiling variants.
    public static final DeferredBlock<FurnitureBlock> CAPYBARA_PLUSHIE = piece("capybara_plushie", "animal_crate", 0xA0643E,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CAT_PLUSHIE = piece("cat_plushie", "animal_crate", 0xE88438,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> DOG_PLUSHIE = piece("dog_plushie", "animal_crate", 0xFA9B5C,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** A seat; its height (6 pixels) is a guess until tuned with the furniture wand. */
    public static final DeferredBlock<SeatFurnitureBlock> FROGGY_CHAIR = piece("froggy_chair", "animal_crate", 0x93BA4F,
            p -> new SeatFurnitureBlock(p, true, Form.FLOOR, 6));
    public static final DeferredBlock<FurnitureBlock> GIRAFFE_PLUSHIE = piece("giraffe_plushie", "animal_crate", 0xF89D25,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /**
     * The pack's {@code monkey_plushie_sitting} model on the floor; clicked against a wall it is {@link #MONKEY_PLUSHIE_WALL}
     * ({@code monkey_plushie_wall}), under a block {@link #MONKEY_PLUSHIE_CEILING} ({@code monkey_plushie_hanging}).
     */
    public static final DeferredBlock<FurnitureBlock> MONKEY_PLUSHIE = piece("monkey_plushie", "animal_crate", 0xA0643E,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MONKEY_PLUSHIE_WALL.get(),
                    () -> DecorFurniture.MONKEY_PLUSHIE_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> MONKEY_PLUSHIE_WALL = piece("monkey_plushie_wall", "animal_crate", 0xA0643E,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MONKEY_PLUSHIE.get()));
    public static final DeferredBlock<FacingFurnitureBlock> MONKEY_PLUSHIE_CEILING = piece("monkey_plushie_ceiling", "animal_crate", 0xA0643E,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MONKEY_PLUSHIE.get()));
    /**
     * Animated: Ren's model ({@code geckolib/models/furniture/cat_clock.geo.json}, from the armor stand recording), its
     * tail swinging and eyes looking left and right. Only on a wall, its Blockbench north towards the player.
     */
    public static final DeferredBlock<AnimatedWallFurnitureBlock> CAT_CLOCK = piece("cat_clock", "animal_crate", 0xE88438,
            p -> new AnimatedWallFurnitureBlock(p, "furniture", true, "animation.cat_clock.tick"));
    /**
     * Animated: Ren's model ({@code geckolib/models/furniture/elephant_inflatable.geo.json}, from the armor stand
     * recording, moved so its body stands on its block), bobbing; its hitbox is only its body (the script's
     * {@code HITBOX_BONES}). Turned a quarter so its head points the way the pack's still model's does.
     */
    public static final DeferredBlock<AnimatedFurnitureBlock> ELEPHANT_INFLATABLE = piece("elephant_inflatable", "animal_crate", 0xB0DFE5,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.elephant_inflatable.bob", null).modelYaw(-90));


    // The Bathroom Crate's furniture: the server pack's models/custom/furniture/bathroom/ models, names, rarities and
    // paints from Ren's F9 capture of the Bathroom Crate's reward page (the cabinet was not on it). Floor pieces with a
    // wall variant place it when clicked against a wall; wall pieces (WallFurnitureBlock) hang only on a wall or, the
    // lamp and shower head, under a block as their ceiling variant (the pack's shower_head model). Variants drop their
    // base piece and are not in the creative tab.
    public static final DeferredBlock<FurnitureBlock> BATHROOM_BIN = piece("bathroom_bin", "bathroom_crate", 0xA0643E,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> BATHROOM_CABINET = piece("bathroom_cabinet", "bathroom_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.BATHROOM_CABINET_WALL.get(), null, null).renderOffset(0, 16, 0));
    public static final DeferredBlock<FacingFurnitureBlock> BATHROOM_CABINET_WALL = piece("bathroom_cabinet_wall", "bathroom_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BATHROOM_CABINET.get()));
    /** The pack's bathroom_lights_wall; under a block, bathroom_lights_ceiling. */
    public static final DeferredBlock<WallFurnitureBlock> BATHROOM_LAMP = piece("bathroom_lamp", "bathroom_crate", 0xB2B2B2,
            p -> new WallFurnitureBlock(p, true, () -> DecorFurniture.BATHROOM_LAMP_CEILING.get()));
    public static final DeferredBlock<FacingFurnitureBlock> BATHROOM_LAMP_CEILING = piece("bathroom_lamp_ceiling", "bathroom_crate", 0xB2B2B2,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BATHROOM_LAMP.get()));
    public static final DeferredBlock<WallFurnitureBlock> BATHROOM_MIRROR = piece("bathroom_mirror", "bathroom_crate", 0xB2B2B2,
            p -> new WallFurnitureBlock(p, true));
    /** No collision: walked over. */
    public static final DeferredBlock<FurnitureBlock> BATHROOM_RUG = piece("bathroom_rug", "bathroom_crate", 0x74A4CA,
            BlockBehaviour.Properties::noCollision, p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> BATHROOM_SHELF = piece("bathroom_shelf", "bathroom_crate", 0x74A4CA,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.BATHROOM_SHELF_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> BATHROOM_SHELF_WALL = piece("bathroom_shelf_wall", "bathroom_crate", 0x74A4CA,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BATHROOM_SHELF.get()));
    public static final DeferredBlock<FurnitureBlock> BATHROOM_SINK = piece("bathroom_sink", "bathroom_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.BATHROOM_SINK_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> BATHROOM_SINK_WALL = piece("bathroom_sink_wall", "bathroom_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BATHROOM_SINK.get()));
    public static final DeferredBlock<FurnitureBlock> BATHROOM_TOWELS = piece("bathroom_towels", "bathroom_crate", 0x74A4CA,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.BATHROOM_TOWELS_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> BATHROOM_TOWELS_WALL = piece("bathroom_towels_wall", "bathroom_crate", 0x74A4CA,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BATHROOM_TOWELS.get()).renderOffset(0, 6, 0));
    /** Two seats in the tub, from its footprint: SEATS. */
    public static final DeferredBlock<SeatFurnitureBlock> BATHTUB = piece("bathtub", "bathroom_crate",
            p -> new SeatFurnitureBlock(p, true, 4).seats(SEATS));
    /** The pack's shower_head_wall; under a block, the pack's shower_head. */
    public static final DeferredBlock<WallFurnitureBlock> SHOWER_HEAD = piece("shower_head", "bathroom_crate", 0xB2B2B2,
            p -> new WallFurnitureBlock(p, true, () -> DecorFurniture.SHOWER_HEAD_CEILING.get()));
    public static final DeferredBlock<FacingFurnitureBlock> SHOWER_HEAD_CEILING = piece("shower_head_ceiling", "bathroom_crate", 0xB2B2B2,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.SHOWER_HEAD.get()));
    public static final DeferredBlock<FurnitureBlock> TOILET = piece("toilet", "bathroom_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> TOILET_PAPER = piece("toilet_paper", "bathroom_crate", 0xB2B2B2,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.TOILET_PAPER_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> TOILET_PAPER_WALL = piece("toilet_paper_wall", "bathroom_crate", 0xB2B2B2,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.TOILET_PAPER.get()));
    public static final DeferredBlock<FurnitureBlock> TOILETRIES_RACK = piece("toiletries_rack", "bathroom_crate", 0xB2B2B2,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.TOILETRIES_RACK_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> TOILETRIES_RACK_WALL = piece("toiletries_rack_wall", "bathroom_crate", 0xB2B2B2,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.TOILETRIES_RACK.get()));
    public static final DeferredBlock<FurnitureBlock> WASHING_MACHINE = piece("washing_machine", "bathroom_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));

    // The Business Crate's furniture: the server pack's models/custom/furniture/business/ models, names, rarities and
    // paints from Ren's F9 capture of the Business Crate's reward page (ids are the captured names; the pack's
    // filling_cabinet, cubical and office_supply are the filing cabinet, cubicle and office supplies). The pack's
    // business case, paper bin, security camera and ventilator are not on the capture and are left out. The ceiling fan
    // and revolving door are animated, from Ren's GeckoLib models; their blades and doors are tagged _ignore, so the
    // footprint generator gives them no hitbox. Paintable where the pack model has tint index 1 faces (all of them).
    public static final DeferredBlock<FurnitureBlock> OFFICE_SUPPLIES = piece("office_supplies", "business_crate", 0x666666,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** The pack's fluorescent_light_wall, lying as the pack draws it (rotation x 90, a top face piece); under a block, fluorescent_light_ceiling. */
    public static final DeferredBlock<WallFurnitureBlock> FLUORESCENT_LIGHT = piece("fluorescent_light", "business_crate", 0x666666,
            p -> new WallFurnitureBlock(p, true, () -> DecorFurniture.FLUORESCENT_LIGHT_CEILING.get()).displayAs(DisplayMode.TOP_FACE)
                    .modelYaw(90));
    public static final DeferredBlock<FacingFurnitureBlock> FLUORESCENT_LIGHT_CEILING = piece("fluorescent_light_ceiling", "business_crate", 0x666666,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.FLUORESCENT_LIGHT.get()));
    public static final DeferredBlock<FurnitureBlock> LAPTOP = piece("laptop", "business_crate", 0x999999,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** A seat 7 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> OFFICE_CHAIR = piece("office_chair", "business_crate", 0x666666,
            p -> new SeatFurnitureBlock(p, true, 7));
    public static final DeferredBlock<FurnitureBlock> DESK_LAMP = piece("desk_lamp", "business_crate", 0x999999,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<WallFurnitureBlock> OFFICE_BLINDS = piece("office_blinds", "business_crate", 0x999999,
            p -> new WallFurnitureBlock(p, true));
    public static final DeferredBlock<FurnitureBlock> OFFICE_DESK = piece("office_desk", "business_crate", 0x999999,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> FILING_CABINET = piece("filing_cabinet", "business_crate", 0x666666,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model, its blades (bone_tintable_ignore) turning once every 3.5 seconds; only under a block. */
    public static final DeferredBlock<AnimatedCeilingFurnitureBlock> OFFICE_CEILING_FAN = piece("office_ceiling_fan", "business_crate",
            p -> new AnimatedCeilingFurnitureBlock(p, "furniture", true, "animation.office_ceiling_fan.spin"));
    public static final DeferredBlock<FurnitureBlock> MONITOR = piece("monitor", "business_crate", 0x999999,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MONITOR_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> MONITOR_WALL = piece("monitor_wall", "business_crate", 0x999999,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MONITOR.get()));
    public static final DeferredBlock<FurnitureBlock> PRINTING_MACHINE = piece("printing_machine", "business_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> BILLBOARD = piece("billboard", "business_crate", 0x547483,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.BILLBOARD_WALL.get(), null, null).renderOffset(0, 16, 0));
    public static final DeferredBlock<FacingFurnitureBlock> BILLBOARD_WALL = piece("billboard_wall", "business_crate", 0x547483,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BILLBOARD.get()));
    public static final DeferredBlock<FurnitureBlock> CUBICLE = piece("cubicle", "business_crate", 0x999999,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model, its doors (bone2_tintable_ignore, bone2_ignore) turning once every 9 seconds. */
    public static final DeferredBlock<AnimatedFurnitureBlock> REVOLVING_DOOR = piece("revolving_door", "business_crate", 0x999999,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.revolving_door.spin", null));

    // The Camping Crate's furniture: the server pack's models/custom/furniture/camping/ models, names, rarities and paints
    // from Ren's F9 capture of the Camping Crate's reward page (the pack's log_chair, log_table, log_bed, young_cyprus and
    // big_hammock are the cabin chair, table and bed, young cypress and large hammock). None is animated (the bonfire's
    // and fireplace's flames are animated textures of still models). Each floor piece's pack variants are its wall and
    // ceiling variants, which drop it and are never crate rewards (crate_loot lists only base pieces, BadgerLoot maps
    // variants to their base and camping.json excludes animated furniture).
    public static final DeferredBlock<FurnitureBlock> CABIN_LAMP = piece("cabin_lamp", "camping_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.CABIN_LAMP_WALL.get(), () -> DecorFurniture.CABIN_LAMP_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> CABIN_LAMP_WALL = piece("cabin_lamp_wall", "camping_crate", 0x93BA4F,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CABIN_LAMP.get()));
    public static final DeferredBlock<FacingFurnitureBlock> CABIN_LAMP_CEILING = piece("cabin_lamp_ceiling", "camping_crate", 0x93BA4F,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CABIN_LAMP.get()));
    public static final DeferredBlock<FurnitureBlock> POTTED_FOREST_FERN = piece("potted_forest_fern", "camping_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FLOOR, null, () -> DecorFurniture.POTTED_FOREST_FERN_CEILING.get(), null));
    /** The pack's ceiling model lies like a floor piece (rotation x -90). */
    public static final DeferredBlock<FacingFurnitureBlock> POTTED_FOREST_FERN_CEILING = piece("potted_forest_fern_ceiling", "camping_crate", 0x93BA4F,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.POTTED_FOREST_FERN.get()).displayAs(DisplayMode.FLOOR)
                    .renderOffset(0, -32, 0));
    /** A seat 8 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> CABIN_CHAIR = piece("cabin_chair", "camping_crate", 0xCB5148,
            p -> new SeatFurnitureBlock(p, true, Form.FLOOR, 8));
    /** The pack's deer_mount_floor (no tint index, so it shows no paint) and, against a wall, deer_mount_wall. */
    public static final DeferredBlock<FurnitureBlock> DEER_MOUNT = piece("deer_mount", "camping_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.DEER_MOUNT_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> DEER_MOUNT_WALL = piece("deer_mount_wall", "camping_crate", 0x93BA4F,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.DEER_MOUNT.get()));
    public static final DeferredBlock<FurnitureBlock> CABIN_TABLE = piece("cabin_table", "camping_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> YOUNG_CYPRESS = piece("young_cypress", "camping_crate", 0x647A4C,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CABIN_BED = piece("cabin_bed", "camping_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> WOODEN_CANOE = piece("wooden_canoe", "camping_crate", 0x884B44,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.WOODEN_CANOE_WALL.get(), () -> DecorFurniture.WOODEN_CANOE_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> WOODEN_CANOE_WALL = piece("wooden_canoe_wall", "camping_crate", 0x884B44,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.WOODEN_CANOE.get()));
    public static final DeferredBlock<FacingFurnitureBlock> WOODEN_CANOE_CEILING = piece("wooden_canoe_ceiling", "camping_crate", 0x884B44,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.WOODEN_CANOE.get()));
    public static final DeferredBlock<FurnitureBlock> LARGE_HAMMOCK = piece("large_hammock", "camping_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.LARGE_HAMMOCK_WALL.get(), () -> DecorFurniture.LARGE_HAMMOCK_CEILING.get(), null));
    /** The pack's wall model stands like the floor one (rotation x -90), turned by its wall. */
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_HAMMOCK_WALL = piece("large_hammock_wall", "camping_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.LARGE_HAMMOCK.get()).displayAs(DisplayMode.FLOOR));
    public static final DeferredBlock<FacingFurnitureBlock> LARGE_HAMMOCK_CEILING = piece("large_hammock_ceiling", "camping_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.LARGE_HAMMOCK.get()).renderOffset(0, -64, 0));
    public static final DeferredBlock<FurnitureBlock> CABIN_FIREPLACE = piece("cabin_fireplace", "camping_crate", 0xAC5353,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.CABIN_FIREPLACE_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> CABIN_FIREPLACE_WALL = piece("cabin_fireplace_wall", "camping_crate", 0xAC5353,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CABIN_FIREPLACE.get()));
    /** A paper item on the capture, with no tint index faces: not paintable. */
    public static final DeferredBlock<FurnitureBlock> BONFIRE = piece("bonfire", "camping_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CAMPERVAN = piece("campervan", "camping_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));

    // The Construction Crate's furniture: the server pack's models/custom/furniture/construction/ models, names, rarities
    // and paints from Ren's F9 capture of the Construction Crate's reward page (ids are the captured names; the pack's
    // ladder, wheelbarrow, tool_box/tool_rack, wood_pallet_floor/_wall and shipping_container are the construction
    // ladder, wheelbarrow and tools, pallet stack and storage unit). The pack's cinderblocks, construction barrel, lights,
    // sign and jackhammer are not on the capture and are left out. The cement mixer and construction crane are animated,
    // from Ren's armor stand recordings (the mixer's _paintable bones renamed _tintable). Display modes follow each pack
    // model's fixed display (rotation x -90 a floor piece, 90 a top face piece, none a wall piece); the pallet stack has no
    // tinted faces, so it takes no paint.
    public static final DeferredBlock<FurnitureBlock> CONSTRUCTION_LADDER = piece("construction_ladder", "construction_crate", 0xE7A127,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.CONSTRUCTION_LADDER_WALL.get(), null, null));
    /** The pack's wall model stands like the floor one (rotation x -90), turned by its wall. */
    public static final DeferredBlock<FacingFurnitureBlock> CONSTRUCTION_LADDER_WALL = piece("construction_ladder_wall", "construction_crate", 0xE7A127,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CONSTRUCTION_LADDER.get()).displayAs(DisplayMode.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CONSTRUCTION_WHEELBARROW = piece("construction_wheelbarrow", "construction_crate", 0xE7A127,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** The tool box on the floor; against a wall, the pack's tool rack. */
    public static final DeferredBlock<FurnitureBlock> CONSTRUCTION_TOOLS = piece("construction_tools", "construction_crate", 0xE7A127,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.CONSTRUCTION_TOOLS_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> CONSTRUCTION_TOOLS_WALL = piece("construction_tools_wall", "construction_crate", 0xE7A127,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CONSTRUCTION_TOOLS.get()));
    public static final DeferredBlock<FurnitureBlock> PALLET_STACK = piece("pallet_stack", "construction_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR, () -> DecorFurniture.PALLET_STACK_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> PALLET_STACK_WALL = piece("pallet_stack_wall", "construction_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.PALLET_STACK.get()).displayAs(DisplayMode.FLOOR));
    public static final DeferredBlock<FurnitureBlock> I_BEAM = piece("i_beam", "construction_crate", 0x999999,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.I_BEAM_WALL.get(), () -> DecorFurniture.I_BEAM_CEILING.get(), null));
    /** The pack's wall model is turned (rotation -90, 90, -90); drawn as a floor-style display (a guess until tried). */
    public static final DeferredBlock<FacingFurnitureBlock> I_BEAM_WALL = piece("i_beam_wall", "construction_crate", 0x999999,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.I_BEAM.get()).displayAs(DisplayMode.FLOOR));
    public static final DeferredBlock<FacingFurnitureBlock> I_BEAM_CEILING = piece("i_beam_ceiling", "construction_crate", 0x999999,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.I_BEAM.get()));
    public static final DeferredBlock<FurnitureBlock> METAL_SCAFFOLDING = piece("metal_scaffolding", "construction_crate", 0x4C4C4C,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.METAL_SCAFFOLDING_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> METAL_SCAFFOLDING_WALL = piece("metal_scaffolding_wall", "construction_crate", 0x4C4C4C,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.METAL_SCAFFOLDING.get()));
    public static final DeferredBlock<FurnitureBlock> PORTA_POTTY = piece("porta_potty", "construction_crate", 0x74A4CA,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** Animated: Ren's model, from the armor stand recording, its drum turning. */
    public static final DeferredBlock<AnimatedFurnitureBlock> CEMENT_MIXER = piece("cement_mixer", "construction_crate", 0xE7A127,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.cement_mixer.spin", null));
    /** The pack's shipping container. */
    public static final DeferredBlock<FurnitureBlock> STORAGE_UNIT = piece("storage_unit", "construction_crate", 0xB2B2B2,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> FORKLIFT = piece("forklift", "construction_crate", 0xE7A127,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> EXCAVATOR = piece("excavator", "construction_crate", 0xE7A127,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** Animated: Ren's model, from the armor stand recording; its _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> CONSTRUCTION_CRANE = piece("construction_crane", "construction_crate", 0xE7A127,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.construction_crane.loop", null));

    // The Fast Food Crate's furniture: the server pack's models/custom/furniture/fast_food/ models, names, rarities and
    // paints from Ren's F9 capture of the Fast Food Crate's reward page (the pack's menu_screen_floor is the menu screen).
    // The pack's food stall and restaurant stool are not on the capture and are left out. The burger mascot inflatable is
    // animated, from Ren's armor stand recording; its _ignore bones (head, arms, legs) get no hitbox. Display modes
    // follow each pack model's fixed display (rotation x -90 a floor piece, 90 a top face piece, a wall model drawn on its
    // wall with its own fixed turn).
    public static final DeferredBlock<FurnitureBlock> CONDIMENT_CADDY = piece("condiment_caddy", "fast_food_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> BURGER_TRAY = piece("burger_tray", "fast_food_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CONE_LIGHT = piece("cone_light", "fast_food_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.CONE_LIGHT_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> CONE_LIGHT_WALL = piece("cone_light_wall", "fast_food_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CONE_LIGHT.get()));
    public static final DeferredBlock<FurnitureBlock> RESTAURANT_TABLE = piece("restaurant_table", "fast_food_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> HOTDOG_TRAY = piece("hotdog_tray", "fast_food_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> DONUT_BOX = piece("donut_box", "fast_food_crate", 0xFB8EB0,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** A seat 7 pixels up (a guess until tried), drawn on the floor as the pack's booth lies (rotation x -90). */
    public static final DeferredBlock<SeatFurnitureBlock> RESTAURANT_BOOTH = piece("restaurant_booth", "fast_food_crate", 0xCB5148,
            p -> new SeatFurnitureBlock(p, true, Form.FLOOR, 7));
    public static final DeferredBlock<FurnitureBlock> MENU_SCREEN = piece("menu_screen", "fast_food_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MENU_SCREEN_WALL.get(), () -> DecorFurniture.MENU_SCREEN_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> MENU_SCREEN_WALL = piece("menu_screen_wall", "fast_food_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MENU_SCREEN.get()));
    public static final DeferredBlock<FacingFurnitureBlock> MENU_SCREEN_CEILING = piece("menu_screen_ceiling", "fast_food_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MENU_SCREEN.get()));
    public static final DeferredBlock<FurnitureBlock> VENDING_MACHINE = piece("vending_machine", "fast_food_crate", 0xB2B2B2,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> BURGER_SIGNAGE = piece("burger_signage", "fast_food_crate", 0xCB3C2A,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.BURGER_SIGNAGE_WALL.get(), null, null));
    /** The pack's wall model leans out 22.5 degrees, as it is drawn on the wall. */
    public static final DeferredBlock<FacingFurnitureBlock> BURGER_SIGNAGE_WALL = piece("burger_signage_wall", "fast_food_crate", 0xCB3C2A,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.BURGER_SIGNAGE.get()));
    public static final DeferredBlock<FurnitureBlock> HOTDOG_SIGNAGE = piece("hotdog_signage", "fast_food_crate", 0xF7C021,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.HOTDOG_SIGNAGE_WALL.get(), null, null));
    /** The pack's wall model is turned (rotation -90, 135, 0); drawn on the wall as it is (a guess until tried). */
    public static final DeferredBlock<FacingFurnitureBlock> HOTDOG_SIGNAGE_WALL = piece("hotdog_signage_wall", "fast_food_crate", 0xF7C021,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.HOTDOG_SIGNAGE.get()));
    public static final DeferredBlock<FurnitureBlock> DONUT_SIGNAGE = piece("donut_signage", "fast_food_crate", 0xFB8EB0,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.DONUT_SIGNAGE_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> DONUT_SIGNAGE_WALL = piece("donut_signage_wall", "fast_food_crate", 0xFB8EB0,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.DONUT_SIGNAGE.get()));
    /** Animated: Ren's model, from the armor stand recording; its item is the pack's inflatable base. */
    public static final DeferredBlock<AnimatedFurnitureBlock> BURGER_MASCOT_INFLATABLE = piece("burger_mascot_inflatable", "fast_food_crate", 0xCB3C2A,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.burger_mascot_inflatable.bob", null));

    // The Fun Fair Crate's furniture ("Funfair Crate Exclusive" in the captured #CB5148): the server pack's
    // models/custom/furniture/fun_fair/ models (the spotlight's texture is the pack's jester one, the food stall the
    // fast_food folder's), names, rarities and paints from Ren's F9 capture of the crate's reward page. The pack's funfair
    // sign and snack stand are not on the capture and are left out, as are the capture's Rubber Balloon (no balloon slot)
    // and its gestures. Animated, Ren's GeckoLib models (armor stand recordings cleaned up in Blockbench): the circus flag
    // (floor and wall, with the pack's still flag under a block), the spot light, balloon bundle, tube man and carousel.
    // Display modes follow each pack model's fixed display (rotation x -90 a floor piece, 90 a top face piece, none a wall
    // piece); pieces whose pack model has no tinted faces take no paint.
    /** Animated, swaying; against a wall its animated wall flag, under a block the pack's still ceiling flag. */
    public static final DeferredBlock<AnimatedFurnitureBlock> CIRCUS_FLAG = piece("circus_flag", "fun_fair_crate", 0xCB3C2A,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.circus_flag.wave",
                    () -> DecorFurniture.CIRCUS_FLAG_WALL.get(), () -> DecorFurniture.CIRCUS_FLAG_CEILING.get(), null));
    public static final DeferredBlock<AnimatedWallFurnitureBlock> CIRCUS_FLAG_WALL = piece("circus_flag_wall", "fun_fair_crate", 0xCB3C2A,
            p -> new AnimatedWallFurnitureBlock(p, "furniture", true, "animation.circus_flag_wall.wave", () -> DecorFurniture.CIRCUS_FLAG.get()));
    public static final DeferredBlock<FacingFurnitureBlock> CIRCUS_FLAG_CEILING = piece("circus_flag_ceiling", "fun_fair_crate", 0xCB3C2A,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CIRCUS_FLAG.get()));
    /** The pack's fair_lights_wall, hung on a wall; under a block its fair_lights_ceiling. */
    public static final DeferredBlock<WallFurnitureBlock> FAIR_LIGHTS = piece("fair_lights", "fun_fair_crate", 0xFFC766,
            p -> new WallFurnitureBlock(p, true, () -> DecorFurniture.FAIR_LIGHTS_CEILING.get()));
    public static final DeferredBlock<FacingFurnitureBlock> FAIR_LIGHTS_CEILING = piece("fair_lights_ceiling", "fun_fair_crate", 0xFFC766,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.FAIR_LIGHTS.get()));
    public static final DeferredBlock<FurnitureBlock> COTTON_CANDY_TRAY = piece("cotton_candy_tray", "fun_fair_crate", 0xFFA8DE,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CIRCUS_PODIUM = piece("circus_podium", "fun_fair_crate", 0xCB3C2A,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** The pack's photo_standees (its fixed display tips it 94 degrees: a top face piece). */
    public static final DeferredBlock<FurnitureBlock> PHOTO_STANDEE = piece("photo_standee", "fun_fair_crate", 0xCB3C2A,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> POPCORN_BUCKET = piece("popcorn_bucket", "fun_fair_crate", 0xCB3C2A,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** The pack's fast_food/food_stall, which the Fun Fair Crate gives. */
    public static final DeferredBlock<FurnitureBlock> FOOD_STALL = piece("food_stall", "fun_fair_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** Animated: Ren's model, the light sweeping; the pack's spotlight_whole is its item. No tinted bones, so no paint. */
    public static final DeferredBlock<AnimatedFurnitureBlock> SPOT_LIGHT = piece("spot_light", "fun_fair_crate",
            p -> new AnimatedFurnitureBlock(p, "furniture", false, "animation.spot_light.loop", null));
    public static final DeferredBlock<FurnitureBlock> BUMPER_CAR = piece("bumper_car", "fun_fair_crate", 0x3EB8DD,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model, the balloons bobbing. */
    public static final DeferredBlock<AnimatedFurnitureBlock> BALLOON_BUNDLE = piece("balloon_bundle", "fun_fair_crate", 0xCB3C2A,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.balloon_bundle.float", null));
    public static final DeferredBlock<FurnitureBlock> HIGH_STRIKER = piece("high_striker", "fun_fair_crate", 0xCB3C2A,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model, flailing; its _ignore bones (the tube and arms) get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> TUBE_MAN = piece("tube_man", "fun_fair_crate", 0xCB5148,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.tube_man.dance", null));
    /** The pack's circus_tent (its item model, circus_tent_inventory, is the same model). */
    public static final DeferredBlock<FurnitureBlock> CIRCUS_TENT = piece("circus_tent", "fun_fair_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** Animated: Ren's model, turning; its _ignore horses get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> CAROUSEL = piece("carousel", "fun_fair_crate", 0xCB5148,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.carousel.spin", null));

    // The Garden Crate's furniture ("Garden Crate Exclusive" in the captured #4D7A60): the server pack's
    // models/custom/furniture/garden/ models, names, rarities and paints from Ren's F9 capture of the crate's reward page
    // (the pack's delphinium_potted, garden_lamp_post and green_house are the potted delphiniums, garden lamppost and
    // greenhouse). The pack's garden grass (and its potted and ceiling versions), horse topiary and watering can are not on
    // the capture and are left out, as are its gestures. The garden fountain is animated, Ren's GeckoLib model; its
    // _ignore spouts get no hitbox. The tall hedge, young tree, ivy trellis and ivy pergola have no tinted faces.
    /** Three seats 8 pixels up along its three blocks (a guess until tried): BENCH_SEATS. */
    public static final DeferredBlock<SeatFurnitureBlock> GARDEN_BENCH = piece("garden_bench", "garden_crate", 0x4D7A60,
            p -> new SeatFurnitureBlock(p, true, 8).seats(BENCH_SEATS));
    /** A seat 8 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> GARDEN_CHAIR = piece("garden_chair", "garden_crate", 0xCCCCCC,
            p -> new SeatFurnitureBlock(p, true, 8));
    public static final DeferredBlock<FurnitureBlock> GARDEN_TABLE = piece("garden_table", "garden_crate", 0x4D7A60,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** On the floor the pack's garden_tools; against a wall its garden_tools_wall. */
    public static final DeferredBlock<FurnitureBlock> GARDEN_TOOLS = piece("garden_tools", "garden_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.GARDEN_TOOLS_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> GARDEN_TOOLS_WALL = piece("garden_tools_wall", "garden_crate", 0x93BA4F,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.GARDEN_TOOLS.get()));
    public static final DeferredBlock<FurnitureBlock> POTTED_DELPHINIUMS = piece("potted_delphiniums", "garden_crate", 0xD499FF,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> TALL_HEDGE = piece("tall_hedge", "garden_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> YARD_GNOME = piece("yard_gnome", "garden_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> YOUNG_TREE = piece("young_tree", "garden_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> GARDEN_LAMPPOST = piece("garden_lamppost", "garden_crate", 0x4D7A60,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> HYDRANGEA_BUSH = piece("hydrangea_bush", "garden_crate", 0xFB8EB0,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> IVY_TRELLIS = piece("ivy_trellis", "garden_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> PUSH_MOWER = piece("push_mower", "garden_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> GREENHOUSE = piece("greenhouse", "garden_crate", 0x4D7A60,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> IVY_PERGOLA = piece("ivy_pergola", "garden_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    /** Animated: Ren's model, its water running; the pack's garden_fountain_whole is its item. */
    public static final DeferredBlock<AnimatedFurnitureBlock> GARDEN_FOUNTAIN = piece("garden_fountain", "garden_crate", 0xCCCCCC,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.garden_fountain.loop", null));

    // The Kitchen Crate's furniture ("Kitchen Crate Exclusive"): the server pack's models/custom/furniture/kitchen/ models,
    // names, rarities and paints from Ren's F9 capture of its reward page. Display modes follow each model's fixed display.
    /** A seat 10 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> KITCHEN_STOOL = piece("kitchen_stool", "kitchen_crate", 0xCCCCCC,
            p -> new SeatFurnitureBlock(p, true, Form.FLOOR, 10));
    public static final DeferredBlock<FurnitureBlock> KITCHEN_CABINET = piece("kitchen_cabinet", "kitchen_crate", 0xCCCCCC,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.KITCHEN_CABINET_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> KITCHEN_CABINET_WALL = piece("kitchen_cabinet_wall", "kitchen_crate", 0xCCCCCC,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.KITCHEN_CABINET.get()));
    public static final DeferredBlock<FurnitureBlock> CUTTING_BOARD = piece("cutting_board", "kitchen_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE, () -> DecorFurniture.CUTTING_BOARD_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> CUTTING_BOARD_WALL = piece("cutting_board_wall", "kitchen_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.CUTTING_BOARD.get()));
    public static final DeferredBlock<FurnitureBlock> PAN = piece("pan", "kitchen_crate", 0x333333,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.PAN_WALL.get(), () -> DecorFurniture.PAN_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> PAN_WALL = piece("pan_wall", "kitchen_crate", 0x333333,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.PAN.get()));
    public static final DeferredBlock<FacingFurnitureBlock> PAN_CEILING = piece("pan_ceiling", "kitchen_crate", 0x333333,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.PAN.get()));
    public static final DeferredBlock<FurnitureBlock> BOWL_OF_FRUIT = piece("bowl_of_fruit", "kitchen_crate",
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> UTENSILS_RACK = piece("utensils_rack", "kitchen_crate", 0x666666,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.UTENSILS_RACK_WALL.get(), () -> DecorFurniture.UTENSILS_RACK_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> UTENSILS_RACK_WALL = piece("utensils_rack_wall", "kitchen_crate", 0x666666,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.UTENSILS_RACK.get()));
    public static final DeferredBlock<FacingFurnitureBlock> UTENSILS_RACK_CEILING = piece("utensils_rack_ceiling", "kitchen_crate", 0x666666,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.UTENSILS_RACK.get()));
    public static final DeferredBlock<FurnitureBlock> OVEN_STOVE = piece("oven_stove", "kitchen_crate",
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> MICROWAVE = piece("microwave", "kitchen_crate",
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MICROWAVE_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> MICROWAVE_WALL = piece("microwave_wall", "kitchen_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MICROWAVE.get()).displayAs(DisplayMode.FLOOR));
    public static final DeferredBlock<FurnitureBlock> KITCHEN_SINK = piece("kitchen_sink", "kitchen_crate", 0xCCCCCC,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> KITCHEN_SHELF = piece("kitchen_shelf", "kitchen_crate",
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.KITCHEN_SHELF_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> KITCHEN_SHELF_WALL = piece("kitchen_shelf_wall", "kitchen_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.KITCHEN_SHELF.get()));
    public static final DeferredBlock<FurnitureBlock> REFRIDGERATOR = piece("refridgerator", "kitchen_crate", 0xCCCCCC,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.REFRIDGERATOR_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> REFRIDGERATOR_WALL = piece("refridgerator_wall", "kitchen_crate", 0xCCCCCC,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.REFRIDGERATOR.get()));
    public static final DeferredBlock<FurnitureBlock> KITCHEN_ISLAND = piece("kitchen_island", "kitchen_crate", 0xCCCCCC,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));

    // The Medieval Crate's furniture ("Medieval Crate Exclusive"): the server pack's models/custom/furniture/medieval/ models,
    // names, rarities and paints from Ren's F9 capture of its reward page. Display modes follow each model's fixed display.
    /** A seat 8 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> MEDIEVAL_CHAIR = piece("medieval_chair", "medieval_crate", 0x647A4C,
            p -> new SeatFurnitureBlock(p, true, Form.FLOOR, 8));
    public static final DeferredBlock<FurnitureBlock> MEDIEVAL_TABLE = piece("medieval_table", "medieval_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> ROYAL_BANNER = piece("royal_banner", "medieval_crate", 0x8D6397,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.ROYAL_BANNER_WALL.get(), () -> DecorFurniture.ROYAL_BANNER_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> ROYAL_BANNER_WALL = piece("royal_banner_wall", "medieval_crate", 0x8D6397,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.ROYAL_BANNER.get()));
    public static final DeferredBlock<FacingFurnitureBlock> ROYAL_BANNER_CEILING = piece("royal_banner_ceiling", "medieval_crate", 0x8D6397,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.ROYAL_BANNER.get()));
    public static final DeferredBlock<WallFurnitureBlock> ROYAL_CURTAIN = piece("royal_curtain", "medieval_crate", 0x8D6397,
            p -> new WallFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> MEDIEVAL_CANDELABRA = piece("medieval_candelabra", "medieval_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR, () -> DecorFurniture.MEDIEVAL_CANDELABRA_WALL.get(), () -> DecorFurniture.MEDIEVAL_CANDELABRA_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> MEDIEVAL_CANDELABRA_WALL = piece("medieval_candelabra_wall", "medieval_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.MEDIEVAL_CANDELABRA.get()));
    public static final DeferredBlock<FacingFurnitureBlock> MEDIEVAL_CANDELABRA_CEILING = piece("medieval_candelabra_ceiling", "medieval_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.MEDIEVAL_CANDELABRA.get()));
    public static final DeferredBlock<FurnitureBlock> ARMOR_DISPLAY = piece("armor_display", "medieval_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> DUNGEON_CAGE = piece("dungeon_cage", "medieval_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE, () -> DecorFurniture.DUNGEON_CAGE_WALL.get(), () -> DecorFurniture.DUNGEON_CAGE_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> DUNGEON_CAGE_WALL = piece("dungeon_cage_wall", "medieval_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.DUNGEON_CAGE.get()));
    public static final DeferredBlock<FacingFurnitureBlock> DUNGEON_CAGE_CEILING = piece("dungeon_cage_ceiling", "medieval_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.DUNGEON_CAGE.get()));
    public static final DeferredBlock<FurnitureBlock> MEDIEVAL_CART = piece("medieval_cart", "medieval_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    /** A seat 8 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> ROYAL_THRONE = piece("royal_throne", "medieval_crate", 0x8D6397,
            p -> new SeatFurnitureBlock(p, true, Form.FURNITURE, 8));

    // The Music Crate's furniture ("Music Crate Exclusive"): the server pack's models/custom/furniture/music/ models,
    // names, rarities and paints from Ren's F9 capture of its reward page. Display modes follow each model's fixed display.
    public static final DeferredBlock<FurnitureBlock> ACOUSTIC_GUITAR = piece("acoustic_guitar", "music_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.ACOUSTIC_GUITAR_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> ACOUSTIC_GUITAR_WALL = piece("acoustic_guitar_wall", "music_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.ACOUSTIC_GUITAR.get()));
    public static final DeferredBlock<FurnitureBlock> BOOMBOX = piece("boombox", "music_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> STAGE_LIGHT = piece("stage_light", "music_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.STAGE_LIGHT_WALL.get(), () -> DecorFurniture.STAGE_LIGHT_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> STAGE_LIGHT_WALL = piece("stage_light_wall", "music_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.STAGE_LIGHT.get()));
    public static final DeferredBlock<FacingFurnitureBlock> STAGE_LIGHT_CEILING = piece("stage_light_ceiling", "music_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.STAGE_LIGHT.get()));
    public static final DeferredBlock<FurnitureBlock> KEYBOARD_PIANO = piece("keyboard_piano", "music_crate", 0x4C4C4C,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> SPEAKER = piece("speaker", "music_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.SPEAKER_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> SPEAKER_WALL = piece("speaker_wall", "music_crate", 0xCB5148,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.SPEAKER.get()));
    public static final DeferredBlock<FurnitureBlock> DRUMSET = piece("drumset", "music_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> DJ_BOOTH = piece("dj_booth", "music_crate", 0x3EB8DD,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.dj_booth.loop", null));
    public static final DeferredBlock<FurnitureBlock> GRAND_PIANO = piece("grand_piano", "music_crate", 0x4C4C4C,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> CONCERT_STAGE = piece("concert_stage", "music_crate", 0x547483,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.concert_stage.loop", null));

    // The Space Crate's furniture ("Space Crate Exclusive"): the server pack's models/custom/furniture/space/ models,
    // names, rarities and paints from Ren's F9 capture of its reward page. Display modes follow each model's fixed display.
    public static final DeferredBlock<FurnitureBlock> CONTROL_PANEL = piece("control_panel", "space_crate", 0x81D8D0,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.CONTROL_PANEL_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> CONTROL_PANEL_WALL = piece("control_panel_wall", "space_crate", 0x81D8D0,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CONTROL_PANEL.get()));
    public static final DeferredBlock<FurnitureBlock> METAL_CRATE = piece("metal_crate", "space_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> SOLAR_PANEL = piece("solar_panel", "space_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE, () -> DecorFurniture.SOLAR_PANEL_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> SOLAR_PANEL_WALL = piece("solar_panel_wall", "space_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.SOLAR_PANEL.get()));
    public static final DeferredBlock<FurnitureBlock> TUBE_LIGHT = piece("tube_light", "space_crate", 0xB0DFE5,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.TUBE_LIGHT_WALL.get(), () -> DecorFurniture.TUBE_LIGHT_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> TUBE_LIGHT_WALL = piece("tube_light_wall", "space_crate", 0xB0DFE5,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.TUBE_LIGHT.get()));
    public static final DeferredBlock<FacingFurnitureBlock> TUBE_LIGHT_CEILING = piece("tube_light_ceiling", "space_crate", 0xB0DFE5,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.TUBE_LIGHT.get()));
    public static final DeferredBlock<FurnitureBlock> TELESCOPE = piece("telescope", "space_crate", 0x74A4CA,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CRYOTANK = piece("cryotank", "space_crate", 0x81D8D0,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> ROCKETSHIP = piece("rocketship", "space_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> SPACE_BUGGY = piece("space_buggy", "space_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> SOLAR_SYSTEM_MODEL = piece("solar_system_model", "space_crate",
            p -> new AnimatedFurnitureBlock(p, "furniture", false, "animation.solar_system_model.orbit", null, () -> DecorFurniture.SOLAR_SYSTEM_MODEL_CEILING.get(), null));
    public static final DeferredBlock<AnimatedCeilingFurnitureBlock> SOLAR_SYSTEM_MODEL_CEILING = piece("solar_system_model_ceiling", "space_crate",
            p -> new AnimatedCeilingFurnitureBlock(p, "furniture", false, "animation.solar_system_model_ceiling.orbit", () -> DecorFurniture.SOLAR_SYSTEM_MODEL.get()));

    // The Western Crate's furniture ("Western Crate Exclusive"): the server pack's models/custom/furniture/western/ models,
    // names, rarities and paints from Ren's F9 capture of its reward page. Display modes follow each model's fixed display.
    public static final DeferredBlock<FurnitureBlock> FLAT_CACTUS = piece("flat_cactus", "western_crate", 0xFB8EB0,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> RUSTIC_CRATE = piece("rustic_crate", "western_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR));
    /** A seat 8 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> SALOON_CHAIR = piece("saloon_chair", "western_crate", 0x647A4C,
            p -> new SeatFurnitureBlock(p, true, Form.FLOOR, 8));
    public static final DeferredBlock<FurnitureBlock> SALOON_LANTERN = piece("saloon_lantern", "western_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR, () -> DecorFurniture.SALOON_LANTERN_WALL.get(), () -> DecorFurniture.SALOON_LANTERN_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> SALOON_LANTERN_WALL = piece("saloon_lantern_wall", "western_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.SALOON_LANTERN.get()));
    public static final DeferredBlock<FacingFurnitureBlock> SALOON_LANTERN_CEILING = piece("saloon_lantern_ceiling", "western_crate",
            p -> new FacingFurnitureBlock(p, false, () -> DecorFurniture.SALOON_LANTERN.get()));
    public static final DeferredBlock<FurnitureBlock> SALOON_TABLE = piece("saloon_table", "western_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR));
    public static final DeferredBlock<WallFurnitureBlock> SALOON_SIGN = piece("saloon_sign", "western_crate",
            p -> new WallFurnitureBlock(p, true, null));
    public static final DeferredBlock<FurnitureBlock> OIL_LAMPPOST = piece("oil_lamppost", "western_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR));
    /** A seat 8 pixels up (a guess until tried). */
    public static final DeferredBlock<SeatFurnitureBlock> SALOON_BENCH = piece("saloon_bench", "western_crate",
            p -> new SeatFurnitureBlock(p, false, Form.FLOOR, 8));
    public static final DeferredBlock<FurnitureBlock> COVERED_WAGON = piece("covered_wagon", "western_crate",
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> WATER_TOWER = piece("water_tower", "western_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> RUSTIC_WINDMILL = piece("rustic_windmill", "western_crate",
            p -> new AnimatedFurnitureBlock(p, "furniture", false, "animation.rustic_windmill.spin", null));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> RUSTIC_WATERWHEEL = piece("rustic_waterwheel", "western_crate",
            p -> new AnimatedFurnitureBlock(p, "furniture", false, "animation.rustic_waterwheel.spin", null));

    // The Wizardry Crate's furniture ("Wizardry Crate Exclusive"): the server pack's models/custom/furniture/wizardry/ models,
    // names, rarities and paints from Ren's F9 capture of its reward page. Display modes follow each model's fixed display.
    public static final DeferredBlock<FurnitureBlock> MAGIC_HERBS = piece("magic_herbs", "wizardry_crate", 0xD499FF,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MAGIC_HERBS_WALL.get(), () -> DecorFurniture.MAGIC_HERBS_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> MAGIC_HERBS_WALL = piece("magic_herbs_wall", "wizardry_crate", 0xD499FF,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MAGIC_HERBS.get()));
    public static final DeferredBlock<FacingFurnitureBlock> MAGIC_HERBS_CEILING = piece("magic_herbs_ceiling", "wizardry_crate", 0xD499FF,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MAGIC_HERBS.get()));
    public static final DeferredBlock<FurnitureBlock> POTTED_GLOWSHROOM = piece("potted_glowshroom", "wizardry_crate", 0x81D8D0,
            p -> new FurnitureBlock(p, true, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> MAGIC_BOOK_STACK = piece("magic_book_stack", "wizardry_crate", 0x8D4585,
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.MAGIC_BOOK_STACK_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> MAGIC_BOOK_STACK_WALL = piece("magic_book_stack_wall", "wizardry_crate", 0x8D4585,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MAGIC_BOOK_STACK.get()));
    public static final DeferredBlock<WallFurnitureBlock> DREAM_CATCHER = piece("dream_catcher", "wizardry_crate", 0x8D4585,
            p -> new WallFurnitureBlock(p, true, () -> DecorFurniture.DREAM_CATCHER_CEILING.get()));
    public static final DeferredBlock<FacingFurnitureBlock> DREAM_CATCHER_CEILING = piece("dream_catcher_ceiling", "wizardry_crate", 0x8D4585,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.DREAM_CATCHER.get()));
    public static final DeferredBlock<FurnitureBlock> MAGIC_TAPESTRY = piece("magic_tapestry", "wizardry_crate", 0x8D4585,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MAGIC_TAPESTRY_WALL.get(), () -> DecorFurniture.MAGIC_TAPESTRY_CEILING.get(), null));
    public static final DeferredBlock<FacingFurnitureBlock> MAGIC_TAPESTRY_WALL = piece("magic_tapestry_wall", "wizardry_crate", 0x8D4585,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MAGIC_TAPESTRY.get()));
    public static final DeferredBlock<FacingFurnitureBlock> MAGIC_TAPESTRY_CEILING = piece("magic_tapestry_ceiling", "wizardry_crate", 0x8D4585,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MAGIC_TAPESTRY.get()));
    public static final DeferredBlock<FurnitureBlock> MAGIC_MIRROR = piece("magic_mirror", "wizardry_crate", 0xCB66F9,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.MAGIC_MIRROR_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> MAGIC_MIRROR_WALL = piece("magic_mirror_wall", "wizardry_crate", 0xCB66F9,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.MAGIC_MIRROR.get()));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> MAGIC_CANDLES = piece("magic_candles", "wizardry_crate", 0xCB66F9,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.magic_candles.flicker", null));
    public static final DeferredBlock<FurnitureBlock> POTION_STATION = piece("potion_station", "wizardry_crate", 0x93BA4F,
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> RUNIC_STATUE = piece("runic_statue", "wizardry_crate", 0x81D8D0,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.runic_statue.loop", null));
    /** Animated: Ren's model; _ignore bones get no hitbox. */
    public static final DeferredBlock<AnimatedFurnitureBlock> MAGIC_CAULDRON = piece("magic_cauldron", "wizardry_crate", 0x93BA4F,
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "animation.magic_cauldron.bubble", null));

    private DecorFurniture() {}

    /** Every decor furniture block, in registration order. */
    public static List<DeferredBlock<? extends FurnitureBlock>> all() {
        return List.copyOf(ALL);
    }

    /**
     * Registers a piece and its item: wood, breaking quickly with anything (the Crates mod's furniture), with the
     * crate's gold exclusive line when it has one.
     */
    private static <B extends FurnitureBlock> DeferredBlock<B> piece(String name, @Nullable String crate,
            Function<BlockBehaviour.Properties, ? extends B> factory) {
        return piece(name, crate, null, factory);
    }

    /**
     * As {@link #piece(String, String, Function)}, with {@code defaultPaint}, the colour its item starts in: for a
     * piece from Ren's uicapture, the captured item's {@code minecraft:firework_explosion} colour (white is none).
     */
    private static <B extends FurnitureBlock> DeferredBlock<B> piece(String name, @Nullable String crate, @Nullable Integer defaultPaint,
            Function<BlockBehaviour.Properties, ? extends B> factory) {
        return piece(name, crate, defaultPaint, UnaryOperator.identity(), factory);
    }

    /** As {@link #piece(String, String, Integer, Function)}, with {@code tweak} changing the block's properties (the rug has no collision). */
    private static <B extends FurnitureBlock> DeferredBlock<B> piece(String name, @Nullable String crate, @Nullable Integer defaultPaint,
            UnaryOperator<BlockBehaviour.Properties> tweak, Function<BlockBehaviour.Properties, ? extends B> factory) {
        DeferredBlock<B> block = R.blocks().registerBlock(name, factory, () -> FurnitureFootprints.properties(name,
                tweak.apply(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion())));
        R.items().registerItem(name, properties -> new FurnitureBlockItem(block.get(), properties), properties -> {
            properties.useBlockDescriptionPrefix();
            if (defaultPaint != null) {
                properties.component(DecorDataComponents.PAINT.get(), defaultPaint);
            }
            if (crate != null) {
                properties.component(DataComponents.LORE, new ItemLore(List.of(CosmeticItem.exclusive(crate).withStyle(style -> style.withItalic(false)))));
            }
            return properties;
        });
        ALL.add(block);
        return block;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
