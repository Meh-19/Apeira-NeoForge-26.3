package com.apeira.main.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Xenon's grizzly bear. Hostile to players (unless tamed) and roars at players and humanoid monsters within 10 blocks,
 * which makes zombies, skeletons and illagers flee from it (the {@code *FleeGrizzlyMixin}s). A honeycomb distracts it
 * for a minute: it sits and eats, stops targeting, and a cub fed one becomes passive towards its feeder. It sleeps at
 * night, fishes in nearby water (salmon is carried ashore and eaten, anything else dropped) and breeds with salmon.
 * Drops come from its loot table.
 */
public class GrizzlyBearEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.grizzly.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.grizzly.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.grizzly.attack");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlayAndHold("animation.grizzly.roar");
    private static final RawAnimation SIT_SEQUENCE = RawAnimation.begin()
            .thenPlay("animation.grizzly.idle_to_sit")
            .thenLoop("animation.grizzly.sitting");
    private static final RawAnimation SIT_EXIT = RawAnimation.begin().thenPlay("animation.grizzly.sit_to_idle");
    private static final RawAnimation SLEEP_SEQUENCE = RawAnimation.begin()
            .thenPlay("animation.grizzly.idle_to_sleep")
            .thenLoop("animation.grizzly.sleeping");
    private static final RawAnimation SLEEP_EXIT = RawAnimation.begin().thenPlay("animation.grizzly.sleep_to_idle");

    public static final int REST_NONE = 0;
    public static final int REST_SITTING = 1;
    public static final int REST_SIT_EXIT = 2;
    public static final int REST_SLEEPING = 3;
    public static final int REST_SLEEP_EXIT = 4;

    public static final int MOUTH_NONE = 0;
    public static final int MOUTH_HONEYCOMB = 1;
    public static final int MOUTH_SALMON = 2;

    public static final int FACE_ANGRY = 0;
    public static final int FACE_PASSIVE = 1;
    public static final int FACE_SLEEPING = 2;

    // Durations in ticks.
    private static final int HONEY_DURATION = 1200;
    private static final int ROAR_DURATION = 70;
    private static final int ROAR_SOUND_DELAY = 10;
    private static final int ROAR_COOLDOWN = 300;
    /** Eight seconds of sitting and eating. */
    private static final int EAT_DURATION = 160;
    private static final int EAT_SOUND_INTERVAL = 12;
    private static final int ATTACK_DURATION = 32;
    private static final int REST_DURATION_MIN = 1000;
    private static final int REST_DURATION_MAX = 1400;
    private static final int SIT_EXIT_DURATION = 24;
    private static final int SLEEP_EXIT_DURATION = 60;
    private static final int POST_EAT_REST_MIN = 300;
    private static final int POST_EAT_REST_MAX = 400;
    /** Seconds a cub grows when fed a honeycomb. */
    private static final int GROWTH_BOOST = 200;

    private static final EntityDataAccessor<Boolean> IS_HONEY_DISTRACTED = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_PASSIVE = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ROARING = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FISHING = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REST_PHASE = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOUTH_ITEM = SynchedEntityData.defineId(GrizzlyBearEntity.class,
            EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Server-side timers.
    private int honeyTimer = 0;
    private int roarTimer = 0;
    private int roarSoundDelay = -1;
    private int roarCooldown = 0;
    private int eatTimer = 0;
    private int attackTimer = 0;
    private int restTimer = 0;
    private int transitionTimer = 0;
    private int postEatRestTimer = -1;

    private @Nullable UUID ownerUuid = null;

    public GrizzlyBearEntity(EntityType<? extends GrizzlyBearEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_HONEY_DISTRACTED, false);
        builder.define(IS_PASSIVE, false);
        builder.define(IS_ROARING, false);
        builder.define(IS_EATING, false);
        builder.define(IS_ATTACKING, false);
        builder.define(IS_FISHING, false);
        builder.define(REST_PHASE, REST_NONE);
        builder.define(MOUTH_ITEM, MOUTH_NONE);
    }

    /** Xenon's attributes. The base is the animal attributes, as 26.x's tempt goal needs a tempt range (vanilla's 10). */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RoarGoal(this));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                GrizzlyBearEntity bear = GrizzlyBearEntity.this;
                if (bear.isHoneyDistracted() || bear.getRestPhase() != REST_NONE) return false;
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                if (GrizzlyBearEntity.this.isHoneyDistracted()) return false;
                return super.canContinueToUse();
            }
        });

        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, stack -> stack.is(Items.SALMON), false) {
            @Override
            public boolean canUse() {
                return GrizzlyBearEntity.this.isPassive() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new RestGoal(this));
        this.goalSelector.addGoal(7, new FishGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> !isPassive() && !isHoneyDistracted()
                        && target instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            @Override
            public boolean canUse() {
                GrizzlyBearEntity bear = GrizzlyBearEntity.this;
                if (bear.isPassive() || bear.isHoneyDistracted() || bear.getRestPhase() != REST_NONE) return false;
                return super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            if (isHoneyDistracted()) {
                honeyTimer--;
                if (honeyTimer <= 0) {
                    entityData.set(IS_HONEY_DISTRACTED, false);
                    setTarget(null);
                }
            }

            if (isRoaring()) {
                roarTimer--;
                if (roarTimer <= 0) entityData.set(IS_ROARING, false);
            }
            if (roarSoundDelay > 0) {
                roarSoundDelay--;
                if (roarSoundDelay == 0) {
                    playSound(MainSounds.GRIZZLY_BEAR_ROAR.get(), 1.5F, 0.9F);
                    roarSoundDelay = -1;
                }
            }
            if (roarCooldown > 0) roarCooldown--;

            if (isAttacking()) {
                attackTimer--;
                if (attackTimer <= 0) entityData.set(IS_ATTACKING, false);
            }

            if (isEating()) {
                eatTimer--;

                // Munches like a panda eating bamboo.
                if (eatTimer > 0 && eatTimer % EAT_SOUND_INTERVAL == 0) {
                    playSound(SoundEvents.PANDA_EAT, 0.8F, 0.85F + getRandom().nextFloat() * 0.2F);
                }

                // Done eating: the item leaves the mouth, then the bear stands up.
                if (eatTimer <= 0 && getRestPhase() == REST_SITTING) {
                    entityData.set(MOUTH_ITEM, MOUTH_NONE);
                    entityData.set(IS_EATING, false);
                    entityData.set(REST_PHASE, REST_SIT_EXIT);
                    transitionTimer = SIT_EXIT_DURATION;
                }
            }

            int phase = getRestPhase();
            if (phase == REST_SIT_EXIT || phase == REST_SLEEP_EXIT) {
                transitionTimer--;
                if (transitionTimer <= 0) {
                    entityData.set(REST_PHASE, REST_NONE);
                }
            }
            // Sitting is timed by the eat timer above.
            if (phase == REST_SLEEPING) {
                restTimer--;
                if (restTimer <= 0) {
                    entityData.set(REST_PHASE, REST_SLEEP_EXIT);
                    transitionTimer = SLEEP_EXIT_DURATION;
                }
            }

            if (postEatRestTimer > 0) {
                postEatRestTimer--;
                if (postEatRestTimer == 0) {
                    if (getRandom().nextFloat() < 0.6F && getRestPhase() == REST_NONE && !isRoaring() && getTarget() == null) {
                        startResting();
                    }
                    postEatRestTimer = -1;
                }
            }

            if (isHoneyDistracted() && getTarget() != null) setTarget(null);
            if (isPassive() && getTarget() instanceof Player targetPlayer && targetPlayer.getUUID().equals(ownerUuid)) {
                setTarget(null);
            }
        }

        // The body holds still while roaring, resting or eating; the head stays free.
        if (isRoaring() || getRestPhase() != REST_NONE || isEating()) {
            this.yBodyRot = this.yBodyRotO;
            this.setYRot(this.yRotO);
            this.setXRot(this.xRotO);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (isRoaring() || getRestPhase() != REST_NONE || isEating()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0, 1, 0));
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    /** Plays the attack animation on every hit. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            entityData.set(IS_ATTACKING, true);
            attackTimer = ATTACK_DURATION;
        }
        return hit;
    }

    /** Getting hurt wakes it up. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean result = super.hurtServer(level, source, amount);
        if (result && isAlive() && isResting()) interruptRest();
        return result;
    }

    public boolean isHoneyDistracted() {
        return entityData.get(IS_HONEY_DISTRACTED);
    }

    public boolean isPassive() {
        return entityData.get(IS_PASSIVE);
    }

    public boolean isRoaring() {
        return entityData.get(IS_ROARING);
    }

    public boolean isEating() {
        return entityData.get(IS_EATING);
    }

    public boolean isAttacking() {
        return entityData.get(IS_ATTACKING);
    }

    public boolean isFishing() {
        return entityData.get(IS_FISHING);
    }

    public int getRestPhase() {
        return entityData.get(REST_PHASE);
    }

    public int getMouthItemType() {
        return entityData.get(MOUTH_ITEM);
    }

    public boolean isResting() {
        return getRestPhase() != REST_NONE;
    }

    public boolean isSleepPhase() {
        int phase = getRestPhase();
        return phase == REST_SLEEPING || phase == REST_SLEEP_EXIT;
    }

    /** Whether it carries a fish or honeycomb in its mouth without eating it yet. */
    public boolean isCarrying() {
        return getMouthItemType() != MOUTH_NONE && !isEating();
    }

    /** Which face overlay the model shows: sleeping, passive (tamed or eating honey) or angry. */
    public int getFaceState() {
        if (isSleepPhase()) return FACE_SLEEPING;
        if (isHoneyDistracted() || isPassive()) return FACE_PASSIVE;
        return FACE_ANGRY;
    }

    public @Nullable UUID getOwnerUuid() {
        return ownerUuid;
    }

    public boolean canRoar() {
        return !isHoneyDistracted() && !isRoaring() && !isEating() && !isAttacking() && roarCooldown <= 0
                && getRestPhase() == REST_NONE && !isCarrying();
    }

    public void startRoar() {
        entityData.set(IS_ROARING, true);
        roarTimer = ROAR_DURATION;
        roarCooldown = ROAR_COOLDOWN;
        roarSoundDelay = ROAR_SOUND_DELAY;
        getNavigation().stop();
    }

    /** Sits down to eat; the sitting animation is the eating. The munching sound loops in {@link #tick}. */
    public void startEating() {
        entityData.set(IS_EATING, true);
        eatTimer = EAT_DURATION;
        entityData.set(REST_PHASE, REST_SITTING);
        getNavigation().stop();
    }

    public void setMouthItem(int itemType) {
        entityData.set(MOUTH_ITEM, itemType);
    }

    public void setFishing(boolean fishing) {
        entityData.set(IS_FISHING, fishing);
    }

    /** Lies down to sleep. Bears never sit at random: sitting is only for eating. */
    public void startResting() {
        if (getRestPhase() != REST_NONE) return;
        entityData.set(REST_PHASE, REST_SLEEPING);
        restTimer = REST_DURATION_MIN + getRandom().nextInt(REST_DURATION_MAX - REST_DURATION_MIN);
        getNavigation().stop();
    }

    public void interruptRest() {
        int phase = getRestPhase();
        if (phase == REST_SITTING) {
            entityData.set(REST_PHASE, REST_SIT_EXIT);
            transitionTimer = SIT_EXIT_DURATION;
        } else if (phase == REST_SLEEPING) {
            entityData.set(REST_PHASE, REST_SLEEP_EXIT);
            transitionTimer = SLEEP_EXIT_DURATION;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.HONEYCOMB)) {
            if (!level().isClientSide()) {
                honeyTimer = HONEY_DURATION;
                entityData.set(IS_HONEY_DISTRACTED, true);

                // Interrupt any rest before eating (startEating sets REST_SITTING, which interruptRest would undo),
                // then clear the transition.
                if (isResting()) interruptRest();
                transitionTimer = 0;
                entityData.set(REST_PHASE, REST_NONE);

                setMouthItem(MOUTH_HONEYCOMB);
                startEating();

                if (isBaby()) {
                    ownerUuid = player.getUUID();
                    entityData.set(IS_PASSIVE, true);
                    ageUp(GROWTH_BOOST, true);
                }
                postEatRestTimer = POST_EAT_REST_MIN + getRandom().nextInt(POST_EAT_REST_MAX - POST_EAT_REST_MIN);
                setTarget(null);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SALMON);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return MainEntities.GRIZZLY_BEAR.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsPassive", isPassive());
        output.putInt("HoneyTimer", honeyTimer);
        output.putInt("RoarCooldown", roarCooldown);
        output.putInt("RestPhase", getRestPhase());
        output.putInt("RestTimer", restTimer);
        output.storeNullable("OwnerUUID", UUIDUtil.CODEC, ownerUuid);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(IS_PASSIVE, input.getBooleanOr("IsPassive", false));
        honeyTimer = input.getIntOr("HoneyTimer", 0);
        if (honeyTimer > 0) entityData.set(IS_HONEY_DISTRACTED, true);
        roarCooldown = input.getIntOr("RoarCooldown", 0);
        entityData.set(REST_PHASE, input.getIntOr("RestPhase", REST_NONE));
        restTimer = input.getIntOr("RestTimer", 0);
        ownerUuid = input.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public float getVoicePitch() {
        return 0.9F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GrizzlyBearEntity>("main", 3, test -> {
            GrizzlyBearEntity bear = test.animatable();

            int restPhase = bear.getRestPhase();
            if (restPhase == REST_SITTING) return test.setAndContinue(SIT_SEQUENCE);
            if (restPhase == REST_SIT_EXIT) return test.setAndContinue(SIT_EXIT);
            if (restPhase == REST_SLEEPING) return test.setAndContinue(SLEEP_SEQUENCE);
            if (restPhase == REST_SLEEP_EXIT) return test.setAndContinue(SLEEP_EXIT);

            if (bear.isRoaring()) return test.setAndContinue(ROAR);
            if (bear.isAttacking()) return test.setAndContinue(ATTACK);

            if (test.isMoving()) return test.setAndContinue(WALK);
            return test.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Roars at the nearest visible survival player within 10 blocks (unless tamed) or at a humanoid monster there. */
    static class RoarGoal extends Goal {
        private final GrizzlyBearEntity bear;
        private @Nullable LivingEntity roarTarget;

        RoarGoal(GrizzlyBearEntity bear) {
            this.bear = bear;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!bear.canRoar()) return false;
            double range = 10.0;
            AABB box = bear.getBoundingBox().inflate(range);

            if (!bear.isPassive()) {
                Player nearest = bear.level().getNearestPlayer(bear.getX(), bear.getY(), bear.getZ(), range, false);
                if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && bear.hasLineOfSight(nearest)) {
                    roarTarget = nearest;
                    return true;
                }
            }

            List<Monster> mobs = bear.level().getEntitiesOfClass(Monster.class, box,
                    e -> isHumanoidHostile(e) && bear.hasLineOfSight(e));
            if (!mobs.isEmpty()) {
                roarTarget = mobs.get(0);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return bear.isRoaring();
        }

        @Override
        public void start() {
            bear.startRoar();
            if (roarTarget != null) bear.getLookControl().setLookAt(roarTarget, 30.0F, 30.0F);
        }

        @Override
        public void stop() {
            roarTarget = null;
        }

        static boolean isHumanoidHostile(Monster entity) {
            return entity instanceof Zombie
                    || entity instanceof AbstractSkeleton
                    || entity instanceof Pillager
                    || entity instanceof Vindicator
                    || entity instanceof Evoker;
        }
    }

    /** Falls asleep now and then at night (day time 13000-23000) after standing idle for a while. */
    static class RestGoal extends Goal {
        private final GrizzlyBearEntity bear;
        private int idleTicks = 0;

        RestGoal(GrizzlyBearEntity bear) {
            this.bear = bear;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (bear.isResting()) return true;
            if (bear.isBaby() || bear.isRoaring() || bear.isEating() || bear.isCarrying() || bear.isAttacking()) return false;
            if (bear.getTarget() != null || bear.getNavigation().isInProgress()) return false;

            long dayTime = bear.level().getOverworldClockTime() % 24000;
            if (dayTime >= 13000 && dayTime <= 23000) {
                idleTicks++;
                if (idleTicks > 100 && bear.getRandom().nextInt(200) == 0) {
                    bear.startResting();
                    return true;
                }
            } else {
                idleTicks = 0;
            }
            return bear.isResting();
        }

        @Override
        public boolean canContinueToUse() {
            if (!bear.isResting()) return false;
            if (!bear.isHoneyDistracted() && bear.getTarget() != null) {
                bear.interruptRest();
                return true;
            }
            return true;
        }

        @Override
        public void start() {
            bear.getNavigation().stop();
        }

        @Override
        public void stop() {
            idleTicks = 0;
        }
    }

    /**
     * Fishing: the bear swims into water within 16 blocks and waits; a fish's wake spirals in (fishing particles) and
     * it catches something. Salmon (55%) is carried in its mouth back to shore, then inland, where it sits and eats
     * it; anything else (cod, string, pufferfish, leather, a name tag or a gold ingot) pops up out of the water.
     */
    static class FishGoal extends Goal {
        private enum Phase { SWIM_TO_WATER, FISHING, RETURN_TO_SHORE, WALK_TO_EAT, SIT_AND_EAT }

        private static final int FISH_WAIT_MIN = 80;
        private static final int FISH_WAIT_MAX = 200;
        /** The fish takes two seconds to reach the bear. */
        private static final int APPROACH_TICKS = 40;
        private static final double FISH_START_DISTANCE = 5.0;

        private final GrizzlyBearEntity bear;
        private @Nullable Phase phase;
        private @Nullable BlockPos deepWaterPos;
        private @Nullable BlockPos shorePos;
        private @Nullable BlockPos eatPos;
        private int fishBiteTimer;
        private int cooldown;

        // The approaching fish's wake.
        private double fishX;
        private double fishZ;
        private double fishAngle;
        private double fishDistance;

        FishGoal(GrizzlyBearEntity bear) {
            this.bear = bear;
            this.cooldown = 200 + bear.getRandom().nextInt(400);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (bear.isBaby() || bear.isResting() || bear.isRoaring() || bear.isEating() || bear.isCarrying()
                    || bear.isAttacking()) return false;
            if (bear.getTarget() != null) return false;
            if (cooldown-- > 0) return false;
            deepWaterPos = findDeepWater();
            if (deepWaterPos == null) return false;
            shorePos = findShoreNear(deepWaterPos);
            return shorePos != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (bear.getTarget() != null && !bear.isHoneyDistracted()) return false;
            if (bear.isRoaring()) return false;
            return phase != null;
        }

        @Override
        public void start() {
            phase = Phase.SWIM_TO_WATER;
            bear.setFishing(true);
            bear.getNavigation().moveTo(deepWaterPos.getX() + 0.5, deepWaterPos.getY() + 0.5, deepWaterPos.getZ() + 0.5, 1.0);
        }

        @Override
        public void tick() {
            if (phase == null) return;
            switch (phase) {
                case SWIM_TO_WATER -> tickSwimToWater();
                case FISHING -> tickFishing();
                case RETURN_TO_SHORE -> tickReturnToShore();
                case WALK_TO_EAT -> tickWalkToEat();
                case SIT_AND_EAT -> tickSitAndEat();
            }
        }

        private void tickSwimToWater() {
            if (bear.isInWater()) {
                bear.getNavigation().stop();
                fishBiteTimer = FISH_WAIT_MIN + bear.getRandom().nextInt(FISH_WAIT_MAX - FISH_WAIT_MIN);
                phase = Phase.FISHING;
                return;
            }
            if (!bear.getNavigation().isInProgress()) {
                bear.getNavigation().moveTo(deepWaterPos.getX() + 0.5, deepWaterPos.getY() + 0.5, deepWaterPos.getZ() + 0.5, 1.0);
            }
        }

        private void tickFishing() {
            bear.getLookControl().setLookAt(bear.getX(), bear.getY() - 2.0, bear.getZ());

            fishBiteTimer--;

            // During the last APPROACH_TICKS a fish spirals in towards the bear, like vanilla fishing's wake.
            if (fishBiteTimer == APPROACH_TICKS) {
                fishAngle = bear.getRandom().nextDouble() * Math.PI * 2.0;
                fishDistance = FISH_START_DISTANCE;
                fishX = bear.getX() + Math.cos(fishAngle) * fishDistance;
                fishZ = bear.getZ() + Math.sin(fishAngle) * fishDistance;
            }

            if (fishBiteTimer <= APPROACH_TICKS && fishBiteTimer > 0 && bear.level() instanceof ServerLevel level) {
                double progress = 1.0 - ((double) fishBiteTimer / APPROACH_TICKS);
                fishDistance = FISH_START_DISTANCE * (1.0 - progress);
                // The spiral turns faster as the fish closes in.
                fishAngle += 0.4 + progress * 0.8;

                fishX = bear.getX() + Math.cos(fishAngle) * fishDistance;
                fishZ = bear.getZ() + Math.sin(fishAngle) * fishDistance;

                double py = bear.getY() + 0.1;
                level.sendParticles(ParticleTypes.FISHING, fishX, py, fishZ, 0, 0.0, 0.0, 0.0, 0.0);
                if (bear.getRandom().nextInt(3) == 0) {
                    level.sendParticles(ParticleTypes.SPLASH, fishX, py, fishZ, 2, 0.1, 0.05, 0.1, 0.0);
                }
                // The bite: a big splash at the bear.
                if (fishBiteTimer <= 3) {
                    level.sendParticles(ParticleTypes.SPLASH, bear.getX(), py, bear.getZ(), 6, 0.3, 0.1, 0.3, 0.1);
                }
            }

            if (fishBiteTimer <= 0) {
                catchSomething();
            }
        }

        private void tickReturnToShore() {
            double distToShore = bear.distanceToSqr(shorePos.getX() + 0.5, shorePos.getY(), shorePos.getZ() + 0.5);

            if (distToShore <= 4.0 && !bear.isInWater()) {
                // Ashore: find a dry spot further inland.
                eatPos = findDryLandPosition();
                if (eatPos == null) {
                    // None found: walk 5 blocks on from the shore, away from the water.
                    Vec3 away = new Vec3(shorePos.getX() - deepWaterPos.getX(), 0, shorePos.getZ() - deepWaterPos.getZ()).normalize();
                    eatPos = new BlockPos(shorePos.getX() + (int) (away.x * 5), shorePos.getY(), shorePos.getZ() + (int) (away.z * 5));
                }
                bear.getNavigation().moveTo(eatPos.getX() + 0.5, eatPos.getY(), eatPos.getZ() + 0.5, 1.0);
                phase = Phase.WALK_TO_EAT;
                return;
            }

            if (!bear.getNavigation().isInProgress()) {
                bear.getNavigation().moveTo(shorePos.getX() + 0.5, shorePos.getY(), shorePos.getZ() + 0.5, 1.0);
            }
        }

        private void tickWalkToEat() {
            double dist = bear.distanceToSqr(eatPos.getX() + 0.5, eatPos.getY(), eatPos.getZ() + 0.5);
            if (dist <= 4.0 || !bear.getNavigation().isInProgress()) {
                bear.getNavigation().stop();
                bear.setFishing(false);
                bear.startEating();
                bear.postEatRestTimer = POST_EAT_REST_MIN + bear.getRandom().nextInt(POST_EAT_REST_MAX - POST_EAT_REST_MIN);
                phase = Phase.SIT_AND_EAT;
            }
        }

        private void tickSitAndEat() {
            // Waits out the whole meal: sitting, the eat timer and standing up.
            if (!bear.isEating() && bear.getRestPhase() == REST_NONE) {
                phase = null;
            }
        }

        private void catchSomething() {
            if (!(bear.level() instanceof ServerLevel level)) return;

            level.sendParticles(ParticleTypes.SPLASH, bear.getX(), bear.getY() + 0.3, bear.getZ(), 12, 0.4, 0.2, 0.4, 0.15);

            if (bear.getRandom().nextFloat() < 0.55F) {
                bear.setMouthItem(MOUTH_SALMON);
                bear.getNavigation().moveTo(shorePos.getX() + 0.5, shorePos.getY(), shorePos.getZ() + 0.5, 1.0);
                phase = Phase.RETURN_TO_SHORE;
            } else {
                ItemEntity item = new ItemEntity(level, bear.getX(), bear.getY() + 0.5, bear.getZ(), getTreasureItem());
                item.setDeltaMovement(0, 0.15, 0);
                level.addFreshEntity(item);
                phase = null;
            }
        }

        @Override
        public void stop() {
            deepWaterPos = null;
            shorePos = null;
            eatPos = null;
            phase = null;
            cooldown = 200 + bear.getRandom().nextInt(400);
            bear.setFishing(false);
            if (bear.isCarrying()) bear.setMouthItem(MOUTH_NONE);
        }

        private ItemStack getTreasureItem() {
            float r = bear.getRandom().nextFloat();
            if (r < 0.30F) return new ItemStack(Items.COD);
            if (r < 0.50F) return new ItemStack(Items.STRING);
            if (r < 0.65F) return new ItemStack(Items.PUFFERFISH);
            if (r < 0.75F) return new ItemStack(Items.LEATHER);
            if (r < 0.97F) return new ItemStack(Items.NAME_TAG);
            return new ItemStack(Items.GOLD_INGOT);
        }

        /** The nearest water source within 16 blocks (3 up or down) with another source above it. */
        private @Nullable BlockPos findDeepWater() {
            BlockPos origin = bear.blockPosition();
            int range = 16;
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -range; dz <= range; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (!bear.level().getFluidState(pos).isSource()) continue;
                        if (!bear.level().getFluidState(pos.above()).isSource()) continue;
                        double dist = bear.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = pos;
                        }
                    }
                }
            }
            return best;
        }

        /** The nearest dry air block on solid ground within 5 blocks (2 up or down) of the water. */
        private @Nullable BlockPos findShoreNear(BlockPos water) {
            int sr = 5;
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -sr; dx <= sr; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -sr; dz <= sr; dz++) {
                        BlockPos pos = water.offset(dx, dy, dz);
                        if (!bear.level().getBlockState(pos).isAir()) continue;
                        if (!bear.level().getBlockState(pos.below()).isRedstoneConductor(bear.level(), pos.below())) continue;
                        if (!bear.level().getFluidState(pos).isEmpty()) continue;
                        double dist = pos.distSqr(water);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = pos;
                        }
                    }
                }
            }
            return best;
        }

        /** A two-high dry spot on solid ground 6 to 3 blocks on from the bear, away from the water. */
        private @Nullable BlockPos findDryLandPosition() {
            BlockPos origin = bear.blockPosition();
            if (deepWaterPos == null) return null;
            Vec3 away = new Vec3(origin.getX() - deepWaterPos.getX(), 0, origin.getZ() - deepWaterPos.getZ()).normalize();
            Level level = bear.level();
            for (int dist = 6; dist >= 3; dist--) {
                int tx = origin.getX() + (int) (away.x * dist);
                int tz = origin.getZ() + (int) (away.z * dist);
                BlockPos candidate = new BlockPos(tx, origin.getY(), tz);
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos check = candidate.offset(0, dy, 0);
                    if (level.getBlockState(check.below()).isRedstoneConductor(level, check.below())
                            && level.getBlockState(check).isAir()
                            && level.getBlockState(check.above()).isAir()
                            && level.getFluidState(check).isEmpty()) {
                        return check;
                    }
                }
            }
            return null;
        }
    }
}
