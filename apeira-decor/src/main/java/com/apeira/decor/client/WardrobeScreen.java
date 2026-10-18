package com.apeira.decor.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.menu.WardrobeMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

/**
 * The wardrobe: hat, back and hand slots down the left and a slowly turning preview of the player wearing them.
 * Layout, texture and turning speed are CosmoLib's {@code CosmeticScreen}, which hides the vanilla labels.
 */
public class WardrobeScreen extends AbstractContainerScreen<WardrobeMenu> {
    private static final Identifier BACKGROUND = ApeiraIds.id("textures/gui/wardrobe.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 200;
    /** The preview region, relative to the panel. */
    private static final int PREVIEW_X0 = 50;
    private static final int PREVIEW_Y0 = 10;
    private static final int PREVIEW_X1 = 125;
    private static final int PREVIEW_Y1 = 109;
    private static final int PREVIEW_SCALE = 40;
    /** One full turn every eight seconds. */
    private static final float DEGREES_PER_MILLI = 360.0F / 8000.0F;

    private final long openedAt = Util.getMillis();

    public WardrobeScreen(WardrobeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        preview(graphics);
    }

    private void preview(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        EntityRenderState state = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player)
                .createRenderState(minecraft.player, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F + (Util.getMillis() - openedAt) * DEGREES_PER_MILLI % 360.0F;
            living.yRot = 0.0F;
            living.xRot = 0.0F;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0F;
        }
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
        graphics.entity(state, PREVIEW_SCALE, translation, rotation, null, leftPos + PREVIEW_X0, topPos + PREVIEW_Y0,
                leftPos + PREVIEW_X1, topPos + PREVIEW_Y1);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The texture has no room for labels, as in CosmoLib.
    }
}
