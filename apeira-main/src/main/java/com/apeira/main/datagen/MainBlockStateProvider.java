package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainWoods;

import net.minecraft.world.level.block.Block;

/**
 * Block states and block models for {@link MainBlocks}, {@link MainWoods} and {@link MainCrates}. Block items use
 * their block's model, except buttons and fences, which use vanilla's inventory models.
 *
 * <p>Textures are ORForged's: {@code block/<name>}, with {@code block/<name>_top} for pillar, log and beam ends. A
 * wood's log texture is also its wood's, and carved planks show their carving on every side.
 */
public final class MainBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        simpleBlock(MainBlocks.PLATINUM_BLOCK.get());
        simpleBlock(MainBlocks.PLATINUM_BRICKS.get());
        pillar(MainBlocks.PLATINUM_PILLAR.get());
        MainBlocks.ores().forEach(ore -> simpleBlock(ore.get()));
        simpleBlock(MainBlocks.RUBY_BLOCK.get());
        simpleBlock(MainBlocks.RUBY_BRICKS.get());
        pillar(MainBlocks.RUBY_PILLAR.get());
        simpleBlock(MainBlocks.DIAMOND_BRICK.get());

        for (MainWoods.Wood wood : MainWoods.woods()) {
            log(wood.log().get(), wood.wood().get());
            log(wood.strippedLog().get(), wood.strippedWood().get());
            family(wood.planks().family());
            simpleBlock(wood.verticalPlanks().get());
            wood.leaves().forEach(leaves -> leaves(leaves.get()));
        }
        simpleBlock(MainWoods.PALM_CROWN.get());
        MainWoods.WISTERIA_FLOWERS.forEach(flowers -> leaves(flowers.get()));
        MainWoods.fungalPlankSets().stream().map(PlankSet::family).forEach(this::family);
        simpleBlock(MainWoods.VERTICAL_FUNGAL_PLANKS.get());
        MainWoods.carvedPlanks().forEach(carved -> simpleBlock(carved.get()));
        MainWoods.beams().forEach(beam -> pillar(beam.get()));

        // Crates are drawn by their block entity, whatever their rotation; the hand-made block model only gives the
        // breaking particles, and the item uses the crate's hand-made item model.
        for (var crate : MainCrates.all()) {
            Block block = crate.get();
            existingModelBlock(block);
            blockItemModel(block, modLoc("item/" + crate.getId().getPath()));
        }
    }
}
