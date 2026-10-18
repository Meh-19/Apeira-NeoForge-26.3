package com.apeira.decor.crate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.decor.network.CrateOpenPayload;
import com.apeira.decor.network.CrateUnlockPayload;
import com.apeira.decor.registry.DecorBlockEntities;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A crate's opening, ported from CosmoLib's {@code CrateEntity}. All decisions are made on the server; clients get the
 * state, the rerolls left and the rolled items with the block entity update packet, and play the matching GeckoLib
 * animation ({@link CrateState}).
 *
 * <ol>
 * <li>Right-clicking a closed crate rolls three items ({@link CrateLoot}) for the player, who becomes its opener. Their
 * camera turns to the crate ({@link CrateOpenPayload}) and they take no damage until the opening ends.</li>
 * <li>Once the items are out, the opener crouches to collect them or punches the crate to reroll
 * ({@link CrateEvents}), up to {@link CrateBlock#rerolls()} times. After the last reroll the items are collected
 * automatically two seconds later.</li>
 * <li>Collecting gives the items (dropping what does not fit), updates the opener's {@link CrateLuck} and makes the
 * crate vanish; the block is then removed.</li>
 * </ol>
 * If the opener logs out, changes dimension or moves more than 8 blocks away, the items drop on the crate and it
 * vanishes. The state is saved with the chunk, so a crate opened when the server stopped carries on.
 */
public class CrateBlockEntity extends BlockEntity implements GeoBlockEntity {
    /** How far, in blocks, the opener may move from the crate before the opening is abandoned. */
    public static final double MAX_OPENER_DISTANCE = 8.0;
    /** Ticks between the end of the last reroll and the automatic collection. */
    public static final int AUTO_COLLECT_DELAY = 40;

    /** Players opening a crate anywhere, who take no damage ({@link CrateEvents}). Server side. */
    private static final Set<UUID> ACTIVE_OPENERS = Collections.synchronizedSet(new HashSet<>());

    private static final String STATE_TAG = "state";
    private static final String TICKS_TAG = "ticks";
    private static final String OPENER_TAG = "opener";
    private static final String REROLLS_TAG = "rerolls";
    private static final String ITEMS_TAG = "items";
    private static final String AUTO_COLLECT_TAG = "auto_collect";

    private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation open;
    private final RawAnimation openLoop;
    private final RawAnimation reroll;
    private final RawAnimation vanish;

    private CrateState state = CrateState.CLOSED;
    /** Ticks spent in the current state. */
    private int ticks;
    private @Nullable UUID opener;
    private int rerollsLeft;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CrateLoot.ITEMS_PER_ROLL, ItemStack.EMPTY);
    /** Ticks until the items are collected automatically, or -1. */
    private int autoCollect = -1;

    public CrateBlockEntity(BlockPos pos, BlockState blockState) {
        super(DecorBlockEntities.CRATE.get(), pos, blockState);
        // The crates' animation files name their clips after the crate: animation.<crate>.open and so on.
        String prefix = "animation." + crateId(blockState).getPath() + ".";
        open = RawAnimation.begin().thenPlay(prefix + "open");
        openLoop = RawAnimation.begin().thenLoop(prefix + "open_loop");
        reroll = RawAnimation.begin().thenPlay(prefix + "reroll");
        vanish = RawAnimation.begin().thenPlay(prefix + "vanish");
    }

    /** The crate's id: its block's, which also names its loot, model, texture and animations. */
    public static Identifier crateId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    public Identifier crateId() {
        return crateId(getBlockState());
    }

    /** Whether the player with this UUID is opening a crate and must take no damage. */
    public static boolean isOpening(UUID player) {
        return ACTIVE_OPENERS.contains(player);
    }

    public CrateState state() {
        return state;
    }

    public int rerollsLeft() {
        return rerollsLeft;
    }

    /** The three rolled items, empty when nothing is rolled. */
    public List<ItemStack> items() {
        return Collections.unmodifiableList(items);
    }

    public boolean isOpener(UUID player) {
        return player.equals(opener);
    }

    /** Opens a closed crate for {@code player}. Fails if the crate is not closed or has no loot. */
    public boolean open(ServerPlayer player) {
        CrateLoot loot = CrateLootManager.get(crateId());
        if (state != CrateState.CLOSED || loot.isEmpty()) {
            return false;
        }
        opener = player.getUUID();
        ACTIVE_OPENERS.add(opener);
        rerollsLeft = getBlockState().getBlock() instanceof CrateBlock crate ? crate.rerolls() : CrateBlock.DEFAULT_REROLLS;
        setItems(loot.roll(player.getRandom(), CrateLuck.get(player)));
        setState(CrateState.OPENING);
        ApeiraPackets.sendToPlayer(player, new CrateOpenPayload(worldPosition));
        return true;
    }

    /** Spends a reroll for the opener while the items are out. Returns whether the crate rerolled. */
    public boolean reroll(ServerPlayer player) {
        if (state != CrateState.LOOP || !isOpener(player.getUUID()) || rerollsLeft <= 0) {
            return false;
        }
        rerollsLeft--;
        setItems(CrateLootManager.get(crateId()).roll(player.getRandom(), CrateLuck.get(player)));
        setState(CrateState.REROLLING);
        return true;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ticks++;
        if (state.ticks() > 0 && ticks == state.ticks()) {
            switch (state) {
                case OPENING -> setState(CrateState.LOOP);
                case REROLLING -> {
                    if (rerollsLeft > 0) {
                        setState(CrateState.LOOP);
                    } else {
                        autoCollect = AUTO_COLLECT_DELAY;
                    }
                }
                case VANISHING -> {
                    serverLevel.removeBlock(worldPosition, false);
                    return;
                }
                default -> {}
            }
        }
        if (!state.inSession()) {
            return;
        }
        ServerPlayer player = opener == null ? null : serverLevel.getServer().getPlayerList().getPlayer(opener);
        if (player == null || player.level() != serverLevel
                || player.distanceToSqr(Vec3.atCenterOf(worldPosition)) > MAX_OPENER_DISTANCE * MAX_OPENER_DISTANCE) {
            abandon(player);
            return;
        }
        if (opener != null) {
            // Kept in sync after a server restart, when the set starts empty.
            ACTIVE_OPENERS.add(opener);
        }
        // CosmoLib's protection: resistance V blocks all damage, and CrateEvents cancels what resistance does not.
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4, false, false));
        if (autoCollect > 0 && --autoCollect == 0) {
            collect(player);
        } else if (state == CrateState.LOOP && player.isShiftKeyDown()) {
            collect(player);
        }
    }

    /** Gives the items to the opener, updates their luck and makes the crate vanish. */
    private void collect(ServerPlayer player) {
        List<ItemStack> collected = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                collected.add(stack.copy());
            }
        }
        for (ItemStack stack : collected) {
            // Adds what fits and drops the rest at the player.
            player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
        }
        CrateLuck.onCollected(player, CrateLootManager.get(crateId()), collected);
        endSession(player);
    }

    /** The opener left: the items drop on the crate, which vanishes. */
    private void abandon(@Nullable ServerPlayer player) {
        dropItems();
        endSession(player);
    }

    private void endSession(@Nullable ServerPlayer player) {
        if (opener != null) {
            ACTIVE_OPENERS.remove(opener);
        }
        opener = null;
        autoCollect = -1;
        setItems(List.of());
        if (player != null) {
            ApeiraPackets.sendToPlayer(player, CrateUnlockPayload.INSTANCE);
        }
        setState(CrateState.VANISHING);
    }

    private void dropItems() {
        if (level != null) {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                            stack.copy());
                }
            }
        }
    }

    /** Removed during an opening (by a command, for example): the items drop and the opener is freed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (state.inSession() && level instanceof ServerLevel serverLevel) {
            dropItems();
            ServerPlayer player = opener == null ? null : serverLevel.getServer().getPlayerList().getPlayer(opener);
            if (opener != null) {
                ACTIVE_OPENERS.remove(opener);
            }
            if (player != null) {
                ApeiraPackets.sendToPlayer(player, CrateUnlockPayload.INSTANCE);
            }
        }
    }

    private void setItems(List<ItemStack> stacks) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
        }
    }

    private void setState(CrateState state) {
        this.state = state;
        ticks = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state = input.read(STATE_TAG, CrateState.CODEC).orElse(CrateState.CLOSED);
        ticks = input.getIntOr(TICKS_TAG, 0);
        opener = input.read(OPENER_TAG, UUIDUtil.CODEC).orElse(null);
        rerollsLeft = input.getIntOr(REROLLS_TAG, 0);
        setItems(input.read(ITEMS_TAG, ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        autoCollect = input.getIntOr(AUTO_COLLECT_TAG, -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (state == CrateState.CLOSED) {
            return;
        }
        output.store(STATE_TAG, CrateState.CODEC, state);
        output.putInt(TICKS_TAG, ticks);
        output.storeNullable(OPENER_TAG, UUIDUtil.CODEC, opener);
        output.putInt(REROLLS_TAG, rerollsLeft);
        output.store(ITEMS_TAG, ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.putInt(AUTO_COLLECT_TAG, autoCollect);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CrateBlockEntity>("crate", 0, test -> {
            RawAnimation animation = switch (state) {
                case CLOSED -> null;
                case OPENING -> open;
                case LOOP -> openLoop;
                case REROLLING -> reroll;
                case VANISHING -> vanish;
            };
            return animation == null ? PlayState.STOP : test.setAndContinue(animation);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animations;
    }
}
