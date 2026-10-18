package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.cosmetic.Wardrobe;

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

    private DecorAttachments() {}

    public static void init(ApeiraRegistrar registrar) {}
}
