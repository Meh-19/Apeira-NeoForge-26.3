package com.apeira.main.ruby;

import java.util.Locale;

import com.apeira.main.realm.RealmData;
import com.apeira.main.registry.MainRubies;
import com.apeira.main.registry.MainSounds;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * A player's ruby balance, Origin Realms' currency: the {@code apeira:rubies} attachment ({@link MainRubies#RUBIES}),
 * saved with the player, kept on death and synced to them alone. Every change goes through here, which never lets it
 * fall below 0 and keeps a copy in {@link RealmData}'s known players for profiles viewed while they are offline.
 */
public final class Rubies {
    /** The most rubies a player can hold. */
    public static final long MAX = 1_000_000_000_000L;

    private Rubies() {}

    public static long get(Player player) {
        return player.getData(MainRubies.RUBIES);
    }

    /** Sets {@code player}'s balance to {@code rubies}, clamped to 0 and {@link #MAX}. */
    public static void set(ServerPlayer player, long rubies) {
        long clamped = Math.clamp(rubies, 0L, MAX);
        player.setData(MainRubies.RUBIES, clamped);
        RealmData.get(player.level().getServer()).rubiesChanged(player.getUUID(), clamped);
    }

    /** Adds {@code rubies} (at least 1) to {@code player}'s balance and plays the ruby gain sound to them alone. */
    public static void give(ServerPlayer player, long rubies) {
        set(player, get(player) + rubies);
        player.connection.send(new ClientboundSoundPacket(MainSounds.RUBY_GAIN, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()));
    }

    /** Takes {@code rubies} from {@code player} if they have that many; returns whether they did. */
    public static boolean take(ServerPlayer player, long rubies) {
        long balance = get(player);
        if (rubies > balance) {
            return false;
        }
        set(player, balance - rubies);
        return true;
    }

    /** {@code rubies} with thousands separated by commas, as Origin Realms writes them ("30,400"). */
    public static String format(long rubies) {
        return String.format(Locale.ROOT, "%,d", rubies);
    }
}
