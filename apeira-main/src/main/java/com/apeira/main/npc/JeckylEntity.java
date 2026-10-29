package com.apeira.main.npc;

import org.jspecify.annotations.Nullable;

import com.apeira.main.realm.RealmService;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.SteppedInterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Jeckyl, Origin Realms' farming accessories trader. He is placed somewhere on the ground near a realm's new starter
 * balloon ({@link NpcService#spawnJeckyl}) and wanders round that spot ({@link #home}, within {@link #WANDER_RADIUS}
 * blocks) until someone first talks to him: that first talk gives his introduction line and anchors him for good
 * ({@link #isAnchored}), so he stands still from then on and only then can be moved with the anchor tool. Later talks
 * give one of his two idle lines. Left-clicking him while his dialogue is open opens his shop ({@link NpcShop#JECKYL}).
 *
 * <p>The wander is his own, as he is a plain entity and not a mob: a walk at {@link #SPEED} blocks a tick to a random
 * spot near home that he can stand on, along the ground (one block up or down a step), then a pause, then another. Clients
 * interpolate his moves as they do a mob's.
 */
public class JeckylEntity extends NpcEntity {
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED = SynchedEntityData.defineId(JeckylEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WALKING = SynchedEntityData.defineId(JeckylEntity.class, EntityDataSerializers.BOOLEAN);
    /** His legs' walk cycle ({@code animation.jeckyl.walk}), played over the idle while he walks. */
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.jeckyl.walk");
    /** Ticks the legs take to blend into and out of the walk cycle. */
    private static final int WALK_BLEND_TICKS = 5;
    static final int WANDER_RADIUS = 8;
    private static final double SPEED = 0.06;
    private static final int MIN_PAUSE = 60;
    private static final int MAX_PAUSE = 200;
    private static final String INTRO = "dialogue.apeira.jeckyl.intro";
    private static final String[] IDLE = {"dialogue.apeira.jeckyl.idle.1", "dialogue.apeira.jeckyl.idle.2"};

    private @Nullable BlockPos home;
    private @Nullable Vec3 target;
    private int pause = MIN_PAUSE;

    public JeckylEntity(EntityType<? extends NpcEntity> type, Level level, RawAnimation idle) {
        super(type, level, idle);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return SteppedInterpolationHandler.create(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ANCHORED, false);
        entityData.define(DATA_WALKING, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_ANCHORED, input.getBooleanOr("anchored", false));
        home = input.read("home", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("anchored", isAnchored());
        output.storeNullable("home", BlockPos.CODEC, home);
    }

    /** Whether he has been talked to and stands still, which also lets his editors move him. */
    public boolean isAnchored() {
        return entityData.get(DATA_ANCHORED);
    }

    /** Marks him found, as after his first talk: he stands still and gives his idle lines (a reset realm's Jeckyl). */
    public void markFound() {
        entityData.set(DATA_ANCHORED, true);
        target = null;
    }

    /** Whether he is walking (synced), which plays his legs' walk cycle. */
    public boolean isWalking() {
        return entityData.get(DATA_WALKING);
    }

    /** The spot he wanders round until anchored. */
    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    @Override
    public boolean canAnchor() {
        return isAnchored();
    }

    @Override
    public @Nullable NpcShop shop() {
        return NpcShop.JECKYL;
    }

    /** The first talk is his introduction and anchors him; after that, one of his idle lines. */
    @Override
    public String lineFor(ServerPlayer player) {
        if (!isAnchored()) {
            markFound();
            if (level() instanceof ServerLevel level) {
                RealmService.markJeckylFound(level);
            }
            return INTRO;
        }
        return IDLE[getRandom().nextInt(IDLE.length)];
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (!isAnchored()) {
                wander(level);
            }
            boolean walking = !isAnchored() && target != null;
            if (walking != isWalking()) {
                entityData.set(DATA_WALKING, walking);
            }
        }
    }

    /**
     * The idle, and a second controller for his legs: the walk cycle while {@link #isWalking}, blended in and out over
     * {@link #WALK_BLEND_TICKS} ticks as a mob's limb swing eases, and nothing (the idle's still legs) otherwise.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<NpcEntity>("legs", WALK_BLEND_TICKS,
                test -> test.animatable() instanceof JeckylEntity jeckyl && jeckyl.isWalking() ? test.setAndContinue(WALK) : PlayState.STOP));
    }

    private void wander(ServerLevel level) {
        if (home == null) {
            home = blockPosition();
        }
        if (target == null) {
            if (--pause <= 0) {
                target = pickTarget(level, getRandom());
                pause = Mth.nextInt(getRandom(), MIN_PAUSE, MAX_PAUSE);
            }
            return;
        }
        double dx = target.x - getX();
        double dz = target.z - getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.1) {
            target = null;
            return;
        }
        double step = Math.min(SPEED, distance);
        double x = getX() + dx / distance * step;
        double z = getZ() + dz / distance * step;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        BlockPos next = BlockPos.containing(x, ground, z);
        if (Math.abs(ground - getY()) > 1.0 || (!next.equals(blockPosition()) && !canStandAt(level, getType(), next))) {
            target = null;
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 15.0F));
        setPos(x, ground, z);
    }

    /** A spot near home he can stand on, on the ground there; null when the tries find none. */
    private @Nullable Vec3 pickTarget(ServerLevel level, RandomSource random) {
        for (int tries = 0; tries < 8; tries++) {
            int x = home.getX() + random.nextInt(WANDER_RADIUS * 2 + 1) - WANDER_RADIUS;
            int z = home.getZ() + random.nextInt(WANDER_RADIUS * 2 + 1) - WANDER_RADIUS;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (level.isLoaded(pos) && canStandAt(level, getType(), pos)) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return null;
    }
}
