package com.apeira.main.registry;

import static com.apeira.decor.cosmetic.CosmeticRarity.COMMON;
import static com.apeira.decor.cosmetic.CosmeticRarity.EPIC;
import static com.apeira.decor.cosmetic.CosmeticRarity.LEGENDARY;
import static com.apeira.decor.cosmetic.CosmeticRarity.RARE;
import static com.apeira.decor.cosmetic.CosmeticSlot.BACK;
import static com.apeira.decor.cosmetic.CosmeticSlot.HAND;
import static com.apeira.decor.cosmetic.CosmeticSlot.HAT;

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
 * The crates' cosmetics. The Animal Crate's, from Ren's F9 capture of its reward page ("Next Cycle"): names, rarities (the name's
 * rarity glyph) and default paints (the captured {@code minecraft:firework_explosion} colour), the server pack's
 * {@code models/custom/cosmetics/} models as {@code models/item/cosmetic/<name>.json} (textures in
 * {@code textures/item/cosmetics/animal/}), all paintable on tint index 1. They show their model when worn and their
 * rarity token elsewhere ({@code MainModelProvider#cosmetic}). The balloons (Parrot, Snake) and the Monkey Mode gesture
 * are left out: Apeira has no balloon slot or gestures.
 */
public final class MainCosmetics {
    private static final List<Cosmetic> ALL = new ArrayList<>();
    private static final Component ANIMAL_CRATE = Component.translatable("tooltip.apeira.exclusive.animal_crate");
    private static final Component BATHROOM_CRATE = CosmeticItem.exclusive("bathroom_crate");
    private static final Component BUSINESS_CRATE = CosmeticItem.exclusive("business_crate");
    private static final Component CAMPING_CRATE = CosmeticItem.exclusive("camping_crate");
    private static final Component COSMETIC_CRATE = CosmeticItem.exclusive("cosmetic_crate");
    private static final Component FAST_FOOD_CRATE = CosmeticItem.exclusive("fast_food_crate");
    private static final Component FUN_FAIR_CRATE = CosmeticItem.exclusive("fun_fair_crate");
    private static final Component CONSTRUCTION_CRATE = CosmeticItem.exclusive("construction_crate");
    private static final Component KITCHEN_CRATE = CosmeticItem.exclusive("kitchen_crate");
    private static final Component MEDIEVAL_CRATE = CosmeticItem.exclusive("medieval_crate");
    private static final Component MUSIC_CRATE = CosmeticItem.exclusive("music_crate");
    private static final Component SPACE_CRATE = CosmeticItem.exclusive("space_crate");
    private static final Component MINING_CRATE = CosmeticItem.exclusive("mining_crate");
    private static final Component WESTERN_CRATE = CosmeticItem.exclusive("western_crate");
    private static final Component WIZARDRY_CRATE = CosmeticItem.exclusive("wizardry_crate");

    /** A registered cosmetic, its slot and rarity. */
    public record Cosmetic(DeferredItem<CosmeticItem> item, CosmeticSlot slot, CosmeticRarity rarity) {}

    public static final DeferredItem<CosmeticItem> AXOLOTL_HAT = cosmetic("axolotl_hat", HAT, COMMON, 0xFFA8DE);
    public static final DeferredItem<CosmeticItem> ANIMAL_EARS = cosmetic("animal_ears", HAT, COMMON, 0x835432);
    public static final DeferredItem<CosmeticItem> WOLF_HELMET = cosmetic("wolf_helmet", HAT, COMMON, 0xB2B2B2);
    public static final DeferredItem<CosmeticItem> WOLF_TAIL = cosmetic("wolf_tail", BACK, COMMON, 0xB2B2B2);
    public static final DeferredItem<CosmeticItem> WOLF_PAW = cosmetic("wolf_paw", HAND, COMMON, 0xB2B2B2);
    public static final DeferredItem<CosmeticItem> MOOSE_ANTLERS = cosmetic("moose_antlers", HAT, RARE, 0xA0643E);
    public static final DeferredItem<CosmeticItem> GORILLA_MASK = cosmetic("gorilla_mask", HAT, RARE, 0x4C4C4C);
    public static final DeferredItem<CosmeticItem> MASCOT_DOG_HELMET = cosmetic("mascot_dog_helmet", HAT, RARE, 0xFA9B5C);
    public static final DeferredItem<CosmeticItem> MONKEY_BACKSWAG = cosmetic("monkey_backswag", BACK, EPIC, 0xA0643E);
    public static final DeferredItem<CosmeticItem> GIRAFFE_HELMET = cosmetic("giraffe_helmet", HAT, EPIC, 0xF89D25);

    // The Bathroom Crate's, from Ren's F9 capture of its reward page (the server pack's models/custom/cosmetics/bathroom/,
    // textures in textures/item/cosmetics/bathroom/; the rubber duckie hat and tub also use bathroom furniture textures).
    // The plunger's linked gesture and the Flushed gesture are left out. The tub's capture has no paint.
    public static final DeferredItem<CosmeticItem> PLUNGER = cosmetic("plunger", HAND, RARE, 0xCB3C2A, BATHROOM_CRATE);
    public static final DeferredItem<CosmeticItem> RUBBER_DUCKIE_HAT = cosmetic("rubber_duckie_hat", HAT, EPIC, 0x74A4CA, BATHROOM_CRATE);
    public static final DeferredItem<CosmeticItem> TUB = cosmetic("tub", BACK, LEGENDARY, null, BATHROOM_CRATE);
    // The Business Crate's, from Ren's F9 capture of its reward page: the server pack's suit_case, top_hat and
    // business/money_sack (textures in textures/item/cosmetics/business/). The Money Rain gesture is left out.
    public static final DeferredItem<CosmeticItem> SUIT_CASE = cosmetic("suit_case", HAND, COMMON, 0xAC5353, BUSINESS_CRATE);
    public static final DeferredItem<CosmeticItem> TOP_HAT = cosmetic("top_hat", HAT, COMMON, 0xCB5148, BUSINESS_CRATE);
    public static final DeferredItem<CosmeticItem> MONEY_SACK = cosmetic("money_sack", BACK, EPIC, 0xFFC766, BUSINESS_CRATE);
    // The Camping Crate's, from Ren's F9 capture of its reward page: the pack's camping/flashlight_prop, log_backswag,
    // chainsaw_prop and cuckcoo_clock (textures in textures/item/cosmetics/camping/). The Binoculars gesture and the
    // chainsaw's linked gesture are left out; the chainsaw's capture is white, so it has no default paint.
    public static final DeferredItem<CosmeticItem> FLASHLIGHT = cosmetic("flashlight", HAND, COMMON, 0x93BA4F, CAMPING_CRATE);
    public static final DeferredItem<CosmeticItem> LOG_BUNDLE = cosmetic("log_bundle", BACK, RARE, 0x884B44, CAMPING_CRATE);
    public static final DeferredItem<CosmeticItem> CHAINSAW = cosmetic("chainsaw", HAND, EPIC, null, CAMPING_CRATE);
    public static final DeferredItem<CosmeticItem> CUCKOO_CLOCK = cosmetic("cuckoo_clock", BACK, LEGENDARY, 0xD499FF, CAMPING_CRATE);
    // The Construction Crate's, from Ren's F9 capture of its reward page: the server pack's hard_hat, traffic_cone,
    // construction/treads and construction/crane_backswag (textures in textures/item/cosmetics/construction/). The
    // Jackhammer gesture and the crane's linked gesture are left out. The treads' slot is a guess (there is no feet slot).
    public static final DeferredItem<CosmeticItem> HARD_HAT = cosmetic("hard_hat", HAT, COMMON, 0xE7A127, CONSTRUCTION_CRATE);
    public static final DeferredItem<CosmeticItem> TRAFFIC_CONE_HAT = cosmetic("traffic_cone_hat", HAT, COMMON, 0xFD811C, CONSTRUCTION_CRATE);
    public static final DeferredItem<CosmeticItem> TREADS = cosmetic("treads", BACK, EPIC, 0xF89D25, CONSTRUCTION_CRATE);
    public static final DeferredItem<CosmeticItem> CRANE = cosmetic("crane", BACK, LEGENDARY, 0xF7C021, CONSTRUCTION_CRATE);
    // The Fast Food Crate's, from Ren's F9 capture of its reward page: the server pack's large_fork, large_spoon,
    // fry_pack and burger_cap (textures in textures/item/cosmetics/fast_food/). The Donut Balloon is left out, as the
    // Animal Crate's balloons are.
    public static final DeferredItem<CosmeticItem> COMICALLY_LARGE_FORK = cosmetic("comically_large_fork", HAND, COMMON, 0xCB3C2A, FAST_FOOD_CRATE);
    public static final DeferredItem<CosmeticItem> COMICALLY_LARGE_SPOON = cosmetic("comically_large_spoon", HAND, COMMON, 0xCB3C2A, FAST_FOOD_CRATE);
    public static final DeferredItem<CosmeticItem> FRY_PACK = cosmetic("fry_pack", BACK, RARE, 0xCB3C2A, FAST_FOOD_CRATE);
    public static final DeferredItem<CosmeticItem> BURGER_CAP = cosmetic("burger_cap", HAT, RARE, 0xCB3C2A, FAST_FOOD_CRATE);
    // The Cosmetic Crate's, from Ren's F9 capture of its reward pages (two pages): the server pack's cosmetics models
    // (hairs are its male_hair/female_hair models, the adventurer stick the traveler's staff; textures in
    // textures/item/cosmetics/cosmetic/). Left out: the Basic Kite, Hot Air Balloon and 0R-Bit Balloon (balloons, as the
    // Animal Crate's are). The Starry Blanket Cloak is the updated pack's starry_pajamas_cloak. White and paper items have no default
    // paint. Slots follow the model: hats, hair, helmets and masks on the head, staffs, claws and fists in the hand,
    // packs, tails, suits and chests on the back.
    public static final DeferredItem<CosmeticItem> MOHAWK_HAIR = cosmetic("mohawk_hair", HAT, COMMON, 0x835432, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> BASEBALL_CAP = cosmetic("baseball_cap", HAT, COMMON, 0x4D7A60, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FADED_HAIR = cosmetic("faded_hair", HAT, COMMON, 0xA0643E, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> AFRO_HAIR = cosmetic("afro_hair", HAT, COMMON, 0x884B44, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> PONYTAIL_HAIR = cosmetic("ponytail_hair", HAT, COMMON, 0xA0643E, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> HAIR_BUNS = cosmetic("hair_buns", HAT, COMMON, 0x835432, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> WEAVE_PONYTAIL_HAIR = cosmetic("weave_ponytail_hair", HAT, COMMON, 0x835432, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> STRAW_HAT = cosmetic("straw_hat", HAT, COMMON, 0xB0DFE5, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> SUN_HAT = cosmetic("sun_hat", HAT, COMMON, 0xD499FF, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> HEADPHONES_HAT = cosmetic("headphones_hat", HAT, COMMON, 0x8D6397, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> KNITTED_HAT = cosmetic("knitted_hat", HAT, COMMON, 0x81D8D0, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> BERET = cosmetic("beret", HAT, COMMON, 0xB0DFE5, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FEDORA = cosmetic("fedora", HAT, COMMON, 0xFD811C, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> BEANIE = cosmetic("beanie", HAT, COMMON, 0x4C4C4C, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FLOWERBUD_CROWN = cosmetic("flowerbud_crown", HAT, COMMON, 0xFB8EB0, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> MUG_HAT = cosmetic("mug_hat", HAT, COMMON, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> RUSTYS_HAT = cosmetic("rustys_hat", HAT, COMMON, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> ADVENTURER_STICK = cosmetic("adventurer_stick", HAND, COMMON, 0x93BA4F, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> CAPTAIN_HAT = cosmetic("captain_hat", HAT, COMMON, 0xB0DFE5, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> BACKPACK = cosmetic("backpack", BACK, COMMON, 0x884B44, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> TRAVEL_CASE = cosmetic("travel_case", HAND, COMMON, 0xA0643E, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FOAM_FINGER = cosmetic("foam_finger", HAND, COMMON, 0x93BA4F, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> TOILET_PAPER_STAFF = cosmetic("toilet_paper_staff", HAND, COMMON, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> DINO_TAIL = cosmetic("dino_tail", BACK, COMMON, 0x8D6397, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> TRIBAL_STAFF = cosmetic("tribal_staff", HAND, COMMON, 0xFD811C, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> DINO_CLAW = cosmetic("dino_claw", HAND, COMMON, 0x8D6397, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> PIRATE_HOOK = cosmetic("pirate_hook", HAND, COMMON, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> SAMURAI_GAUNTLET = cosmetic("samurai_gauntlet", HAND, COMMON, 0x883A3F, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FOOTBALL_HELMET = cosmetic("football_helmet", HAT, RARE, 0xF89D25, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> TELEVISION_HELMET = cosmetic("television_helmet", HAT, RARE, 0x4E617E, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> CARDBOARD_HELMET = cosmetic("cardboard_helmet", HAT, RARE, 0xCB5148, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> CARDBOARD_SUIT = cosmetic("cardboard_suit", BACK, RARE, 0xCB5148, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> TRIBAL_MASK = cosmetic("tribal_mask", HAT, RARE, 0xFD811C, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> PIRATE_HAT = cosmetic("pirate_hat", HAT, RARE, 0xCB3C2A, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> DINO_HELMET = cosmetic("dino_helmet", HAT, RARE, 0x8D6397, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> PIRATE_CHEST = cosmetic("pirate_chest", BACK, RARE, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> SAMURAI_SWORDS = cosmetic("samurai_swords", BACK, RARE, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FLAME_FIST = cosmetic("flame_fist", HAND, RARE, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> REPLICA_MAYOR_HAT = cosmetic("replica_mayor_hat", HAT, EPIC, null, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> FISH_BOWL_HELMET = cosmetic("fish_bowl_helmet", HAT, EPIC, 0x169B9C, COSMETIC_CRATE);
    public static final DeferredItem<CosmeticItem> STARRY_BLANKET_CLOAK = cosmetic("starry_blanket_cloak", BACK, LEGENDARY, 0xD499FF, COSMETIC_CRATE);

    // The Fun Fair Crate's, from Ren's F9 capture of its reward page: the Party Hat is the server pack's fun_fair/clown_hat
    // (texture in textures/item/cosmetics/fun_fair/). The Rubber Balloon is left out, as the other crates' balloons are.
    public static final DeferredItem<CosmeticItem> PARTY_HAT = cosmetic("party_hat", HAT, COMMON, 0xF7C021, FUN_FAIR_CRATE);


    // The Kitchen, Medieval, Music, Space, Western and Wizardry Crates' cosmetics, from Ren's F9 captures of their reward pages
    // (rarity from the name's glyph, default paint from the firework colour, white none; slots from the backswag logs and the
    // models' head and hand displays). Balloons, gestures and tool skins are left out.
    public static final DeferredItem<CosmeticItem> CHEF_HAT = cosmetic("chef_hat", HAT, COMMON, null, KITCHEN_CRATE);
    public static final DeferredItem<CosmeticItem> APRON = cosmetic("apron", BACK, RARE, 0xF89D25, KITCHEN_CRATE);
    public static final DeferredItem<CosmeticItem> PANCAKE_STACK = cosmetic("pancake_stack", HAT, EPIC, 0xF7C021, KITCHEN_CRATE);
    public static final DeferredItem<CosmeticItem> SOUP_POT = cosmetic("soup_pot", BACK, LEGENDARY, null, KITCHEN_CRATE);
    public static final DeferredItem<CosmeticItem> JESTER_HAT = cosmetic("jester_hat", HAT, COMMON, 0x5B7816, MEDIEVAL_CRATE);
    public static final DeferredItem<CosmeticItem> KNIGHT_ARMOR = cosmetic("knight_armor", BACK, RARE, 0xA0643E, MEDIEVAL_CRATE);
    public static final DeferredItem<CosmeticItem> KNIGHT_HELMET = cosmetic("knight_helmet", HAT, RARE, 0xCB3C2A, MEDIEVAL_CRATE);
    public static final DeferredItem<CosmeticItem> LUTE = cosmetic("lute", HAND, RARE, null, MEDIEVAL_CRATE);
    public static final DeferredItem<CosmeticItem> DRAGON_WINGS = cosmetic("dragon_wings", BACK, EPIC, 0xCB3C2A, MEDIEVAL_CRATE);
    public static final DeferredItem<CosmeticItem> ROYAL_CROWN = cosmetic("royal_crown", HAT, EPIC, 0xCB3C2A, MEDIEVAL_CRATE);
    public static final DeferredItem<CosmeticItem> EDM_HEADSET = cosmetic("edm_headset", HAT, COMMON, 0x81D8D0, MUSIC_CRATE);
    public static final DeferredItem<CosmeticItem> SAXOPHONE = cosmetic("saxophone", HAND, RARE, 0xF7C021, MUSIC_CRATE);
    public static final DeferredItem<CosmeticItem> GUITAR = cosmetic("guitar", HAND, RARE, 0xCB5148, MUSIC_CRATE);
    public static final DeferredItem<CosmeticItem> TUBA = cosmetic("tuba", BACK, EPIC, 0xE88438, MUSIC_CRATE);
    public static final DeferredItem<CosmeticItem> EDM_AURA = cosmetic("edm_aura", BACK, LEGENDARY, 0x81D8D0, MUSIC_CRATE);
    public static final DeferredItem<CosmeticItem> ASTRONAUT_HELMET = cosmetic("astronaut_helmet", HAT, RARE, 0xFD811C, SPACE_CRATE);
    public static final DeferredItem<CosmeticItem> ASTRONAUT_AIR_TANK = cosmetic("astronaut_air_tank", BACK, RARE, 0xFD811C, SPACE_CRATE);
    public static final DeferredItem<CosmeticItem> SUSSY_HELMET = cosmetic("sussy_helmet", HAT, RARE, 0xCB3C2A, SPACE_CRATE);
    public static final DeferredItem<CosmeticItem> STELLAR_HAT = cosmetic("stellar_hat", HAT, LEGENDARY, 0xCB66F9, SPACE_CRATE);
    public static final DeferredItem<CosmeticItem> WESTERN_HAT = cosmetic("western_hat", HAT, COMMON, 0x93BA4F, WESTERN_CRATE);
    public static final DeferredItem<CosmeticItem> CACTUS_HELMET = cosmetic("cactus_helmet", HAT, RARE, 0xFB8EB0, WESTERN_CRATE);
    public static final DeferredItem<CosmeticItem> CACTUS_SUIT = cosmetic("cactus_suit", BACK, EPIC, 0xFB8EB0, WESTERN_CRATE);
    public static final DeferredItem<CosmeticItem> WITCH_HAT = cosmetic("witch_hat", HAT, COMMON, 0xCB66F9, WIZARDRY_CRATE);
    public static final DeferredItem<CosmeticItem> WITCH_BROOM = cosmetic("witch_broom", HAND, COMMON, null, WIZARDRY_CRATE);
    public static final DeferredItem<CosmeticItem> WIZARD_HAT = cosmetic("wizard_hat", HAT, RARE, 0x8D4585, WIZARDRY_CRATE);
    public static final DeferredItem<CosmeticItem> WIZARD_STAFF = cosmetic("wizard_staff", HAND, RARE, 0x8643AD, WIZARDRY_CRATE);
    public static final DeferredItem<CosmeticItem> WITCH_CAULDRON = cosmetic("witch_cauldron", BACK, RARE, 0xCB66F9, WIZARDRY_CRATE);
    public static final DeferredItem<CosmeticItem> WIZARD_ROBE = cosmetic("wizard_robe", BACK, EPIC, 0x8D4585, WIZARDRY_CRATE);
    // The Mining Crate's (Ren's F9 capture of its reward page; models the pack's miner_cap, coal_pack (the Ore Backpack),
    // mining_crate/drill_prop and crystal_spikes, textures in textures/item/cosmetics/mining/; the backs' rows are Ren's
    // logged ones).
    public static final DeferredItem<CosmeticItem> MINER_CAP = cosmetic("miner_cap", HAT, COMMON, 0xE88438, MINING_CRATE);
    public static final DeferredItem<CosmeticItem> ORE_BACKPACK = cosmetic("ore_backpack", BACK, RARE, 0x999999, MINING_CRATE);
    public static final DeferredItem<CosmeticItem> DRILL = cosmetic("drill", HAND, EPIC, 0xF7C021, MINING_CRATE);
    public static final DeferredItem<CosmeticItem> CRYSTAL_SPIKES = cosmetic("crystal_spikes", BACK, LEGENDARY, 0xD499FF, MINING_CRATE);

    private MainCosmetics() {}

    public static void init(ApeiraRegistrar registrar) {}

    /** Every cosmetic, in the capture's order. */
    public static List<Cosmetic> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static DeferredItem<CosmeticItem> cosmetic(String name, CosmeticSlot slot, CosmeticRarity rarity, int defaultPaint) {
        return cosmetic(name, slot, rarity, defaultPaint, ANIMAL_CRATE);
    }

    private static DeferredItem<CosmeticItem> cosmetic(String name, CosmeticSlot slot, CosmeticRarity rarity, @Nullable Integer defaultPaint,
            Component exclusive) {
        DeferredItem<CosmeticItem> item = Apeira.REGISTRAR.items().registerItem(name,
                properties -> new CosmeticItem(properties, slot, rarity, true, defaultPaint, exclusive));
        ALL.add(new Cosmetic(item, slot, rarity));
        return item;
    }
}
