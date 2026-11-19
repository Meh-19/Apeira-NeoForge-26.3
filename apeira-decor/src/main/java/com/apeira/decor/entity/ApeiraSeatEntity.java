package com.apeira.decor.entity;

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.SeatBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible entity a player rides while sitting on a {@link SeatBlock}.
 *
 * <p>A seat sits at the centre of its chair, at the height of the chair's seat, so its block position is the chair's.
 * It has no size, physics or sound, saves only its chair's position, and its tick only checks that it still has a passenger and
 * still belongs to a chair: it removes itself as soon as either is no longer true. It is still saved with the chunk
 * (riding requires a savable vehicle), so a player who logs out while sitting is seated again when they log back in.
 *
 * <p>Seats render nothing; the client registers a no-op renderer for {@link DecorEntities#SEAT}, which every entity
 * type needs.
 */
public class ApeiraSeatEntity extends Entity {
    /** The chair this seat belongs to, saved; {@code null} for seats from before chairs had several. */
    private @Nullable BlockPos chair;

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
        if (!(state.getBlock() instanceof SeatBlock chair)) {
            return false;
        }
        // The free seat nearest the player, for chairs with several (the bathtub).
        List<Vec3> spots = chair.seats(state).stream().map(offset -> Vec3.atLowerCornerOf(pos).add(offset))
                .sorted(Comparator.comparingDouble(spot -> spot.distanceToSqr(player.position()))).toList();
        for (Vec3 spot : spots) {
            List<ApeiraSeatEntity> seats = level.getEntitiesOfClass(ApeiraSeatEntity.class, new AABB(spot, spot).inflate(0.05), Entity::isAlive);
            ApeiraSeatEntity seat = seats.isEmpty() ? null : seats.getFirst();
            if (seat != null && seat.isVehicle()) {
                continue;
            }
            if (seat == null) {
                seat = new ApeiraSeatEntity(DecorEntities.SEAT.get(), level);
                seat.chair = pos.immutable();
                float yaw = chair.seatYaw(state, player);
                seat.snapTo(spot.x, spot.y, spot.z, yaw, 0.0F);
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
        return false;
    }

    /** The chair's block: the seat's own block, unless the chair has seats off its block (the bathtub's). */
    private BlockPos chair() {
        return chair != null ? chair : blockPosition();
    }

    @Override
    public void tick() {
        // No physics, fire or fluid checks: a seat only has to notice when it is no longer needed.
        if (!level().isClientSide() && (!isVehicle() || !(level().getBlockState(chair()).getBlock() instanceof SeatBlock))) {
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
    protected void readAdditionalSaveData(ValueInput input) {
        chair = input.read("chair", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.storeNullable("chair", BlockPos.CODEC, chair);
    }

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
