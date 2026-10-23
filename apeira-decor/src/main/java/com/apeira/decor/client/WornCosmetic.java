package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code apeira:worn_cosmetic} item model condition: true only while {@link CosmeticLayer} resolves the models of
 * a wardrobe's cosmetics, or a menu draws a worn cosmetic as its 3D model (the profile's cosmetic sidebar), so a
 * cosmetic's item definition shows its 3D model there and its token everywhere else (inventories, the hand, item
 * frames, the ground). Item definitions use it as
 * {@code {"type": "minecraft:condition", "property": "apeira:worn_cosmetic", ...}}.
 *
 * <p>Models are resolved on the render thread while render states are extracted, so a plain flag is enough.
 */
public record WornCosmetic() implements ConditionalItemModelProperty {
    public static final MapCodec<WornCosmetic> MAP_CODEC = MapCodec.unit(new WornCosmetic());

    private static boolean resolvingWorn;

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
            ItemDisplayContext displayContext) {
        return resolvingWorn;
    }

    /** Runs {@code resolve} with the condition true; item models are resolved inside it, as {@code GuiGraphicsExtractor#item} does. */
    public static void whileWorn(Runnable resolve) {
        resolvingWorn = true;
        try {
            resolve.run();
        } finally {
            resolvingWorn = false;
        }
    }

    @Override
    public MapCodec<WornCosmetic> type() {
        return MAP_CODEC;
    }
}
