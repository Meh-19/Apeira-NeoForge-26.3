package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.block.PrismLanternBlock;

import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy Xenon mod's crystals in seven colours: a crystal cluster ({@code <colour>_crystal}, vanilla's amethyst
 * cluster block, 5 pixels tall and 10 wide like Xenon's {@code CrystalBlock(5, 3)}), a crystal block, bricks, chiseled
 * bricks and a lantern, all amethyst blocks; and the prism lantern. Properties are Xenon's ({@link MainBlockProperties}).
 *
 * <p>Xenon gave the purple lantern a plain crystal block's properties, so it was the only lantern without light; here it
 * glows like the others.
 */
public final class MainCrystals {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public record Crystal(String colour, DeferredBlock<AmethystClusterBlock> cluster, DeferredBlock<AmethystBlock> block,
            DeferredBlock<AmethystBlock> bricks, DeferredBlock<AmethystBlock> chiseledBricks, DeferredBlock<AmethystBlock> lantern) {
        public List<DeferredBlock<? extends Block>> all() {
            return List.of(cluster, block, bricks, chiseledBricks, lantern);
        }
    }

    public static final List<Crystal> CRYSTALS = List.of(crystal("white"), crystal("red"), crystal("orange"), crystal("yellow"),
            crystal("green"), crystal("blue"), crystal("purple"));
    public static final DeferredBlock<PrismLanternBlock> PRISM_LANTERN = R.blockWithItem("prism_lantern", PrismLanternBlock::new,
            MainBlockProperties::crystalBlock, p -> p);

    private MainCrystals() {}

    /** Every block registered here. */
    public static List<DeferredBlock<? extends Block>> all() {
        List<DeferredBlock<? extends Block>> all = new ArrayList<>();
        CRYSTALS.forEach(crystal -> all.addAll(crystal.all()));
        all.add(PRISM_LANTERN);
        return all;
    }

    private static Crystal crystal(String colour) {
        String name = colour + "_crystal";
        return new Crystal(colour,
                R.blockWithItem(name, p -> new AmethystClusterBlock(5.0F, 10.0F, p), MainBlockProperties::crystal, p -> p),
                R.blockWithItem(name + "_block", AmethystBlock::new, MainBlockProperties::crystalBlock, p -> p),
                R.blockWithItem(name + "_bricks", AmethystBlock::new, MainBlockProperties::crystalBlock, p -> p),
                R.blockWithItem("chiseled_" + name + "_bricks", AmethystBlock::new, MainBlockProperties::crystalBlock, p -> p),
                R.blockWithItem(name + "_lantern", AmethystBlock::new, MainBlockProperties::crystalLantern, p -> p));
    }

    public static void init(ApeiraRegistrar registrar) {}
}
