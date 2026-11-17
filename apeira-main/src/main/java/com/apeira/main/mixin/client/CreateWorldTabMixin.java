package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.registry.MainGameRules;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Adds the "Realm Presets" switch to the Create World screen's World tab, under the structure and bonus chest
 * switches: it sets the new world's {@link MainGameRules#REALM_PRESETS} game rule, so a superflat or debug world can be
 * made without the realm onboarding taking over. The value lives in the screen's game rules, so it follows the Game
 * Rules screen both ways and is saved with the world.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class CreateWorldTabMixin {
    private static final Component LABEL = Component.translatable("gamerule.apeira.realm_presets");
    private static final Component INFO = Component.translatable("gamerule.apeira.realm_presets.description");

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apeira$addRealmPresetsSwitch(CreateWorldScreen screen, CallbackInfo ci, @Local GridLayout.RowHelper rows) {
        WorldCreationUiState state = screen.getUiState();
        GameRule<Boolean> rule = MainGameRules.REALM_PRESETS.get();
        CycleButton<Boolean> button = CycleButton.onOffBuilder(state.getGameRules().get(rule))
                .withTooltip(value -> Tooltip.create(INFO))
                .create(0, 0, 308, 20, LABEL, (cycle, value) -> {
                    GameRules rules = state.getGameRules().copy(state.getSettings().dataConfiguration().enabledFeatures());
                    rules.set(rule, value, null);
                    state.setGameRules(rules);
                });
        // Follows edits made in the Game Rules screen.
        state.addListener(changed -> button.setValue(changed.getGameRules().get(rule)));
        rows.addChild(button, 2, rows.newCellSettings().alignHorizontallyCenter());
    }
}
