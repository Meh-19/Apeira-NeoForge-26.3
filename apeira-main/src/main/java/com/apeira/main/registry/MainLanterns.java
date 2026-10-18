package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy ORForged mod's lanterns: a paper lantern for each vanilla and ORForged wood, and crimson and warped shroom
 * lanterns. Each is a full block giving light 15, with glowstone's properties, scaffolding's sound and planks'
 * strength, and a block item of the same name.
 *
 * <p>ORForged registered them without textures; theirs are the Origin Realms server pack's
 * ({@code assets/originrealms/textures/block/misc_blocks/lanterns/}): {@code block/<name>} on the sides and
 * {@code block/<name>_top} on the ends.
 */
public final class MainLanterns {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<Lantern> ALL = new ArrayList<>();

    static {
        lantern("oak_paper_lantern", Blocks.OAK_PLANKS);
        lantern("spruce_paper_lantern", Blocks.SPRUCE_PLANKS);
        lantern("birch_paper_lantern", Blocks.BIRCH_PLANKS);
        lantern("jungle_paper_lantern", Blocks.JUNGLE_PLANKS);
        lantern("acacia_paper_lantern", Blocks.ACACIA_PLANKS);
        lantern("dark_oak_paper_lantern", Blocks.DARK_OAK_PLANKS);
        lantern("mangrove_paper_lantern", Blocks.MANGROVE_PLANKS);
        lantern("cherry_paper_lantern", Blocks.CHERRY_PLANKS);
        lantern("crimson_shroom_lantern", Blocks.CRIMSON_PLANKS);
        lantern("warped_shroom_lantern", Blocks.WARPED_PLANKS);
        MainWoods.woods().forEach(wood -> lantern(wood.name() + "_paper_lantern", wood.planks().planks()));
        lantern("fungal_paper_lantern", MainWoods.FUNGAL.planks());
        lantern("bamboo_paper_lantern", Blocks.BAMBOO_PLANKS);
    }

    private MainLanterns() {}

    /** Every lantern, in registration order: ORForged's, with its own woods' after the vanilla ones. */
    public static List<Lantern> all() {
        return List.copyOf(ALL);
    }

    private static void lantern(String name, ItemLike planks) {
        DeferredBlock<Block> block = R.blockWithItem(name, Block::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLOWSTONE)
                .lightLevel(state -> 15).strength(2.0F, 3.0F).sound(SoundType.SCAFFOLDING), p -> p);
        ALL.add(new Lantern(block, planks));
    }

    public static void init(ApeiraRegistrar registrar) {}

    /** A lantern and the planks its recipe takes. */
    public record Lantern(DeferredBlock<Block> block, ItemLike planks) {}
}
