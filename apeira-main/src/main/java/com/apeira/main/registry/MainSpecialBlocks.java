package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.block.EchoGlassBlock;
import com.apeira.main.block.SpikeTrapBlock;
import com.apeira.main.block.TurkeyMeatBlock;
import com.apeira.main.block.TurkeySkeletonBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy Xenon mod's blocks with a behaviour of their own, and its other natural blocks: echo glass, the spike
 * trap, placed turkeys and their skeleton (blocks without items: the turkey items place them), mycena mushroom blocks and yellow birch leaves. Ids and
 * properties are Xenon's ({@link MainBlockProperties}).
 */
public final class MainSpecialBlocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();

    public static final DeferredBlock<SpikeTrapBlock> SPIKE_TRAP = block("spike_trap", SpikeTrapBlock::new, MainBlockProperties::stoneBricks);
    /** Yellow birch leaves drop birch saplings; Xenon gave them no random ticks, so they never decay. */
    public static final DeferredBlock<LeavesBlock> YELLOW_BIRCH_LEAVES = block("yellow_birch_leaves",
            p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p), MainBlockProperties::leaves);
    public static final DeferredBlock<EchoGlassBlock> ECHO_GLASS = block("echo_glass", EchoGlassBlock::new, MainBlockProperties::echoGlass);
    public static final DeferredBlock<RotatedPillarBlock> MYCENA_MUSHROOM_STEM = block("mycena_mushroom_stem", RotatedPillarBlock::new,
            MainBlockProperties::mushroom);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MYCENA_MUSHROOM_STEM = block("stripped_mycena_mushroom_stem",
            RotatedPillarBlock::new, MainBlockProperties::mushroom);
    public static final DeferredBlock<Block> MYCENA_MUSHROOM_BLOCK = block("mycena_mushroom_block", Block::new, MainBlockProperties::mushroom);
    public static final DeferredBlock<TurkeyMeatBlock> RAW_TURKEY_BLOCK = blockOnly("raw_turkey_block",
            p -> new TurkeyMeatBlock(p, false, MainMaterials.RAW_TURKEY, MainSpecialBlocks.TURKEY_SKELETON), MainBlockProperties::turkey);
    public static final DeferredBlock<TurkeyMeatBlock> COOKED_TURKEY_BLOCK = blockOnly("cooked_turkey_block",
            p -> new TurkeyMeatBlock(p, true, MainMaterials.COOKED_TURKEY, MainSpecialBlocks.TURKEY_SKELETON), MainBlockProperties::turkey);
    public static final DeferredBlock<TurkeySkeletonBlock> TURKEY_SKELETON = blockOnly("turkey_skeleton", TurkeySkeletonBlock::new,
            MainBlockProperties::turkeySkeleton);

    private MainSpecialBlocks() {}

    /** Every block registered here. */
    public static List<DeferredBlock<? extends Block>> all() {
        return List.copyOf(ALL);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = R.blockWithItem(name, factory, properties, p -> p);
        ALL.add(block);
        return block;
    }

    /** A block without an item. */
    private static <B extends Block> DeferredBlock<B> blockOnly(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = R.blocks().registerBlock(name, factory, properties);
        ALL.add(block);
        return block;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
