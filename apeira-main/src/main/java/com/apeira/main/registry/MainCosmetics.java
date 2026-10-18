package com.apeira.main.registry;

import static com.apeira.decor.cosmetic.CosmeticItem.paint;
import static com.apeira.decor.cosmetic.CosmeticRarity.COMMON;
import static com.apeira.decor.cosmetic.CosmeticRarity.EPIC;
import static com.apeira.decor.cosmetic.CosmeticRarity.LEGENDARY;
import static com.apeira.decor.cosmetic.CosmeticRarity.RARE;
import static com.apeira.decor.cosmetic.CosmeticSlot.BACK;
import static com.apeira.decor.cosmetic.CosmeticSlot.HAND;
import static com.apeira.decor.cosmetic.CosmeticSlot.HAT;
import static com.apeira.decor.paint.PaintColor.GREEN;
import static com.apeira.decor.paint.PaintColor.LIGHT_BLUE;
import static com.apeira.decor.paint.PaintColor.ORANGE;
import static com.apeira.decor.paint.PaintColor.PINK;
import static com.apeira.decor.paint.PaintColor.PURPLE;
import static com.apeira.decor.paint.PaintColor.RED;
import static com.apeira.decor.paint.PaintColor.WHITE;
import static com.apeira.decor.paint.PaintColor.YELLOW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticRarity;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.main.Apeira;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The crate cosmetics, ported from the Crates mod ({@code CratesItems}): slot, rarity, paintability, default paint and
 * the crate each is exclusive to are the Crates mod's. Grouped by crate in its order.
 *
 * <p>127 have a 3-D model ({@code models/item/cosmetic/<name>.json}, from the Crates mod's {@code models/cosmetics/}),
 * worn on the player and shown in the head and third-person display contexts; the other 50 had no model in the Crates
 * mod either and show their rarity token everywhere, as they did there.
 */
public final class MainCosmetics {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<Cosmetic> ALL = new ArrayList<>();

    /** A registered cosmetic and whether it has a 3-D model, for datagen. */
    public record Cosmetic(DeferredItem<CosmeticItem> item, CosmeticSlot slot, CosmeticRarity rarity, boolean modelled) {}

    // Aether Crate
    public static final DeferredItem<CosmeticItem> SPARTAN_HELMET = cosmetic("spartan_helmet", HAT, COMMON, true, paint(RED, 3), "aether_crate", true);
    public static final DeferredItem<CosmeticItem> LIGHTNING_BOLT = cosmetic("lightning_bolt", HAND, COMMON, true, paint(YELLOW, 3), "aether_crate", true);
    public static final DeferredItem<CosmeticItem> BLITZ_AURA = cosmetic("blitz_aura", BACK, COMMON, true, paint(YELLOW, 3), "aether_crate", true);
    public static final DeferredItem<CosmeticItem> SPARTAN_ROBE = cosmetic("spartan_robe", BACK, RARE, true, paint(RED, 3), "aether_crate", false);
    public static final DeferredItem<CosmeticItem> CLOUD_WINGS = cosmetic("cloud_wings", BACK, RARE, true, paint(WHITE, 1), "aether_crate", true);
    public static final DeferredItem<CosmeticItem> LYRE = cosmetic("lyre", HAND, RARE, false, paint(YELLOW, 1), "aether_crate", true);
    public static final DeferredItem<CosmeticItem> SOLAR_STAFF = cosmetic("solar_staff", HAND, EPIC, true, paint(RED, 1), "aether_crate", true);

    // Animal Crate
    public static final DeferredItem<CosmeticItem> MOOSE_ANTLERS = cosmetic("moose_antlers", HAT, RARE, true, null, "animal_crate", true);
    public static final DeferredItem<CosmeticItem> GORILLA_MASK = cosmetic("gorilla_mask", HAT, RARE, true, paint(WHITE, 6), "animal_crate", true);
    public static final DeferredItem<CosmeticItem> MASCOT_DOG_HELMET = cosmetic("mascot_dog_helmet", HAT, RARE, true, paint(ORANGE, 1), "animal_crate", true);
    public static final DeferredItem<CosmeticItem> MONKEY_BACKSWAG = cosmetic("monkey_backswag", BACK, EPIC, true, paint(ORANGE, 5), "animal_crate", true);
    public static final DeferredItem<CosmeticItem> GIRAFFE_HELMET = cosmetic("giraffe_helmet", HAT, EPIC, true, paint(YELLOW, 6), "animal_crate", true);

    // Aquatic Crate
    public static final DeferredItem<CosmeticItem> LIFE_RING = cosmetic("life_ring", BACK, COMMON, true, paint(RED, 3), "aquatic_crate", true);
    public static final DeferredItem<CosmeticItem> SHARK_TAIL = cosmetic("shark_tail", BACK, COMMON, true, paint(LIGHT_BLUE, 3), "aquatic_crate", true);
    public static final DeferredItem<CosmeticItem> ANCHOR = cosmetic("anchor", BACK, COMMON, true, paint(ORANGE, 5), "aquatic_crate", true);
    public static final DeferredItem<CosmeticItem> JELLYFISH_HAT = cosmetic("jellyfish_hat", HAT, COMMON, true, null, "aquatic_crate", true);
    public static final DeferredItem<CosmeticItem> FISH_HAND = cosmetic("fish_hand", HAND, COMMON, true, paint(ORANGE, 2), "aquatic_crate", false);
    public static final DeferredItem<CosmeticItem> DIVER_HELMET = cosmetic("diver_helmet", HAT, RARE, true, null, "aquatic_crate", true);
    public static final DeferredItem<CosmeticItem> SHARK_HAT = cosmetic("shark_hat", HAT, RARE, true, paint(LIGHT_BLUE, 3), "aquatic_crate", true);
    public static final DeferredItem<CosmeticItem> CRAB_CLAW = cosmetic("crab_claw", HAND, RARE, true, paint(RED, 3), "aquatic_crate", false);

    // Atlantis Crate
    public static final DeferredItem<CosmeticItem> POSEIDONS_CROWN = cosmetic("poseidons_crown", HAT, RARE, true, null, "atlantis_crate", false);
    public static final DeferredItem<CosmeticItem> POSEIDONS_ROBE = cosmetic("poseidons_robe", BACK, RARE, true, paint(LIGHT_BLUE, 2), "atlantis_crate", false);
    public static final DeferredItem<CosmeticItem> POSEIDONS_TRIDENT = cosmetic("poseidons_trident", HAND, LEGENDARY, true, paint(LIGHT_BLUE, 2), "atlantis_crate", false);

    // Bathroom Crate
    public static final DeferredItem<CosmeticItem> PLUNGER = cosmetic("plunger", HAND, RARE, true, paint(RED, 3), "bathroom_crate", true);
    public static final DeferredItem<CosmeticItem> RUBBER_DUCKIE_HAT = cosmetic("rubber_duckie_hat", HAT, EPIC, true, paint(LIGHT_BLUE, 4), "bathroom_crate", false);
    public static final DeferredItem<CosmeticItem> TUB = cosmetic("tub", BACK, LEGENDARY, true, paint(WHITE, 1), "bathroom_crate", false);

    // Beach Crate
    public static final DeferredItem<CosmeticItem> ICE_CREAM_CONE = cosmetic("ice_cream_cone", HAND, COMMON, true, paint(LIGHT_BLUE, 1), "beach_crate", true);
    public static final DeferredItem<CosmeticItem> FROGGY_HAT = cosmetic("froggy_hat", HAT, COMMON, true, paint(GREEN, 1), "beach_crate", true);
    public static final DeferredItem<CosmeticItem> FRUIT_CROWN = cosmetic("fruit_crown", HAT, COMMON, true, paint(GREEN, 1), "beach_crate", true);
    public static final DeferredItem<CosmeticItem> SURFBOARD = cosmetic("surfboard", BACK, RARE, true, paint(LIGHT_BLUE, 6), "beach_crate", false);
    public static final DeferredItem<CosmeticItem> FLAMINGO_FLOATY = cosmetic("flamingo_floaty", BACK, RARE, true, paint(PINK, 3), "beach_crate", true);
    public static final DeferredItem<CosmeticItem> BEACH_PACK = cosmetic("beach_pack", BACK, EPIC, true, paint(RED, 1), "beach_crate", true);

    // Business Crate
    public static final DeferredItem<CosmeticItem> MONEY_SACK = cosmetic("money_sack", BACK, EPIC, true, paint(YELLOW, 2), "business_crate", true);

    // Camping Crate
    public static final DeferredItem<CosmeticItem> FLASHLIGHT = cosmetic("flashlight", HAND, COMMON, true, paint(GREEN, 1), "camping_crate", false);
    public static final DeferredItem<CosmeticItem> LOG_BUNDLE = cosmetic("log_bundle", BACK, RARE, true, paint(ORANGE, 7), "camping_crate", false);
    public static final DeferredItem<CosmeticItem> CHAINSAW = cosmetic("chainsaw", HAND, EPIC, true, paint(WHITE, 1), "camping_crate", false);
    public static final DeferredItem<CosmeticItem> CUCKOO_CLOCK = cosmetic("cuckoo_clock", BACK, LEGENDARY, true, paint(PURPLE, 3), "camping_crate", false);

    // Construction Crate
    public static final DeferredItem<CosmeticItem> HARD_HAT = cosmetic("hard_hat", HAT, COMMON, true, paint(YELLOW, 5), "construction_crate", true);
    public static final DeferredItem<CosmeticItem> TREADS = cosmetic("treads", BACK, EPIC, true, paint(YELLOW, 6), "construction_crate", true);
    public static final DeferredItem<CosmeticItem> CRANE = cosmetic("crane", BACK, LEGENDARY, true, paint(YELLOW, 4), "construction_crate", false);

    // Easter Crate
    public static final DeferredItem<CosmeticItem> HATCHED_EGG = cosmetic("hatched_egg", BACK, COMMON, true, paint(PURPLE, 3), "easter_crate", true);
    public static final DeferredItem<CosmeticItem> CARROT_CANE = cosmetic("carrot_cane", HAND, COMMON, true, paint(GREEN, 1), "easter_crate", true);
    public static final DeferredItem<CosmeticItem> HATCHED_EGG_HAT = cosmetic("hatched_egg_hat", HAT, COMMON, true, paint(PURPLE, 3), "easter_crate", true);
    public static final DeferredItem<CosmeticItem> BUNNY_BEANIE = cosmetic("bunny_beanie", HAT, RARE, true, paint(PURPLE, 3), "easter_crate", true);
    public static final DeferredItem<CosmeticItem> EGG_BASKET = cosmetic("egg_basket", HAND, RARE, true, paint(PURPLE, 3), "easter_crate", false);
    public static final DeferredItem<CosmeticItem> BUNNY_HEADBAND = cosmetic("bunny_headband", HAT, RARE, true, paint(PURPLE, 3), "easter_crate", false);

    // Fast Food Crate
    public static final DeferredItem<CosmeticItem> COMICALLY_LARGE_FORK = cosmetic("comically_large_fork", HAND, COMMON, true, paint(RED, 3), "fast_food_crate", false);
    public static final DeferredItem<CosmeticItem> COMICALLY_LARGE_SPOON = cosmetic("comically_large_spoon", HAND, COMMON, true, paint(RED, 3), "fast_food_crate", false);
    public static final DeferredItem<CosmeticItem> FRY_PACK = cosmetic("fry_pack", BACK, RARE, true, paint(RED, 3), "fast_food_crate", true);
    public static final DeferredItem<CosmeticItem> BURGER_CAP = cosmetic("burger_cap", HAT, RARE, true, paint(RED, 3), "fast_food_crate", true);

    // Festive Crate
    public static final DeferredItem<CosmeticItem> CHRISTMAS_LIGHTS_ARM = cosmetic("christmas_lights_arm", HAND, COMMON, true, paint(RED, 3), "festive_crate", false);
    public static final DeferredItem<CosmeticItem> CANDY_CANE_STAFF = cosmetic("candy_cane_staff", HAND, COMMON, true, paint(RED, 3), "festive_crate", true);
    public static final DeferredItem<CosmeticItem> REINDEER_ANTLERS = cosmetic("reindeer_antlers", HAT, COMMON, true, paint(ORANGE, 5), "festive_crate", true);
    public static final DeferredItem<CosmeticItem> SANTA_HAT = cosmetic("santa_hat", HAT, COMMON, true, paint(RED, 3), "festive_crate", true);
    public static final DeferredItem<CosmeticItem> ELF_HAT = cosmetic("elf_hat", HAT, COMMON, true, paint(RED, 3), "festive_crate", true);
    public static final DeferredItem<CosmeticItem> FESTIVE_CANDLE = cosmetic("festive_candle", HAND, COMMON, true, paint(RED, 3), "festive_crate", true);
    public static final DeferredItem<CosmeticItem> CHRISTMAS_TREE_HAT = cosmetic("christmas_tree_hat", HAT, RARE, true, paint(LIGHT_BLUE, 1), "festive_crate", true);
    public static final DeferredItem<CosmeticItem> FESTIVE_BELLS = cosmetic("festive_bells", BACK, RARE, true, paint(RED, 3), "festive_crate", false);

    // Fun Fair Crate
    public static final DeferredItem<CosmeticItem> PARTY_HAT = cosmetic("party_hat", HAT, COMMON, true, paint(YELLOW, 4), "fun_fair_crate", false);

    // Gothic Crate
    public static final DeferredItem<CosmeticItem> BONE_STAFF = cosmetic("bone_staff", HAND, COMMON, true, paint(PURPLE, 1), "gothic_crate", true);
    public static final DeferredItem<CosmeticItem> DEMON_HORNS = cosmetic("demon_horns", HAT, COMMON, true, paint(RED, 3), "gothic_crate", true);
    public static final DeferredItem<CosmeticItem> WRAITH_HOOD = cosmetic("wraith_hood", HAT, COMMON, true, paint(PURPLE, 5), "gothic_crate", true);
    public static final DeferredItem<CosmeticItem> GOTHIC_LANTERN = cosmetic("gothic_lantern", HAND, COMMON, true, paint(PURPLE, 1), "gothic_crate", true);
    public static final DeferredItem<CosmeticItem> DEMON_WINGS = cosmetic("demon_wings", BACK, RARE, true, paint(RED, 3), "gothic_crate", true);

    // Harvest Crate
    public static final DeferredItem<CosmeticItem> TURKEY_HELMET = cosmetic("turkey_helmet", HAT, COMMON, false, null, "harvest_crate", false);
    public static final DeferredItem<CosmeticItem> TURKEY_TAIL = cosmetic("turkey_tail", BACK, COMMON, false, null, "harvest_crate", true);
    public static final DeferredItem<CosmeticItem> CORN_STALK_STAFF = cosmetic("corn_stalk_staff", HAND, COMMON, false, null, "harvest_crate", true);
    public static final DeferredItem<CosmeticItem> MAPLE_LEAF_CROWN = cosmetic("maple_leaf_crown", HAT, COMMON, false, null, "harvest_crate", false);
    public static final DeferredItem<CosmeticItem> PILGRIM_HAT = cosmetic("pilgrim_hat", HAT, COMMON, true, paint(WHITE, 6), "harvest_crate", true);
    public static final DeferredItem<CosmeticItem> PUMPKIN_BEANIE = cosmetic("pumpkin_beanie", HAT, COMMON, true, paint(YELLOW, 6), "harvest_crate", true);
    public static final DeferredItem<CosmeticItem> MAPLE_LEAF_WINGS = cosmetic("maple_leaf_wings", BACK, RARE, false, null, "harvest_crate", false);
    public static final DeferredItem<CosmeticItem> SCARECROW_HAT = cosmetic("scarecrow_hat", HAT, RARE, true, paint(PURPLE, 6), "harvest_crate", true);
    public static final DeferredItem<CosmeticItem> SUNFLOWER_UMBRELLA = cosmetic("sunflower_umbrella", HAND, RARE, true, paint(YELLOW, 4), "harvest_crate", true);
    public static final DeferredItem<CosmeticItem> BLUNDERBUSS = cosmetic("blunderbuss", HAND, EPIC, true, paint(ORANGE, 7), "harvest_crate", false);

    // Kitchen Crate
    public static final DeferredItem<CosmeticItem> CHEF_HAT = cosmetic("chef_hat", HAT, COMMON, true, paint(WHITE, 1), "kitchen_crate", true);
    public static final DeferredItem<CosmeticItem> APRON = cosmetic("apron", BACK, RARE, true, null, "kitchen_crate", false);
    public static final DeferredItem<CosmeticItem> PANCAKE_STACK = cosmetic("pancake_stack", HAT, EPIC, true, paint(YELLOW, 4), "kitchen_crate", true);
    public static final DeferredItem<CosmeticItem> SOUP_POT = cosmetic("soup_pot", BACK, LEGENDARY, true, paint(WHITE, 1), "kitchen_crate", true);

    // Lovely Crate
    public static final DeferredItem<CosmeticItem> LOVELY_RIBBON = cosmetic("lovely_ribbon", BACK, COMMON, true, paint(PINK, 7), "lovely_crate", false);
    public static final DeferredItem<CosmeticItem> HEART_HEADBAND = cosmetic("heart_headband", HAT, COMMON, true, paint(PINK, 7), "lovely_crate", true);
    public static final DeferredItem<CosmeticItem> FLOWER_BOUQUET = cosmetic("flower_bouquet", HAND, COMMON, true, paint(PINK, 7), "lovely_crate", true);
    public static final DeferredItem<CosmeticItem> ANGELIC_HALO = cosmetic("angelic_halo", HAT, COMMON, true, null, "lovely_crate", true);
    public static final DeferredItem<CosmeticItem> TEDDY_HELMET = cosmetic("teddy_helmet", HAT, COMMON, true, paint(PINK, 7), "lovely_crate", true);
    public static final DeferredItem<CosmeticItem> ANGELIC_WINGS = cosmetic("angelic_wings", BACK, RARE, true, paint(WHITE, 1), "lovely_crate", true);
    public static final DeferredItem<CosmeticItem> TEDDY_BEAR = cosmetic("teddy_bear", BACK, EPIC, true, paint(PINK, 2), "lovely_crate", false);
    public static final DeferredItem<CosmeticItem> CUPIDS_BOW = cosmetic("cupids_bow", BACK, LEGENDARY, true, null, "lovely_crate", false);

    // Lunar Crate
    public static final DeferredItem<CosmeticItem> LUNAR_CROWN = cosmetic("lunar_crown", HAT, COMMON, true, paint(RED, 3), "lunar_crate", true);
    public static final DeferredItem<CosmeticItem> LUNAR_BANNERS = cosmetic("lunar_banners", BACK, COMMON, true, paint(RED, 3), "lunar_crate", true);
    public static final DeferredItem<CosmeticItem> LOTUS_LANTERN = cosmetic("lotus_lantern", BACK, COMMON, true, paint(PINK, 2), "lunar_crate", true);
    public static final DeferredItem<CosmeticItem> LUNAR_LANTERN = cosmetic("lunar_lantern", BACK, COMMON, true, paint(YELLOW, 1), "lunar_crate", true);
    public static final DeferredItem<CosmeticItem> PELLET_DRUM = cosmetic("pellet_drum", HAND, RARE, true, paint(RED, 3), "lunar_crate", true);
    public static final DeferredItem<CosmeticItem> FIREWORK_BACKPACK = cosmetic("firework_backpack", BACK, EPIC, true, paint(RED, 3), "lunar_crate", false);
    public static final DeferredItem<CosmeticItem> HONGBAO_SACK = cosmetic("hongbao_sack", BACK, EPIC, true, paint(RED, 3), "lunar_crate", true);

    // Medieval Crate
    public static final DeferredItem<CosmeticItem> JESTER_HAT = cosmetic("jester_hat", HAT, COMMON, true, paint(GREEN, 4), "medieval_crate", true);
    public static final DeferredItem<CosmeticItem> KNIGHT_ARMOR = cosmetic("knight_armor", BACK, RARE, true, paint(ORANGE, 5), "medieval_crate", true);
    public static final DeferredItem<CosmeticItem> KNIGHT_HELMET = cosmetic("knight_helmet", HAT, RARE, true, paint(RED, 3), "medieval_crate", true);
    public static final DeferredItem<CosmeticItem> LUTE = cosmetic("lute", HAND, RARE, false, null, "medieval_crate", false);
    public static final DeferredItem<CosmeticItem> DRAGON_WINGS = cosmetic("dragon_wings", BACK, EPIC, true, paint(RED, 3), "medieval_crate", true);
    public static final DeferredItem<CosmeticItem> ROYAL_CROWN = cosmetic("royal_crown", HAT, EPIC, true, null, "medieval_crate", true);

    // Mining Crate
    public static final DeferredItem<CosmeticItem> MINER_CAP = cosmetic("miner_cap", HAT, COMMON, true, paint(ORANGE, 2), "mining_crate", true);
    public static final DeferredItem<CosmeticItem> ORE_BACKPACK = cosmetic("ore_backpack", BACK, RARE, true, paint(WHITE, 4), "mining_crate", false);
    public static final DeferredItem<CosmeticItem> DRILL = cosmetic("drill", HAND, EPIC, true, paint(YELLOW, 4), "mining_crate", false);
    public static final DeferredItem<CosmeticItem> CRYSTAL_SPIKES = cosmetic("crystal_spikes", BACK, LEGENDARY, true, null, "mining_crate", true);

    // Muerte Crate
    public static final DeferredItem<CosmeticItem> CATRINA_CROWN = cosmetic("catrina_crown", HAT, COMMON, true, paint(PINK, 7), "muerte_crate", false);
    public static final DeferredItem<CosmeticItem> SUGAR_SKULL_MASK = cosmetic("sugar_skull_mask", HAT, COMMON, true, paint(GREEN, 1), "muerte_crate", true);
    public static final DeferredItem<CosmeticItem> CANDLE_SOMBRERO = cosmetic("candle_sombrero", HAT, RARE, true, paint(PURPLE, 1), "muerte_crate", true);
    public static final DeferredItem<CosmeticItem> CATRIN_CLOAK = cosmetic("catrin_cloak", BACK, RARE, true, paint(PINK, 7), "muerte_crate", true);
    public static final DeferredItem<CosmeticItem> CATRIN_GUITAR = cosmetic("catrin_guitar", HAND, EPIC, true, paint(PINK, 7), "muerte_crate", true);

    // Music Crate
    public static final DeferredItem<CosmeticItem> EDM_HEADSET = cosmetic("edm_headset", HAT, COMMON, true, paint(LIGHT_BLUE, 2), "music_crate", false);
    public static final DeferredItem<CosmeticItem> SAXOPHONE = cosmetic("saxophone", HAND, RARE, true, paint(YELLOW, 4), "music_crate", true);
    public static final DeferredItem<CosmeticItem> GUITAR = cosmetic("guitar", HAND, RARE, true, paint(RED, 1), "music_crate", false);
    public static final DeferredItem<CosmeticItem> TUBA = cosmetic("tuba", BACK, EPIC, true, paint(ORANGE, 2), "music_crate", true);
    public static final DeferredItem<CosmeticItem> EDM_AURA = cosmetic("edm_aura", BACK, LEGENDARY, true, paint(LIGHT_BLUE, 2), "music_crate", true);

    // Palace Crate
    public static final DeferredItem<CosmeticItem> BAMBOO_HAT = cosmetic("bamboo_hat", HAT, COMMON, true, paint(RED, 3), "palace_crate", true);
    public static final DeferredItem<CosmeticItem> ORNATE_FAN = cosmetic("ornate_fan", HAND, COMMON, true, null, "palace_crate", true);
    public static final DeferredItem<CosmeticItem> BLOSSOM_HAIR_PIECE = cosmetic("blossom_hair_piece", HAT, COMMON, true, null, "palace_crate", false);
    public static final DeferredItem<CosmeticItem> ORNATE_PARASOL = cosmetic("ornate_parasol", HAND, COMMON, true, paint(PINK, 2), "palace_crate", true);
    public static final DeferredItem<CosmeticItem> VEILED_HAT = cosmetic("veiled_hat", HAT, RARE, true, paint(PINK, 3), "palace_crate", true);
    public static final DeferredItem<CosmeticItem> CHERRY_BLOSSOM_POT = cosmetic("cherry_blossom_pot", BACK, RARE, true, paint(PINK, 2), "palace_crate", true);
    public static final DeferredItem<CosmeticItem> DAO_BLADE = cosmetic("dao_blade", BACK, EPIC, true, paint(RED, 3), "palace_crate", true);

    // Paradise Crate
    public static final DeferredItem<CosmeticItem> LEI_NECKLACE = cosmetic("lei_necklace", BACK, COMMON, true, null, "paradise_crate", true);
    public static final DeferredItem<CosmeticItem> SNORKEL_MASK = cosmetic("snorkel_mask", HAT, COMMON, true, paint(LIGHT_BLUE, 3), "paradise_crate", false);
    public static final DeferredItem<CosmeticItem> TIKI_MASK = cosmetic("tiki_mask", HAT, RARE, true, paint(RED, 1), "paradise_crate", true);
    public static final DeferredItem<CosmeticItem> BEACH_BOOMBOX = cosmetic("beach_boombox", BACK, RARE, true, paint(LIGHT_BLUE, 5), "paradise_crate", true);
    public static final DeferredItem<CosmeticItem> TROPICAL_HEADPIECE = cosmetic("tropical_headpiece", HAT, RARE, true, paint(RED, 1), "paradise_crate", true);
    public static final DeferredItem<CosmeticItem> SEA_SHELL = cosmetic("sea_shell", BACK, EPIC, true, paint(PINK, 2), "paradise_crate", false);
    public static final DeferredItem<CosmeticItem> FIRE_BATON = cosmetic("fire_baton", HAND, LEGENDARY, true, paint(ORANGE, 3), "paradise_crate", false);

    // Shamrock Crate
    public static final DeferredItem<CosmeticItem> LEAF_WINGS = cosmetic("leaf_wings", BACK, COMMON, false, null, "shamrock_crate", true);
    public static final DeferredItem<CosmeticItem> LEAF_CROWN = cosmetic("leaf_crown", HAT, COMMON, false, null, "shamrock_crate", true);
    public static final DeferredItem<CosmeticItem> GNOME_HAT = cosmetic("gnome_hat", HAT, COMMON, true, paint(RED, 1), "shamrock_crate", true);
    public static final DeferredItem<CosmeticItem> SHAMROCK_UMBRELLA = cosmetic("shamrock_umbrella", HAND, RARE, true, paint(GREEN, 2), "shamrock_crate", true);
    public static final DeferredItem<CosmeticItem> SHAMROCK_HAT = cosmetic("shamrock_hat", HAT, RARE, true, paint(GREEN, 1), "shamrock_crate", true);
    public static final DeferredItem<CosmeticItem> RAINBOW_AURA = cosmetic("rainbow_aura", BACK, EPIC, false, null, "shamrock_crate", true);

    // Space Crate
    public static final DeferredItem<CosmeticItem> ASTRONAUT_HELMET = cosmetic("astronaut_helmet", HAT, RARE, true, paint(ORANGE, 3), "space_crate", true);
    public static final DeferredItem<CosmeticItem> ASTRONAUT_AIR_TANK = cosmetic("astronaut_air_tank", BACK, RARE, true, paint(ORANGE, 3), "space_crate", true);
    public static final DeferredItem<CosmeticItem> SUSSY_HELMET = cosmetic("sussy_helmet", HAT, RARE, true, paint(RED, 3), "space_crate", true);
    public static final DeferredItem<CosmeticItem> STELLAR_HAT = cosmetic("stellar_hat", HAT, LEGENDARY, true, paint(PURPLE, 1), "space_crate", true);

    // Spooky Crate
    public static final DeferredItem<CosmeticItem> FRANKENSTEIN_HAT = cosmetic("frankenstein_hat", HAT, COMMON, true, paint(LIGHT_BLUE, 2), "spooky_crate", true);
    public static final DeferredItem<CosmeticItem> CANDY_BUCKET = cosmetic("candy_bucket", HAND, COMMON, true, paint(YELLOW, 2), "spooky_crate", true);
    public static final DeferredItem<CosmeticItem> JACKO_HELMET = cosmetic("jacko_helmet", HAT, COMMON, true, paint(YELLOW, 2), "spooky_crate", false);
    public static final DeferredItem<CosmeticItem> EYEBALL = cosmetic("eyeball", HAT, COMMON, true, paint(GREEN, 1), "spooky_crate", false);
    public static final DeferredItem<CosmeticItem> MEAT_CLEAVER = cosmetic("meat_cleaver", HAND, COMMON, true, paint(GREEN, 1), "spooky_crate", true);
    public static final DeferredItem<CosmeticItem> MAGIC_BROOM = cosmetic("magic_broom", HAND, RARE, true, paint(PURPLE, 1), "spooky_crate", false);
    public static final DeferredItem<CosmeticItem> PLAGUE_DOCTOR_MASK = cosmetic("plague_doctor_mask", HAT, RARE, true, paint(PURPLE, 1), "spooky_crate", true);
    public static final DeferredItem<CosmeticItem> MONSTER_SLAYER_GEAR = cosmetic("monster_slayer_gear", BACK, RARE, true, paint(GREEN, 1), "spooky_crate", true);
    public static final DeferredItem<CosmeticItem> SPIDER_LEGS = cosmetic("spider_legs", BACK, EPIC, true, paint(GREEN, 1), "spooky_crate", true);
    public static final DeferredItem<CosmeticItem> MEDUSA_HAT = cosmetic("medusa_hat", HAT, EPIC, true, paint(GREEN, 1), "spooky_crate", true);

    // Spring Crate
    public static final DeferredItem<CosmeticItem> FLOWER_BASKET = cosmetic("flower_basket", HAND, COMMON, true, paint(PINK, 2), "spring_crate", true);
    public static final DeferredItem<CosmeticItem> BONNET = cosmetic("bonnet", HAT, COMMON, true, paint(LIGHT_BLUE, 2), "spring_crate", true);
    public static final DeferredItem<CosmeticItem> BUTTERFLY_ANTENNAE = cosmetic("butterfly_antennae", HAT, COMMON, true, paint(PURPLE, 3), "spring_crate", true);
    public static final DeferredItem<CosmeticItem> LADYBUG_WINGS = cosmetic("ladybug_wings", BACK, COMMON, true, paint(RED, 3), "spring_crate", true);
    public static final DeferredItem<CosmeticItem> BUTTERFLY_WINGS = cosmetic("butterfly_wings", BACK, RARE, true, paint(PURPLE, 3), "spring_crate", true);
    public static final DeferredItem<CosmeticItem> BLOOM_AURA = cosmetic("bloom_aura", BACK, EPIC, true, paint(PINK, 1), "spring_crate", false);
    public static final DeferredItem<CosmeticItem> BLOOMING_UMBRELLA = cosmetic("blooming_umbrella", HAND, EPIC, true, paint(PURPLE, 3), "spring_crate", true);

    // Toy Crate
    public static final DeferredItem<CosmeticItem> GIFT_SACK = cosmetic("gift_sack", BACK, COMMON, true, null, "toy_crate", true);
    public static final DeferredItem<CosmeticItem> SHAKO_HAT = cosmetic("shako_hat", HAT, COMMON, true, paint(RED, 3), "toy_crate", true);
    public static final DeferredItem<CosmeticItem> TOY_WINDER = cosmetic("toy_winder", BACK, EPIC, true, paint(YELLOW, 6), "toy_crate", true);
    public static final DeferredItem<CosmeticItem> DRUM_KIT = cosmetic("drum_kit", BACK, LEGENDARY, true, paint(RED, 3), "toy_crate", false);

    // Western Crate
    public static final DeferredItem<CosmeticItem> WESTERN_HAT = cosmetic("western_hat", HAT, COMMON, true, paint(GREEN, 1), "western_crate", true);
    public static final DeferredItem<CosmeticItem> CACTUS_HELMET = cosmetic("cactus_helmet", HAT, RARE, true, paint(PINK, 2), "western_crate", false);
    public static final DeferredItem<CosmeticItem> CACTUS_SUIT = cosmetic("cactus_suit", BACK, EPIC, true, paint(PINK, 2), "western_crate", true);

    // Winter Crate
    public static final DeferredItem<CosmeticItem> WINTER_SCARF = cosmetic("winter_scarf", BACK, COMMON, true, paint(LIGHT_BLUE, 5), "winter_crate", true);
    public static final DeferredItem<CosmeticItem> SKI_HAT = cosmetic("ski_hat", HAT, COMMON, true, paint(LIGHT_BLUE, 5), "winter_crate", true);
    public static final DeferredItem<CosmeticItem> MITTENS = cosmetic("mittens", HAND, COMMON, true, paint(LIGHT_BLUE, 4), "winter_crate", true);
    public static final DeferredItem<CosmeticItem> SNOW_GLOBE = cosmetic("snow_globe", BACK, RARE, true, null, "winter_crate", true);
    public static final DeferredItem<CosmeticItem> SNOWMAN_HELMET = cosmetic("snowman_helmet", HAT, RARE, true, paint(RED, 1), "winter_crate", true);
    public static final DeferredItem<CosmeticItem> SNOWMAN_SUIT = cosmetic("snowman_suit", BACK, RARE, true, paint(RED, 1), "winter_crate", true);
    public static final DeferredItem<CosmeticItem> SNOW_SHOVEL = cosmetic("snow_shovel", HAND, RARE, true, paint(LIGHT_BLUE, 4), "winter_crate", true);
    public static final DeferredItem<CosmeticItem> SKIIS = cosmetic("skiis", BACK, EPIC, true, paint(LIGHT_BLUE, 5), "winter_crate", true);

    // Wizardry Crate
    public static final DeferredItem<CosmeticItem> WIZARD_HAT = cosmetic("wizard_hat", HAT, RARE, true, paint(PURPLE, 6), "wizardry_crate", true);
    public static final DeferredItem<CosmeticItem> WIZARD_STAFF = cosmetic("wizard_staff", HAND, RARE, true, paint(PURPLE, 2), "wizardry_crate", true);
    public static final DeferredItem<CosmeticItem> WITCH_CAULDRON_COSMETIC = cosmetic("witch_cauldron_cosmetic", BACK, RARE, true, paint(PURPLE, 1), "wizardry_crate", false);
    public static final DeferredItem<CosmeticItem> WIZARD_ROBE = cosmetic("wizard_robe", BACK, EPIC, true, paint(PURPLE, 6), "wizardry_crate", true);

    // Zodiac Crate
    public static final DeferredItem<CosmeticItem> DRAGON_FLOAT_CLOAK = cosmetic("dragon_float_cloak", BACK, COMMON, true, paint(RED, 3), "zodiac_crate", false);
    public static final DeferredItem<CosmeticItem> DRAGON_FLOAT_HELMET = cosmetic("dragon_float_helmet", HAT, COMMON, true, paint(RED, 3), "zodiac_crate", false);
    public static final DeferredItem<CosmeticItem> PANDA_BACKPACK = cosmetic("panda_backpack", BACK, RARE, true, paint(RED, 3), "zodiac_crate", false);
    public static final DeferredItem<CosmeticItem> LUCKY_CAT = cosmetic("lucky_cat", BACK, EPIC, true, paint(RED, 3), "zodiac_crate", true);

    private MainCosmetics() {}

    public static void init(ApeiraRegistrar registrar) {}

    /** Every cosmetic, in registration order. */
    public static List<Cosmetic> all() {
        return Collections.unmodifiableList(ALL);
    }

    /**
     * Registers a cosmetic.
     *
     * @param crate    the crate it is exclusive to, for the "... Crate Exclusive" line ({@code tooltip.apeira.exclusive.<crate>})
     * @param modelled whether it has a 3-D model in {@code models/item/cosmetic/}
     */
    private static DeferredItem<CosmeticItem> cosmetic(String name, CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
            @Nullable Integer paint, String crate, boolean modelled) {
        Component exclusive = Component.translatable("tooltip.apeira.exclusive." + crate);
        DeferredItem<CosmeticItem> item = R.items().registerItem(name,
                properties -> new CosmeticItem(properties, slot, rarity, paintable, paint, exclusive));
        ALL.add(new Cosmetic(item, slot, rarity, modelled));
        return item;
    }
}
