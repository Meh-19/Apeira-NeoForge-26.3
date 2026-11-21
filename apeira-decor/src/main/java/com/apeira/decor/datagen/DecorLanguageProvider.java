package com.apeira.decor.datagen;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.apeira.core.datagen.ApeiraLanguageProvider;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.PaintColor;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorEntities;
import com.apeira.decor.registry.DecorFurniture;
import com.apeira.decor.registry.DecorItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.DeferredBlock;

/** English names for the decor module's blocks, items, entities and painting table. */
public final class DecorLanguageProvider extends ApeiraLanguageProvider {
    /** Furniture names that are not just the id in title case (wall and ceiling variants take their base piece's name). */
    private static final Map<String, String> NAMES = Map.of("i_beam", "I-Beam", "santa_sleigh", "Santa's Sleigh", "dj_booth", "DJ Booth",
            "santa_chair", "Santa's Chair", "toilet_paper", "Toiletpaper");

    public DecorLanguageProvider(PackOutput output, String namespace) {
        super(output, namespace);
    }

    @Override
    protected void addTranslations() {
        block(DecorBlocks.PAINTING_TABLE, "Painting Table");
        item(DecorItems.PAINTBRUSH, "Paintbrush");

        for (DeferredBlock<? extends FurnitureBlock> furniture : DecorFurniture.all()) {
            String name = BuiltInRegistries.BLOCK.getKey(furniture.get().dropBlock()).getPath();
            block(furniture, NAMES.getOrDefault(name, title(name)));
        }
        Set<String> crates = new LinkedHashSet<>(List.of("badger_crate", "animal_crate", "bathroom_crate", "business_crate", "camping_crate", "cosmetic_crate", "fast_food_crate", "construction_crate"));
        // The same lines as the cosmetics' (the main module's language file), for the crates furniture comes from.
        for (String crate : crates) {
            tooltip("exclusive." + crate, title(crate) + " Exclusive");
        }

        addEntityType(DecorEntities.SEAT, "Seat");

        container("painting_table", "Painting Table");
        container("wardrobe", "Wardrobe");
        addId("key", "open_wardrobe", "Open Wardrobe");

        tooltip("paint.color", "Painted %s, Shade %s");
        tooltip("paint.finish", "%s Finish");
        message("paint_copied", "Copied: %s");
        for (PaintColor color : PaintColor.values()) {
            add(color.translationKey(), title(color.name()));
        }
        for (FinishType finish : FinishType.values()) {
            add(finish.translationKey(), title(finish.name()));
        }
    }

    /** {@code LIGHT_BLUE} or {@code light_blue} -> {@code Light Blue}. */
    private static String title(String constant) {
        StringBuilder title = new StringBuilder();
        for (String word : constant.split("_")) {
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return title.toString();
    }
}
