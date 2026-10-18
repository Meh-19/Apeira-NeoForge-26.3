package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.cosmetic.Wardrobe;
import com.mojang.serialization.Codec;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments of the decor module. */
public final class DecorAttachments {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /**
     * {@code apeira:wardrobe}: the cosmetics a player wears. Saved (when not empty), kept on death like CosmoLib's, and
     * synced to every client tracking the player, so the change shows as soon as it is set.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Wardrobe>> WARDROBE = R.forRegistry(
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES).register("wardrobe", () -> AttachmentType.builder(() -> Wardrobe.EMPTY)
                    .serialize(Wardrobe.MAP_CODEC, wardrobe -> !wardrobe.isEmpty())
                    .copyOnDeath()
                    .sync(Wardrobe.STREAM_CODEC)
                    .build());

    /**
     * {@code apeira:crate_luck}: a player's {@link com.apeira.decor.crate.CrateLuck crate luck}, 0 to 60. Saved (when
     * not 0) and kept on death. Only the server reads it, so it is not synced.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> CRATE_LUCK = R.forRegistry(
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES).register("crate_luck", () -> AttachmentType.builder(() -> 0.0F)
                    .serialize(Codec.FLOAT.fieldOf("luck"), luck -> luck != 0.0F)
                    .copyOnDeath()
                    .build());

    private DecorAttachments() {}

    public static void init(ApeiraRegistrar registrar) {}
}
