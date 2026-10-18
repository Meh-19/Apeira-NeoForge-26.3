package com.apeira.decor.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.decor.menu.WardrobeMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the player pressed the wardrobe key. The server opens the {@link WardrobeMenu}. */
public record OpenWardrobePayload() implements ApeiraPayload<OpenWardrobePayload> {
    public static final OpenWardrobePayload INSTANCE = new OpenWardrobePayload();
    public static final ApeiraPayloadType<OpenWardrobePayload> TYPE = ApeiraPayloadType.unit("open_wardrobe", INSTANCE);

    @Override
    public ApeiraPayloadType<OpenWardrobePayload> payloadType() {
        return TYPE;
    }

    /** Opens the wardrobe, unless the player is already in a menu or is a spectator. */
    public static void handle(OpenWardrobePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator() && player.containerMenu == player.inventoryMenu) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> WardrobeMenu.server(id, inventory),
                    Component.translatable("container.apeira.wardrobe")));
        }
    }
}
