package com.apeira.decor.entity;

import java.util.List;

import com.apeira.decor.block.ApeiraChairBlock;
import com.apeira.decor.registry.DecorEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible entity a player rides while sitting on an {@link ApeiraChairBlock}.
 *
 * <p>A seat sits at the centre of its chair, at the height of the chair's seat, so its block position is the chair's.
 * It has no size, physics, sound or save data of its own, and its tick only checks that it still has a passenger and
 * still stands in a chair: it removes itself as soon as either is no longer true. It is still saved with the chunk
 * (riding requires a savable vehicle), so a player who logs out while sitting is seated again when they log back in.
 *
 * <p>Seats render nothing; the client registers a no-op renderer for {@link DecorEntities#SEAT}, which every entity
 * type needs.
 */
public class ApeiraSeatEntity extends Entity {
    public ApeiraSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setInvisible(true);
    }

    /**
     * Seats {@code player} on the chair at {@code pos}, reusing the chair's seat when it already has one. Server side
     * only.
     *
     * @return whether the player sat down; false when someone else already sits on the chair
     */
    public static boolean sit(Level level, BlockPos pos, BlockState state, LivingEntity player) {
        if (!(state.getBlock() instanceof ApeiraChairBlock chair)) {
            return false;
        }
        List<ApeiraSeatEntity> seats = level.getEntitiesOfClass(ApeiraSeatEntity.class, new AABB(pos), Entity::isAlive);
        ApeiraSeatEntity seat = seats.isEmpty() ? null : seats.getFirst();
        if (seat != null && seat.isVehicle()) {
            return false;
        }
        if (seat == null) {
            seat = new ApeiraSeatEntity(DecorEntities.SEAT.get(), level);
            float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
            seat.snapTo(pos.getX() + 0.5, pos.getY() + chair.seatHeight(), pos.getZ() + 0.5, yaw, 0.0F);
            level.addFreshEntity(seat);
        }
        // Forced, so the dismount cooldown does not stop a player getting back up on a chair they just left.
        if (!player.startRiding(seat, true, true)) {
            if (!seat.isVehicle()) {
                seat.discard();
            }
            return false;
        }
        // Face out of the chair.
        player.setYRot(seat.getYRot());
        player.setYHeadRot(seat.getYRot());
        player.setYBodyRot(seat.getYRot());
        return true;
    }

    @Override
    public void tick() {
        // No physics, fire or fluid checks: a seat only has to notice when it is no longer needed.
        if (!level().isClientSide() && (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof ApeiraChairBlock))) {
            discard();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && !isRemoved() && !isVehicle()) {
            discard();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !isVehicle();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Stand up in front of the chair, else beside or behind it, else on top of it.
        BlockPos chair = blockPosition();
        Direction front = getDirection();
        for (Direction side : new Direction[] { front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite() }) {
            Vec3 location = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), chair.relative(side), true);
            if (location != null) {
                return location;
            }
        }
        return Vec3.atBottomCenterOf(chair.above());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
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
}
