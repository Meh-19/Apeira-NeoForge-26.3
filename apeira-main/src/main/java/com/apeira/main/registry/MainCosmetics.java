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
    private static final Component FAST_FOOD_CRATE = CosmeticItem.exclusive("fast_food_crate");
    private static final Component CONSTRUCTION_CRATE = CosmeticItem.exclusive("construction_crate");

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
