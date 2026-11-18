package com.apeira.main.crate;

import java.util.List;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Badger Crate's opening, played in the opener's instance at {@link #ORIGIN}: Ren's
 * Blockbench animation ({@code geckolib/animations/badger_crate.animation.json}, played once and held on its last frame)
 * with the three rolled rewards on its {@code item_1}-{@code item_3} bones. It never moves, is never saved and cannot
 * be hit. Synced: the rewards, their tiers (two bits each) and which one is selected; the client draws the rest.
 *
 * <p>Its yaw is 180 so GeckoLib's entity turn ({@code 180 - yaw}) is none and the model stands as recorded.
 */
public class BadgerCrateShowEntity extends Entity implements GeoEntity {
    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.badger_crate.open");
    /** Where the opening plays in an instance: the model origin at block corner 0, 63, 1. */
    public static final net.minecraft.world.phys.Vec3 ORIGIN = new net.minecraft.world.phys.Vec3(0.0, 63.0, 1.0);
    private static final List<EntityDataAccessor<ItemStack>> ITEMS = List.of(
            SynchedEntityData.defineId(BadgerCrateShowEntity.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(BadgerCrateShowEntity.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(BadgerCrateShowEntity.class, EntityDataSerializers.ITEM_STACK));
    private static final EntityDataAccessor<Integer> TIERS = SynchedEntityData.defineId(BadgerCrateShowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SELECTED = SynchedEntityData.defineId(BadgerCrateShowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SELECTING = SynchedEntityData.defineId(BadgerCrateShowEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BadgerCrateShowEntity(EntityType<? extends BadgerCrateShowEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setYRot(180.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        ITEMS.forEach(item -> builder.define(item, ItemStack.EMPTY));
        builder.define(TIERS, 0);
        builder.define(SELECTED, 0);
        builder.define(SELECTING, false);
    }

    public void setRewards(List<BadgerLoot.Reward> rewards) {
        int tiers = 0;
        for (int i = 0; i < ITEMS.size() && i < rewards.size(); i++) {
            entityData.set(ITEMS.get(i), rewards.get(i).stack().copy());
            tiers |= rewards.get(i).tier().ordinal() << (i * 2);
        }
        entityData.set(TIERS, tiers);
    }

    /** Reward {@code slot}, 0 to 2. */
    public ItemStack item(int slot) {
        return entityData.get(ITEMS.get(slot));
    }

    public BadgerTier tier(int slot) {
        return BadgerTier.byId(entityData.get(TIERS) >> (slot * 2) & 3);
    }

    public int selected() {
        return entityData.get(SELECTED);
    }

    public void setSelected(int slot) {
        entityData.set(SELECTED, Math.floorMod(slot, ITEMS.size()));
    }

    /** True once the rewards can be picked (ten seconds in): the client shows the outline and "Selected" from then on. */
    public boolean selecting() {
        return entityData.get(SELECTING);
    }

    public void setSelecting(boolean selecting) {
        entityData.set(SELECTING, selecting);
    }

    @Override
    public void tick() {
        // Nothing moves it; only its age advances, which the animation and the client's camera follow.
        tickCount++;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BadgerCrateShowEntity>("open", 0, state -> state.setAndContinue(OPEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
