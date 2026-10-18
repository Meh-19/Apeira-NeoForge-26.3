package com.apeira.decor.cosmetic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The cosmetics a player wears, one per {@link CosmeticSlot}: the {@code apeira:wardrobe} player attachment
 * ({@link com.apeira.decor.registry.DecorAttachments#WARDROBE}). It is saved with the player, kept on death, and sent
 * to every client that can see the player, which draws it with the cosmetic render layer.
 *
 * <p>A wardrobe is never changed in place: {@link #with} returns a new one, which is set on the player so the change
 * is synced.
 */
public record Wardrobe(ItemStack hat, ItemStack back, ItemStack hand) {
    public static final Wardrobe EMPTY = new Wardrobe(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

    public static final MapCodec<Wardrobe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("hat", ItemStack.EMPTY).forGetter(Wardrobe::hat),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("back", ItemStack.EMPTY).forGetter(Wardrobe::back),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("hand", ItemStack.EMPTY).forGetter(Wardrobe::hand))
            .apply(instance, Wardrobe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Wardrobe> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, Wardrobe::hat,
            ItemStack.OPTIONAL_STREAM_CODEC, Wardrobe::back,
            ItemStack.OPTIONAL_STREAM_CODEC, Wardrobe::hand,
            Wardrobe::new);

    /** The cosmetic worn in {@code slot}, or an empty stack. */
    public ItemStack get(CosmeticSlot slot) {
        return switch (slot) {
            case HAT -> hat;
            case BACK -> back;
            case HAND -> hand;
        };
    }

    /** A copy of this wardrobe with {@code stack} in {@code slot}. */
    public Wardrobe with(CosmeticSlot slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        return switch (slot) {
            case HAT -> new Wardrobe(copy, back, hand);
            case BACK -> new Wardrobe(hat, copy, hand);
            case HAND -> new Wardrobe(hat, back, copy);
        };
    }

    public boolean isEmpty() {
        return hat.isEmpty() && back.isEmpty() && hand.isEmpty();
    }

    /** Whether {@code stack} can be worn in {@code slot}: a cosmetic made for that slot. */
    public static boolean fits(CosmeticSlot slot, ItemStack stack) {
        return stack.getItem() instanceof CosmeticItem cosmetic && cosmetic.slot() == slot;
    }
}
