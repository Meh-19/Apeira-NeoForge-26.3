package com.apeira.main.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Xenon's Shadow Mode effect, indigo. It does nothing itself: {@link ShadowModeManager} and the shadow mixins act while a
 * player has it.
 */
public class ShadowModeEffect extends MobEffect {
    public ShadowModeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4B0082);
    }
}
