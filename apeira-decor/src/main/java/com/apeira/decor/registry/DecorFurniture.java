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
            p -> new WallFurnitureBlock(p, true, () -> DecorFurniture.FLUORESCENT_LIGHT_CEILING.get()).displayAs(DisplayMode.TOP_FACE));
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
            p -> new FurnitureBlock(p, true, Form.FURNITURE, () -> DecorFurniture.BILLBOARD_WALL.get(), null, null));
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
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.POTTED_FOREST_FERN.get()).displayAs(DisplayMode.FLOOR));
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
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.LARGE_HAMMOCK.get()));
    public static final DeferredBlock<FurnitureBlock> CABIN_FIREPLACE = piece("cabin_fireplace", "camping_crate", 0xAC5353,
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.CABIN_FIREPLACE_WALL.get(), null, null));
    public static final DeferredBlock<FacingFurnitureBlock> CABIN_FIREPLACE_WALL = piece("cabin_fireplace_wall", "camping_crate", 0xAC5353,
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.CABIN_FIREPLACE.get()));
    /** A paper item on the capture, with no tint index faces: not paintable. */
    public static final DeferredBlock<FurnitureBlock> BONFIRE = piece("bonfire", "camping_crate",
            p -> new FurnitureBlock(p, false, Form.FLOOR));
    public static final DeferredBlock<FurnitureBlock> CAMPERVAN = piece("campervan", "camping_crate", 0xCB5148,
            p -> new FurnitureBlock(p, true, Form.FLOOR));

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
