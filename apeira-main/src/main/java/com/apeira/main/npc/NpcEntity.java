package com.apeira.main.npc;

import java.util.Set;
import java.util.function.Predicate;

import com.apeira.main.realm.RealmService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A realm NPC, such as Conductor Carlo ({@link com.apeira.main.registry.MainNpcs}): a plain entity, not a mob, with no
 * AI, physics or health. It stands where it was placed, facing its way, plays its Bedrock model's animation
 * ({@code client/npc/BedrockNpcRenderer}) and cannot be hurt, pushed or moved by pistons. Shift-right-clicking it opens
 * its settings ({@link NpcService}) for whoever may edit it ({@link #canEdit}): its visibility and its anchor, the spot it
 * stands on ({@link #anchor}).
 *
 * <p>An invisible NPC ({@link #isShown()} false) is not drawn and cannot be aimed at for players who may not edit it;
 * those who may see it as a faint ghost, so they can make it visible again.
 */
public class NpcEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_SHOWN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_SHOWN = "shown";

    /**
     * Whether the client's own player may edit {@code npc}, which decides whether a hidden NPC can be aimed at there. Set
     * by the client ({@code client/npc/NpcClient}); on a dedicated server nothing asks.
     */
    public static Predicate<NpcEntity> clientCanEdit = npc -> false;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_SHOWN, true);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setShown(input.getBooleanOr(TAG_SHOWN, true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean(TAG_SHOWN, isShown());
    }

    /** Whether the NPC is visible to everyone (the default), or hidden from all but those who may edit it. */
    public boolean isShown() {
        return entityData.get(DATA_SHOWN);
    }

    public void setShown(boolean shown) {
        entityData.set(DATA_SHOWN, shown);
    }

    /**
     * Whether {@code player} may change this NPC: the owner of the realm it stands in, or an operator in creative or
     * spectator mode ({@link RealmService#bypassesRoles}), who may also edit NPCs outside realms.
     */
    public boolean canEdit(ServerPlayer player) {
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        return RealmService.bypassesRoles(player) || RealmService.ownerOf(level).filter(player.getUUID()::equals).isPresent();
    }

    /** Turns the NPC so its front (Blockbench's north) faces {@code target}. */
    public void face(Vec3 target) {
        double dx = target.x - getX();
        double dz = target.z - getZ();
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(yaw);
            this.yRotO = yaw;
        }
    }

    /**
     * Moves the NPC to stand on {@code anchor} (the bottom centre of that block) facing {@code facing}, the spot picked
     * with the anchor placement tool ({@link NpcAnchorPayload}). The caller has checked {@link #canStandAt}.
     */
    public void anchor(ServerLevel level, BlockPos anchor, Vec3 facing) {
        Vec3 position = Vec3.atBottomCenterOf(anchor);
        float yaw = (float) (Mth.atan2(facing.z - position.z, facing.x - position.x) * Mth.RAD_TO_DEG) - 90.0F;
        teleportTo(level, position.x, position.y, position.z, Set.of(), yaw, 0.0F, false);
        setYRot(yaw);
        this.yRotO = yaw;
    }

    /** The blocks the NPC takes when it stands on {@code anchor}: its collision box, whole blocks, at least one. */
    public static AABB footprint(EntityType<?> type, BlockPos anchor) {
        int radius = Math.max(0, Mth.ceil(type.getWidth() / 2.0F - 0.5F));
        int height = Math.max(1, Mth.ceil(type.getHeight()));
        return new AABB(anchor.getX() - radius, anchor.getY(), anchor.getZ() - radius, anchor.getX() + radius + 1, anchor.getY() + height,
                anchor.getZ() + radius + 1);
    }

    /**
     * Whether an NPC of {@code type} may stand on {@code anchor}: inside the world border and build height, on a block
     * with a collision box, and with nothing solid or liquid in {@link #footprint}. The client's anchor cursor and the
     * server ask the same.
     */
    public static boolean canStandAt(Level level, EntityType<?> type, BlockPos anchor) {
        AABB footprint = footprint(type, anchor);
        if (!level.getWorldBorder().isWithinBounds(footprint) || !level.isInWorldBounds(anchor)
                || !level.isInWorldBounds(BlockPos.containing(footprint.maxX - 1, footprint.maxY - 1, footprint.maxZ - 1))) {
            return false;
        }
        BlockPos below = anchor.below();
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(footprint.minX, footprint.minY, footprint.minZ),
                BlockPos.containing(footprint.maxX - 1, footprint.maxY - 1, footprint.maxZ - 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return isShown() || !level().isClientSide() || clientCanEdit.test(this);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    /** Hits do nothing. */
    @Override
    public boolean skipAttackInteraction(Entity source) {
        return true;
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
