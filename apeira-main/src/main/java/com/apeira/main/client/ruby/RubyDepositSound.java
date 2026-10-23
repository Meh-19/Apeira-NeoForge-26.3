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
 * The ruby deposit's ambient loop ({@code entity.atm_ruby.ambient}, Origin Realms' {@code atm} at 0.7 volume, heard
 * within 6 blocks), played at the block for as long as it stands. {@link #tick} runs from the block entity's client
 * tick and starts the loop again whenever it is not playing (after the sound engine dropped it, or a reload).
 */
public final class RubyDepositSound extends AbstractTickableSoundInstance {
    private static final Map<RubyDepositBlockEntity, RubyDepositSound> PLAYING = new WeakHashMap<>();

    private final RubyDepositBlockEntity deposit;

    private RubyDepositSound(RubyDepositBlockEntity deposit) {
        super(MainSounds.ATM_RUBY_AMBIENT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.deposit = deposit;
        this.looping = true;
        this.delay = 0;
        this.x = deposit.getBlockPos().getX() + 0.5;
        this.y = deposit.getBlockPos().getY() + 0.5;
        this.z = deposit.getBlockPos().getZ() + 0.5;
    }

    public static void tick(RubyDepositBlockEntity deposit) {
        RubyDepositSound sound = PLAYING.get(deposit);
        if (sound == null || !Minecraft.getInstance().getSoundManager().isActive(sound)) {
            sound = new RubyDepositSound(deposit);
            PLAYING.put(deposit, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    @Override
    public void tick() {
        if (deposit.isRemoved() || deposit.getLevel() != Minecraft.getInstance().level) {
            stop();
        }
    }
}
