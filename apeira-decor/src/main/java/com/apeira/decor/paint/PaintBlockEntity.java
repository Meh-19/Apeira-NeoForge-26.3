package com.apeira.decor.paint;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.registry.DecorBlockEntities;
import com.apeira.decor.registry.DecorDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds the paint of a {@link PaintableBlock}. The paint is the block entity's {@code apeira:paint} component, so
 * placing a painted item paints the block, and the block's loot table and pick-block copy it back to the item.
 *
 * <p>Clients receive the paint with the chunk and with each change; the block's renderer draws it from there.
 */
public class PaintBlockEntity extends BlockEntity {
    private static final String PAINT_TAG = "paint";

    private @Nullable Integer paint;

    public PaintBlockEntity(BlockPos pos, BlockState state) {
        this(DecorBlockEntities.PAINT.get(), pos, state);
    }

    /** For block entities that keep paint and more, such as large furniture. */
    protected PaintBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The block's paint, or {@code null} if it is unpainted. */
    public @Nullable Integer paint() {
        return paint;
    }

    /** Paints the block, or clears its paint with {@code null}, and sends the change to clients. */
    public void setPaint(@Nullable Integer paint) {
        this.paint = paint == null ? null : paint & 0xFFFFFF;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        paint = input.getInt(PAINT_TAG).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (paint != null) {
            output.putInt(PAINT_TAG, paint);
        }
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
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        paint = components.get(DecorDataComponents.PAINT.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DecorDataComponents.PAINT.get(), paint);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(PAINT_TAG);
    }
}
