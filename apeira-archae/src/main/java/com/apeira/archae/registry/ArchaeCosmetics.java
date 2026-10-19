package com.apeira.archae.registry;

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

import com.apeira.archae.ApeiraArchae;
import com.apeira.archae.item.FragmentItem;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticRarity;
import com.apeira.decor.cosmetic.CosmeticSlot;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Argon's 25 cosmetics ({@code net.meh.argon.cosmetic.ModCosmeticItems}), with Argon's ids, slots, rarities,
 * paintability, fragments and costs, in Argon's order. Argon gave none a default paint. Each is assembled in the
 * assembly table from {@code cost} of its fragment and shows the "Archeology Exclusive" line. Every one has Argon's 3-D
 * model ({@code models/item/cosmetic/<name>.json}, textures in {@code textures/item/cosmetics/archaeology/}), shown when
 * worn; elsewhere it shows its rarity token.
 */
public final class ArchaeCosmetics {
    private static final List<Cosmetic> ALL = new ArrayList<>();
    private static final Component EXCLUSIVE = Component.translatable("tooltip.apeira.exclusive.archaeology");

    /** A registered cosmetic and the assembly recipe that makes it. */
    public record Cosmetic(DeferredItem<CosmeticItem> item, CosmeticSlot slot, CosmeticRarity rarity,
            DeferredItem<FragmentItem> fragment, int cost) {}

    public static final DeferredItem<CosmeticItem> SKULL_HELMET = cosmetic("skull_helmet", HAT, ArchaeItems.OBSCURE_BONES, 8, true, RARE);
    public static final DeferredItem<CosmeticItem> EXPLORERS_TORCH = cosmetic("explorers_torch", HAND, ArchaeItems.ANCIENT_OIL, 8, false, RARE);
    public static final DeferredItem<CosmeticItem> ANCIENT_AMULET = cosmetic("ancient_amulet", BACK, ArchaeItems.LOST_JEWELS, 10, true, RARE);
    public static final DeferredItem<CosmeticItem> RAID_CAPTAIN_HELMET = cosmetic("raid_captain_helmet", HAT, ArchaeItems.ARCANE_GOLD, 13, true, RARE);
    public static final DeferredItem<CosmeticItem> EVOKER_ROBE = cosmetic("evoker_robe", BACK, ArchaeItems.ARCANE_GOLD, 25, true, LEGENDARY);
    public static final DeferredItem<CosmeticItem> RAID_CAPTAIN_BANNER = cosmetic("raid_captain_banner", BACK, ArchaeItems.RAVAGED_HIDE, 16, true, EPIC);
    public static final DeferredItem<CosmeticItem> RAID_HORN = cosmetic("raid_horn", HAND, ArchaeItems.SINISTEEL_PIECE, 13, false, RARE);
    public static final DeferredItem<CosmeticItem> SILENCED_SKULL_HELMET = cosmetic("silenced_skull_helmet", HAT, ArchaeItems.HOLLOW_BONES, 9, true, RARE);
    public static final DeferredItem<CosmeticItem> FROSTLANDER_HAMMER = cosmetic("frostlander_hammer", HAND, ArchaeItems.FRIGID_ALLOY, 25, true, LEGENDARY);
    public static final DeferredItem<CosmeticItem> FUR_PELT = cosmetic("fur_pelt", BACK, ArchaeItems.PRIMITIVE_FUR, 16, true, EPIC);
    public static final DeferredItem<CosmeticItem> NOMAD_HELMET = cosmetic("nomad_helmet", HAT, ArchaeItems.SEVERED_HORN, 13, true, RARE);
    public static final DeferredItem<CosmeticItem> ANCIENT_CANE = cosmetic("ancient_cane", HAND, ArchaeItems.SOVEREIGN_TREASURE, 15, true, RARE);
    public static final DeferredItem<CosmeticItem> ANCIENT_HELMET = cosmetic("ancient_helmet", HAT, ArchaeItems.SOVEREIGN_TREASURE, 23, true, LEGENDARY);
    public static final DeferredItem<CosmeticItem> ANCIENT_POT = cosmetic("ancient_pot", BACK, ArchaeItems.LIMESTONE_PIECE, 16, true, RARE);
    public static final DeferredItem<CosmeticItem> SCARAB_WINGS = cosmetic("scarab_wings", BACK, ArchaeItems.SOVEREIGN_TREASURE, 25, true, LEGENDARY);
    public static final DeferredItem<CosmeticItem> IGUANO_MASK = cosmetic("iguano_mask", HAT, ArchaeItems.TRIBAL_TIMBER, 9, true, RARE);
    public static final DeferredItem<CosmeticItem> BOAR_HORN = cosmetic("boar_horn", HAND, ArchaeItems.GILDED_METAL, 10, false, RARE);
    public static final DeferredItem<CosmeticItem> BRUTE_HELMET = cosmetic("brute_helmet", HAT, ArchaeItems.GILDED_METAL, 10, false, RARE);
    public static final DeferredItem<CosmeticItem> SPORE_MEDIC_MASK = cosmetic("spore_medic_mask", HAT, ArchaeItems.SEARING_SPORES, 8, false, COMMON);
    public static final DeferredItem<CosmeticItem> HOG_HOOD = cosmetic("hog_hood", HAT, ArchaeItems.HOG_HIDE, 13, true, RARE);
    public static final DeferredItem<CosmeticItem> GRAVITY_HAMMER = cosmetic("gravity_hammer", HAND, ArchaeItems.MYSTICAL_MINERAL, 13, true, LEGENDARY);
    public static final DeferredItem<CosmeticItem> ENVOYS_GUISE = cosmetic("envoys_guise", HAT, ArchaeItems.HOLY_STONES, 13, true, RARE);
    public static final DeferredItem<CosmeticItem> GLISTENING_WREATH_CROWN = cosmetic("glistening_wreath_crown", HAT, ArchaeItems.AETHERIC_GOLD, 9, false, RARE);
    public static final DeferredItem<CosmeticItem> GLORIFIED_GAUNTLET = cosmetic("glorified_gauntlet", HAND, ArchaeItems.AETHERIC_GOLD, 13, true, RARE);
    public static final DeferredItem<CosmeticItem> ANCIENT_MASK = cosmetic("ancient_mask", HAT, ArchaeItems.MOSSY_REMNANTS, 16, true, EPIC);

    private ArchaeCosmetics() {}

    public static void init(ApeiraRegistrar registrar) {}

    /** Every cosmetic, in Argon's order. */
    public static List<Cosmetic> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Argon's {@code registerArtifactCosmetic(id, slot, fragment, cost, paintable, showInGroup, rarity)}; all were shown in its tab. */
    private static DeferredItem<CosmeticItem> cosmetic(String name, CosmeticSlot slot, DeferredItem<FragmentItem> fragment, int cost,
            boolean paintable, CosmeticRarity rarity) {
        DeferredItem<CosmeticItem> item = ApeiraArchae.REGISTRAR.items().registerItem(name,
                properties -> new CosmeticItem(properties, slot, rarity, paintable, null, EXCLUSIVE));
        ALL.add(new Cosmetic(item, slot, rarity, fragment, cost));
        return item;
    }
}
