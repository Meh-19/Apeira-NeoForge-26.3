package com.apeira.uicapture.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.uicapture.UiCapture;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Records the textures, sprites, fills and items {@link GuiGraphics} draws during a capture. Every texture blit overload ends in
 * the two {@code blit} methods hooked here; sprite draws by id are recorded once, not again for the atlas sprite or
 * nested sprite call they turn into.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Shadow
    public abstract Matrix3x2fStack pose();

    @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V", at = @At("HEAD"))
    private void apeira$recordBlit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height,
            int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color, CallbackInfo ci) {
        if (UiCapture.recording()) {
            UiCapture.blit(pose(), texture, x, y, width, height, u, v, srcWidth, srcHeight, textureWidth, textureHeight, color);
        }
    }

    @Inject(method = "blit(Lnet/minecraft/resources/Identifier;IIIIFFFF)V", at = @At("HEAD"))
    private void apeira$recordUvBlit(Identifier texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, CallbackInfo ci) {
        if (UiCapture.recording()) {
            UiCapture.uvBlit(pose(), texture, x0, y0, x1, y1, u0, u1, v0, v1);
        }
    }

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("HEAD"))
    private void apeira$recordSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (UiCapture.enterSprite()) {
            UiCapture.sprite(pose(), sprite, x, y, width, height, null, color);
        }
    }

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("RETURN"))
    private void apeira$leaveSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, CallbackInfo ci) {
        UiCapture.leaveSprite();
    }

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V", at = @At("HEAD"))
    private void apeira$recordSpriteRegion(RenderPipeline pipeline, Identifier sprite, int spriteWidth, int spriteHeight, int textureX, int textureY,
            int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (UiCapture.enterSprite()) {
            UiCapture.sprite(pose(), sprite, x, y, width, height, new int[] {spriteWidth, spriteHeight, textureX, textureY}, color);
        }
    }

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V", at = @At("RETURN"))
    private void apeira$leaveSpriteRegion(RenderPipeline pipeline, Identifier sprite, int spriteWidth, int spriteHeight, int textureX, int textureY,
            int x, int y, int width, int height, int color, CallbackInfo ci) {
        UiCapture.leaveSprite();
    }

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V", at = @At("HEAD"))
    private void apeira$recordAtlasSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (UiCapture.recording() && !UiCapture.inSprite()) {
            UiCapture.atlasSprite(pose(), sprite, x, y, width, height, color);
        }
    }

    @Inject(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V", at = @At("HEAD"))
    private void apeira$recordFill(RenderPipeline pipeline, int x0, int y0, int x1, int y1, int color, CallbackInfo ci) {
        if (UiCapture.recording()) {
            UiCapture.fill(pose(), x0, y0, x1, y1, color);
        }
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void apeira$recordItem(@Nullable LivingEntity owner, @Nullable Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (UiCapture.recording() && !stack.isEmpty()) {
            UiCapture.item(pose(), stack, x, y);
        }
    }
}
