package com.apeira.main.crate;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.main.Apeira;
import com.apeira.main.network.BadgerPhasePayload;
import com.apeira.main.registry.MainBadgerCrate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One player's Badger Crate opening, ticked by {@link BadgerSessions} on the server:
 * <ol>
 * <li>{@link #PHASE_FADE_OUT}: the client fades to black ({@link #FADE_TICKS});</li>
 * <li>{@link #PHASE_LOADING}: the player's instance ({@link BadgerInstances}) is copied and loaded, the player is put
 * in it as a spectator and waits, still black, until their client reports the chunks drawn (or
 * {@link #LOAD_TIMEOUT});</li>
 * <li>{@link #PHASE_SHOW}: the animation entity is spawned at {@link #ANIMATION_ORIGIN} with the three rolled
 * rewards; the client fades in and locks its camera to the recorded track;</li>
 * <li>{@link #PHASE_SELECT}, from {@link #SELECT_AT} (ten seconds in): the reward picked with the scroll wheel is
 * collected by crouching, or after {@link #SELECT_TIMEOUT} (five minutes); one reward, no rerolls;</li>
 * <li>{@link #PHASE_CLAIM}: the ding plays and the client fades to black; the player goes back to their respawn point
 * in their old game mode, the instance is deleted and a nameless double chest opens with the reward
 * ({@link #PHASE_END}).</li>
 * </ol>
 * The game mode to go back to is kept in the player's persistent data too, so a crash mid-opening is undone on the
 * next login ({@link BadgerSessions#onLoggedIn}).
 */
final class BadgerSession {
    static final int PHASE_FADE_OUT = 0;
    static final int PHASE_LOADING = 1;
    static final int PHASE_SHOW = 2;
    static final int PHASE_SELECT = 3;
    static final int PHASE_CLAIM = 4;
    static final int PHASE_END = 5;

    static final int FADE_TICKS = 10;
    /** At least this long in the dark after arriving, so the chunks have a moment to come in. */
    static final int MIN_LOAD_TICKS = 10;
    static final int LOAD_TIMEOUT = 100;
    static final int SELECT_AT = 200;
    static final int SELECT_TIMEOUT = 5 * 60 * 20;

    /** Where the animation plays: the model's origin, at block corner 0, 63, 1 of the instance. */
    static final Vec3 ANIMATION_ORIGIN = BadgerCrateShowEntity.ORIGIN;
    /** The recorded player's feet at the animation's start ({@code badger_crate/camera.json}'s first eye, less 1.62). */
    static final Vec3 CAMERA_FEET = new Vec3(-1.35, -0.498, 2.61);
    static final String MODE_KEY = "apeira_badger_crate_return_mode";

    final UUID player;
    private final List<BadgerLoot.Reward> rewards;
    private int phase = PHASE_FADE_OUT;
    private int ticks;
    private boolean ready;
    private boolean confirmed;
    private GameType returnMode = GameType.SURVIVAL;
    private @Nullable ServerLevel instance;
    private @Nullable BadgerCrateShowEntity show;

    BadgerSession(ServerPlayer player, List<BadgerLoot.Reward> rewards) {
        this.player = player.getUUID();
        this.rewards = rewards;
        send(player, PHASE_FADE_OUT);
    }

    int phase() {
        return phase;
    }

    /** One server tick; false once the session is over. */
    boolean tick(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(this.player);
        if (player == null) {
            return false;
        }
        ticks++;
        switch (phase) {
            case PHASE_FADE_OUT -> {
                if (ticks >= FADE_TICKS) {
                    enterInstance(server, player);
                }
            }
            case PHASE_LOADING -> {
                if (ticks >= LOAD_TIMEOUT || ready && ticks >= MIN_LOAD_TICKS) {
                    startShow(player);
                }
            }
            case PHASE_SHOW -> {
                if (leftInstance(player)) {
                    return abort(server, player);
                }
                if (ticks >= SELECT_AT) {
                    if (show != null) {
                        show.setSelecting(true);
                    }
                    move(player, PHASE_SELECT);
                }
            }
            case PHASE_SELECT -> {
                if (leftInstance(player)) {
                    return abort(server, player);
                }
                if (confirmed || ticks >= SELECT_TIMEOUT) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    move(player, PHASE_CLAIM);
                }
            }
            case PHASE_CLAIM -> {
                if (ticks >= FADE_TICKS) {
                    finish(server, player, true);
                    return false;
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    void input(ServerPlayer player, int action, int value) {
        switch (action) {
            case com.apeira.main.network.BadgerInputPayload.READY -> ready = true;
            case com.apeira.main.network.BadgerInputPayload.SELECT -> {
                if (phase == PHASE_SELECT && show != null && value >= 0 && value < 3) {
                    show.setSelected(value);
                }
            }
            case com.apeira.main.network.BadgerInputPayload.CONFIRM -> {
                if (phase == PHASE_SELECT) {
                    confirmed = true;
                }
            }
            default -> {}
        }
    }

    private void enterInstance(MinecraftServer server, ServerPlayer player) {
        try {
            instance = BadgerInstances.create(server, this.player);
        } catch (IOException e) {
            Apeira.LOGGER.error("Could not make the Badger Crate instance for {}", player.getGameProfile().name(), e);
            give(player, new ItemStack(MainBadgerCrate.BADGER_CRATE.get()));
            send(player, PHASE_END);
            phase = PHASE_END;
            return;
        }
        returnMode = player.gameMode.getGameModeForPlayer();
        player.getPersistentData().putString(MODE_KEY, returnMode.getName());
        player.stopUsingItem();
        player.closeContainer();
        player.setGameMode(GameType.SPECTATOR);
        Vec3 feet = ANIMATION_ORIGIN.add(CAMERA_FEET);
        player.teleportTo(instance, feet.x, feet.y, feet.z, Set.of(), -154.7F, 4.2F, true);
        move(player, PHASE_LOADING);
    }

    private void startShow(ServerPlayer player) {
        if (instance == null) {
            phase = PHASE_END;
            return;
        }
        BadgerCrateShowEntity entity = MainBadgerCrate.SHOW.get().create(instance, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (entity != null) {
            entity.setPos(ANIMATION_ORIGIN);
            entity.setYRot(180.0F);
            entity.setRewards(rewards);
            instance.addFreshEntity(entity);
            show = entity;
        }
        move(player, PHASE_SHOW);
    }

    private boolean leftInstance(ServerPlayer player) {
        return instance == null || player.level() != instance;
    }

    private ItemStack reward() {
        int selected = show != null ? show.selected() : 0;
        return rewards.get(Math.min(selected, rewards.size() - 1)).stack().copy();
    }

    /** Sends the player back, restores their game mode and deletes the instance; with {@code popup}, shows the reward. */
    void finish(MinecraftServer server, ServerPlayer player, boolean popup) {
        ItemStack reward = reward();
        if (instance != null && player.level() == instance) {
            TeleportTransition back = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            player.teleport(back);
        }
        player.setGameMode(returnMode);
        player.getPersistentData().remove(MODE_KEY);
        if (instance != null) {
            BadgerInstances.remove(server, instance.dimension());
            instance = null;
        }
        phase = PHASE_END;
        send(player, PHASE_END);
        if (popup) {
            openPopup(player, reward);
        } else {
            give(player, reward);
        }
    }

    /** The player left the instance some other way (a command, a portal): the reward goes straight to their inventory. */
    private boolean abort(MinecraftServer server, ServerPlayer player) {
        finish(server, player, false);
        return false;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    /** A nameless double chest with the reward in its middle; what is left in it when closed goes to the player. */
    private static void openPopup(ServerPlayer player, ItemStack reward) {
        SimpleContainer chest = new SimpleContainer(54);
        chest.setItem(22, reward);
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new PopupMenu(id, inventory, chest), Component.empty()));
    }

    private void move(ServerPlayer player, int next) {
        phase = next;
        ticks = 0;
        send(player, next);
    }

    private void send(ServerPlayer player, int phase) {
        PacketDistributor.sendToPlayer(player, new BadgerPhasePayload(phase, show != null ? show.getId() : -1));
    }

    private static final class PopupMenu extends ChestMenu {
        private final Container chest;

        PopupMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Container chest) {
            super(MenuType.GENERIC_9x6, id, inventory, chest, 6);
            this.chest = chest;
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            clearContainer(player, chest);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
