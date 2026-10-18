package com.apeira.decor.datagen;

import java.util.Locale;

import com.apeira.core.datagen.ApeiraLanguageProvider;
import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.PaintColor;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorEntities;
import com.apeira.decor.registry.DecorItems;

import net.minecraft.data.PackOutput;

/** English names for the decor module's blocks, items, entities and painting table. */
public final class DecorLanguageProvider extends ApeiraLanguageProvider {
    public DecorLanguageProvider(PackOutput output, String namespace) {
        super(output, namespace);
    }

    @Override
    protected void addTranslations() {
        block(DecorBlocks.OAK_CHAIR, "Oak Chair");
        block(DecorBlocks.PAINTING_TABLE, "Painting Table");
        item(DecorItems.PAINTBRUSH, "Paintbrush");

        addEntityType(DecorEntities.SEAT, "Seat");

        container("painting_table", "Painting Table");
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

    /** {@code LIGHT_BLUE} -> {@code Light Blue}. */
    private static String title(String constant) {
        StringBuilder title = new StringBuilder();
        for (String word : constant.split("_")) {
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return title.toString();
    }
}
