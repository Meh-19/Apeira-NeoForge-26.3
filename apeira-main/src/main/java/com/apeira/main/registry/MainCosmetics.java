package com.apeira.main.registry;

import static com.apeira.decor.cosmetic.CosmeticRarity.COMMON;
import static com.apeira.decor.cosmetic.CosmeticRarity.EPIC;
import static com.apeira.decor.cosmetic.CosmeticRarity.RARE;
import static com.apeira.decor.cosmetic.CosmeticSlot.BACK;
import static com.apeira.decor.cosmetic.CosmeticSlot.HAND;
import static com.apeira.decor.cosmetic.CosmeticSlot.HAT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticRarity;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.main.Apeira;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Animal Crate's cosmetics, from Ren's F9 capture of its reward page ("Next Cycle"): names, rarities (the name's
 * rarity glyph) and default paints (the captured {@code minecraft:firework_explosion} colour), the server pack's
 * {@code models/custom/cosmetics/} models as {@code models/item/cosmetic/<name>.json} (textures in
 * {@code textures/item/cosmetics/animal/}), all paintable on tint index 1. They show their model when worn and their
 * rarity token elsewhere ({@code MainModelProvider#cosmetic}). The balloons (Parrot, Snake) and the Monkey Mode gesture
 * are left out: Apeira has no balloon slot or gestures.
 */
public final class MainCosmetics {
    private static final List<Cosmetic> ALL = new ArrayList<>();
    private static final Component ANIMAL_CRATE = Component.translatable("tooltip.apeira.exclusive.animal_crate");

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

    private MainCosmetics() {}

    public static void init(ApeiraRegistrar registrar) {}

    /** Every cosmetic, in the capture's order. */
    public static List<Cosmetic> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static DeferredItem<CosmeticItem> cosmetic(String name, CosmeticSlot slot, CosmeticRarity rarity, int defaultPaint) {
        DeferredItem<CosmeticItem> item = Apeira.REGISTRAR.items().registerItem(name,
                properties -> new CosmeticItem(properties, slot, rarity, true, defaultPaint, ANIMAL_CRATE));
        ALL.add(new Cosmetic(item, slot, rarity));
        return item;
    }
}
