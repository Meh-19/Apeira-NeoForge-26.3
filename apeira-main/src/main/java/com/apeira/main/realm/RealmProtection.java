package com.apeira.main.realm;

import java.util.Optional;
import java.util.UUID;

import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.crate.CrateBlock;
import com.apeira.main.navigator.OpenProfilePayload;
import com.apeira.main.realm.balloon.StarterBalloon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Enforces realm roles and visiting rules on the server (registered in {@code Apeira}'s constructor). The permission
 * matrix: the owner and Trusted players may do everything; Residents break and place blocks and use doors, buttons and
 * seats, but cannot open anything with a menu or inventory ({@link #isGuardedUse}: containers, chests, barrels, the
 * ruby deposit, crafting stations, crates) or break containers and furniture, and only touch players and hostile mobs;
 * visitors only look around: no block, item or entity use (except eating and drinking and opening a player's profile),
 * no attacks, no punching blocks and no item pickup, and {@link RealmVisitors} keeps mobs and pushes off them.
 * Players who may not visit a realm cannot travel into it and are sent out once a second if the rules change while they are inside.
 * Private realm chat keeps a realm's messages among the players in it. Operators bypass every check, except that no
 * one breaks or waterlogs the starter balloon and the barriers of its floor ({@link StarterBalloon#isFootprint}).
 */
public final class RealmProtection {
    private static final int EVICT_INTERVAL = 20;
    private static int ticks;

    private RealmProtection() {}

    /** The role {@code player} acts with at their position, or empty where no realm rules apply. */
    private static Optional<RealmRole> role(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || RealmService.bypasses(serverPlayer)) {
            return Optional.empty();
        }
        return RealmService.roleIn(serverPlayer, serverPlayer.level());
    }

    /** Containers and furniture need the Trusted role to open or break. */
    private static boolean isGuarded(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof ApeiraFurnitureBlock || level.getBlockEntity(pos) instanceof Container;
    }

    /** Blocks Residents may not open: anything with an inventory or a menu, and crates. */
    private static boolean isGuardedUse(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof Container || state.getMenuProvider(level, pos) != null
                || state.getBlock() instanceof CrateBlock;
    }

    private static void deny(Player player, String key) {
        player.sendOverlayMessage(Component.translatable("message.apeira.realm.denied." + key).withStyle(ChatFormatting.RED));
    }

    /** Where the starter balloon of the realm {@code level} stands, if it is a realm with one. */
    public static Optional<BlockPos> balloon(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return RealmService.ownerOf(serverLevel).flatMap(owner -> RealmData.get(serverLevel.getServer()).access(owner).balloon());
    }

    /** Whether {@code pos} is the balloon or one of its floor's barriers, which no one may break or change. */
    private static boolean isBalloonFootprint(Level level, BlockPos pos) {
        return balloon(level).filter(balloon -> StarterBalloon.isFootprint(balloon, pos)).isPresent();
    }

    public static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof Level level && isBalloonFootprint(level, event.getPos())) {
            // Operators and creative players too, silently: only /realm setspawn moves the balloon and its barriers.
            event.setCanceled(true);
            return;
        }
        role(event.getPlayer()).ifPresent(role -> {
            if (!role.canBuild()) {
                event.setCanceled(true);
                deny(event.getPlayer(), "build");
            } else if (!role.canUseContainers() && event.getLevel() instanceof Level level && isGuarded(level, event.getPos(), event.getState())) {
                event.setCanceled(true);
                deny(event.getPlayer(), "trusted");
            }
        });
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && role(player).filter(role -> !role.canBuild()).isPresent()) {
            event.setCanceled(true);
            deny(player, "build");
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof BucketItem && isBalloonFootprint(event.getLevel(), event.getPos())) {
            // Barriers take water: buckets would waterlog the balloon's floor.
            event.setUseItem(TriState.FALSE);
        }
        role(event.getEntity()).ifPresent(role -> {
            if (!role.canBuild()) {
                // Visitors touch nothing: no doors, buttons, seats or menus, and items never act on blocks.
                event.setUseItem(TriState.FALSE);
                event.setUseBlock(TriState.FALSE);
                deny(event.getEntity(), "visitor");
            } else if (!role.canUseContainers() && isGuardedUse(event.getLevel(), event.getPos(), event.getLevel().getBlockState(event.getPos()))) {
                event.setUseBlock(TriState.FALSE);
                deny(event.getEntity(), "trusted");
            }
        });
    }

    /** Visitors don't punch blocks either (note blocks, crates, dragon eggs); breaking is {@link #onBreak}'s. */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (role(event.getEntity()).filter(role -> !role.canBuild()).isPresent()) {
            event.setCanceled(true);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Visitors may still eat and drink; every other item use (buckets, throwing, shooting, fishing) acts on the world.
        if (!event.getItemStack().has(DataComponents.CONSUMABLE) && role(event.getEntity()).filter(role -> !role.canBuild()).isPresent()) {
            event.setCanceled(true);
            deny(event.getEntity(), "visitor");
        }
    }

    /** Whether {@code player} may right-click {@code target}: players and dummies (profiles) always, others by role. */
    private static boolean deniesEntityUse(Player player, Entity target) {
        if (OpenProfilePayload.profileOf(target) != null) {
            return false;
        }
        return role(player).map(role -> {
            if (!role.canTouchEntities()) {
                deny(player, role.canBuild() ? "trusted" : "visitor");
                return true;
            }
            return false;
        }).orElse(false);
    }

    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (deniesEntityUse(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    /** Visitors attack nothing; Residents only players and hostile mobs. */
    public static void onAttack(AttackEntityEvent event) {
        role(event.getEntity()).ifPresent(role -> {
            if (!role.canBuild()) {
                event.setCanceled(true);
                deny(event.getEntity(), "visitor");
            } else if (!role.canTouchEntities() && !(event.getTarget() instanceof Enemy) && !(event.getTarget() instanceof Player)) {
                event.setCanceled(true);
                deny(event.getEntity(), "trusted");
            }
        });
    }

    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (role(event.getPlayer()).filter(role -> !role.canBuild()).isPresent()) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /** Every way into a realm (commands, portals, teleports) passes here. */
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel target = server.getLevel(event.getDimension());
        if (target == null) {
            return;
        }
        RealmService.ownerOf(target).ifPresent(owner -> {
            if (!RealmService.canVisit(server, owner, player)) {
                event.setCanceled(true);
                RealmService.fail(player, "realm.cannot_visit", RealmService.data(server).nameOf(owner));
            }
        });
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticks % EVICT_INTERVAL == 0) {
            RealmService.evictUnwelcome(event.getServer());
        }
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RealmService.data(player.level().getServer()).seen(player.getUUID(), player.getName().getString());
            RealmService.remindInvites(player);
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RealmService.data(player.level().getServer()).seen(player.getUUID(), player.getName().getString());
        }
    }

    /** In a realm with private chat, a message reaches only the players in that realm. */
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        Optional<UUID> owner = RealmService.ownerOf(sender.level());
        if (owner.isEmpty() || !RealmService.data(sender.level().getServer()).access(owner.get()).privateChat()) {
            return;
        }
        event.setCanceled(true);
        Component line = Component.translatable("message.apeira.realm.private_chat", sender.getDisplayName(), event.getMessage());
        for (ServerPlayer player : sender.level().players()) {
            player.sendSystemMessage(line);
        }
    }
}
