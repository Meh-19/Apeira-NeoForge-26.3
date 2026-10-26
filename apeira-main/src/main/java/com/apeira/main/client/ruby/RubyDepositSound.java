package com.apeira.main.client.ruby;

import java.util.Map;
import java.util.WeakHashMap;

import com.apeira.main.registry.MainSounds;
import com.apeira.main.ruby.RubyDepositBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The ruby deposit's ambient sound ({@code entity.atm_ruby.ambient}, Origin Realms' {@code atm}: a 2.54 second clip
 * at 0.7 volume and pitch 1, heard within 6 blocks with linear falloff, as {@code sounds.json} gives it), played once
 * at the block's centre every {@link #INTERVAL} ticks (4 seconds) as the live server repeats it, not looped.
 * {@link #tick} runs from the block entity's client tick; a playing clip stops when its block goes.
 */
public final class RubyDepositSound extends AbstractTickableSoundInstance {
    /** Ticks between the starts of two plays: 4 seconds at 20 ticks a second. */
    public static final int INTERVAL = 80;
    private static final Map<RubyDepositBlockEntity, Long> LAST_PLAYED = new WeakHashMap<>();

    private final RubyDepositBlockEntity deposit;

    private RubyDepositSound(RubyDepositBlockEntity deposit) {
        super(MainSounds.ATM_RUBY_AMBIENT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.deposit = deposit;
        this.looping = false;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        this.x = deposit.getBlockPos().getX() + 0.5;
        this.y = deposit.getBlockPos().getY() + 0.5;
        this.z = deposit.getBlockPos().getZ() + 0.5;
    }

    public static void tick(RubyDepositBlockEntity deposit) {
        long time = deposit.getLevel().getGameTime();
        Long last = LAST_PLAYED.get(deposit);
        if (last == null || time - last >= INTERVAL || time < last) {
            LAST_PLAYED.put(deposit, time);
            Minecraft.getInstance().getSoundManager().play(new RubyDepositSound(deposit));
        }
    }

    @Override
    public void tick() {
        if (deposit.isRemoved() || deposit.getLevel() != Minecraft.getInstance().level) {
            stop();
        }
    }
}
