package com.apeira.main.datagen;

import java.util.Optional;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.world.level.block.Block;

/**
 * Block states and block models for {@link MainBlocks}, {@link MainWoods}, {@link MainStones}, {@link MainLanterns} and
 * {@link MainCrates}. Block items use their block's model, except buttons and fences, which use vanilla's inventory
 * models.
 *
 * <p>Textures are ORForged's or, for the blocks it had none for, the Origin Realms server pack's: {@code block/<name>},
 * with {@code block/<name>_top} for the ends of pillars, logs, beams, lanterns, chiseled blocks and slate. A wood's log
 * texture is also its wood's, and carved planks show their carving on every side.
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
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            simpleBlock(mineral.brick().get());
            pillar(mineral.pillar().get());
        }

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
        // The server pack's vertical planks: the vanilla planks texture, turned on its side by the hand-made parent.
        ModelTemplate verticalPlanks = new ModelTemplate(Optional.of(modLoc("block/vertical_planks")), Optional.empty(), TextureSlot.ALL);
        for (MainWoods.VanillaVerticalPlanks vertical : MainWoods.VANILLA_VERTICAL_PLANKS) {
            simpleBlock(vertical.vertical().get(), TexturedModel.createDefault(block -> TextureMapping.cube(vertical.planks()), verticalPlanks));
        }

        for (var stone : MainStones.all()) {
            if (MainStones.columns().contains(stone)) {
                simpleBlock(stone.get(), TexturedModel.COLUMN_ALT);
            } else {
                simpleBlock(stone.get());
            }
        }
        MainLanterns.all().forEach(lantern -> simpleBlock(lantern.block().get(), TexturedModel.COLUMN_ALT));

        // Crates are drawn by their block entity, whatever their rotation; the hand-made block model only gives the
        // breaking particles, and the item uses the crate's hand-made item model.
        for (var crate : MainCrates.all()) {
            Block block = crate.get();
            existingModelBlock(block);
            blockItemModel(block, modLoc("item/" + crate.getId().getPath()));
        }
    }
}
