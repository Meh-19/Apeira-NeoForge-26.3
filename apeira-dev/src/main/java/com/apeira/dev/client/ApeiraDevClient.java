package com.apeira.dev.client;

import java.io.IOException;
import java.nio.file.Path;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.decor.cosmetic.BackOffset;
import com.apeira.decor.cosmetic.BackOffsets;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTunings;
import com.apeira.dev.ApeiraDev;
import com.apeira.dev.DevItems;
import com.apeira.dev.network.BackOffsetPayload;
import com.apeira.dev.network.FurnitureTuningPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client entrypoint for Apeira Dev: binds the dev wand to the furniture piece it right-clicks, turns Ctrl + scroll and
 * Alt + scroll into edits while it is held (of the bound piece, or, bound to nothing, of the worn back cosmetic), and
 * adds the HUD, the in-world visualizer and the editor key (for a back cosmetic: export, Shift to reset).
 */
@Mod(value = ApeiraDev.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraDevClient {
    /** Opens the furniture editor for the bound piece. */
    public static final KeyMapping OPEN_EDITOR = new KeyMapping("key.apeira.dev.furniture_editor", InputConstants.KEY_K, KeyMapping.Category.DEBUG);

    public ApeiraDevClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraDevClient::registerKeys);
        modEventBus.addListener(ApeiraDevClient::registerDebugRenderers);
        NeoForge.EVENT_BUS.addListener(ApeiraDevClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ApeiraDevClient::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(ApeiraDevClient::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ApeiraDevClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ApeiraDevClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(TuningHud::onRenderGui);
        ApeiraClientNetwork.create()
                .handle(FurnitureTuningPayload.TYPE, ApeiraDevClient::onTuning)
                .handle(BackOffsetPayload.TYPE, ApeiraDevClient::onBackOffset)
                .register(modEventBus);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
    }

    private static void registerDebugRenderers(RegisterDebugRenderersEvent event) {
        event.register(new TuningVisualizer());
    }

    /** The server's tuning of a piece, after anyone's edit or on joining. */
    private static void onTuning(FurnitureTuningPayload payload, IPayloadContext context) {
        if (BuiltInRegistries.BLOCK.getValue(payload.block()) instanceof FurnitureBlock block) {
            FurnitureTuning tuning = payload.tuning().orElse(null);
            FurnitureTunings.set(block, tuning);
            if (block == DevSession.block()) {
                DevSession.selectBox(DevSession.box(), DevSession.tuning(block));
            }
        }
    }

    /** The server's offset of a back cosmetic, after anyone's edit or on joining. */
    private static void onBackOffset(BackOffsetPayload payload, IPayloadContext context) {
        BuiltInRegistries.ITEM.getOptional(payload.item()).ifPresent(item -> BackOffsets.set(item, payload.offset().orElse(null)));
    }

    /** Binds the wand to a furniture piece; a sneaking click on anything else unbinds it. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !event.getItemStack().is(DevItems.FURNITURE_DEV_WAND)) {
            return;
        }
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof FurnitureBlock block) {
            DevSession.bind(event.getPos(), block);
        } else if (event.getEntity().isShiftKeyDown()) {
            DevSession.unbind();
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && event.getItemStack().is(DevItems.FURNITURE_DEV_WAND) && event.getEntity().isShiftKeyDown()) {
            DevSession.unbind();
        }
    }

    /** Ctrl + scroll changes the selected value (Shift for fine steps), Alt + scroll selects another. */
    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        FurnitureBlock block = DevSession.block();
        Item back = DevSession.backCosmetic();
        int steps = (int) Math.signum(event.getScrollDeltaY());
        if (steps == 0 || (block == null && back == null) || minecraft.gui.screen() != null || !DevSession.holdingWand(minecraft.player)
                || !(minecraft.hasControlDown() || minecraft.hasAltDown())) {
            return;
        }
        event.setCanceled(true);
        if (block == null) {
            if (minecraft.hasAltDown()) {
                DevSession.selectBackParameter(-steps);
                return;
            }
            BackOffset offset = DevSession.backOffset(back);
            BackOffset changed = DevSession.backParameter().adjust(offset, steps, minecraft.hasShiftDown());
            if (!changed.equals(offset)) {
                DevSession.applyBack(back, changed);
            }
            return;
        }
        if (minecraft.hasAltDown()) {
            DevSession.selectParameter(-steps);
            return;
        }
        FurnitureTuning tuning = DevSession.tuning(block);
        if (DevSession.parameter() == TuningParameter.BOX) {
            DevSession.selectBox(DevSession.box() + steps, tuning);
        } else {
            FurnitureTuning changed = DevSession.parameter().adjust(tuning, DevSession.box(), steps, minecraft.hasShiftDown());
            if (!changed.equals(tuning)) {
                DevSession.apply(block, changed);
            }
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (DevSession.target() != null && DevSession.block() == null && minecraft.level != null
                && minecraft.level.isLoaded(DevSession.target())) {
            DevSession.unbind();
        }
        while (OPEN_EDITOR.consumeClick()) {
            if (minecraft.gui.screen() == null && DevSession.holdingWand(minecraft.player)) {
                Item back = DevSession.backCosmetic();
                if (DevSession.block() != null) {
                    minecraft.gui.setScreen(new FurnitureEditorScreen());
                } else if (back != null && minecraft.hasShiftDown()) {
                    DevSession.resetBack(back);
                    DevSession.message(Component.translatable("message.apeira.dev.back_reset", back.getDefaultInstance().getHoverName()));
                } else if (back != null) {
                    exportBack(back);
                } else {
                    DevSession.message(Component.translatable("hud.apeira.dev.unbound"));
                }
            }
        }
    }

    private static void exportBack(Item back) {
        try {
            Path file = BackswagExporter.export(back, DevSession.backOffset(back));
            DevSession.message(Component.translatable("message.apeira.dev.exported", back.getDefaultInstance().getHoverName(), BackswagExporter.FILE));
        } catch (IOException e) {
            ApeiraDev.LOGGER.warn("Back cosmetic export failed", e);
            DevSession.message(Component.translatable("message.apeira.dev.export_failed", e.getMessage()));
        }
    }

    /** Tunings belong to the server that sent them. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DevSession.clear();
        FurnitureTunings.clear();
        BackOffsets.clear();
    }
}
