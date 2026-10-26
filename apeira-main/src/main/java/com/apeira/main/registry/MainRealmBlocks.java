package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.realm.balloon.StarterBalloonBlock;
import com.apeira.main.realm.balloon.StarterBalloonBlockEntity;
import com.apeira.main.realm.balloon.StarterBalloonPartBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The blocks realms place themselves: the starter balloon ({@link com.apeira.main.realm.balloon.StarterBalloon}) and its
 * basket floor. They have no items, drop nothing and cannot be broken in survival; the balloon moves only through
 * {@code /realm setspawn}.
 */
public final class MainRealmBlocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /** The balloon itself, torch-bright (light 14). */
    public static final DeferredBlock<StarterBalloonBlock> STARTER_BALLOON = R.blocks().registerBlock("starter_balloon",
            StarterBalloonBlock::new, () -> properties().lightLevel(state -> 14));
    /** The eight cells of the basket floor around the balloon. */
    public static final DeferredBlock<StarterBalloonPartBlock> STARTER_BALLOON_PART = R.blocks().registerBlock("starter_balloon_part",
            StarterBalloonPartBlock::new, MainRealmBlocks::properties);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarterBalloonBlockEntity>> STARTER_BALLOON_ENTITY = R.blockEntityType(
            "starter_balloon", StarterBalloonBlockEntity::new, STARTER_BALLOON);

    private MainRealmBlocks() {}

    /** Bedrock's hardness and resistance, a wooden sound, no occlusion: the model is drawn by the block entity. */
    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .pushReaction(PushReaction.IMMOVEABLE)
                .isValidSpawn((state, level, pos, type) -> false)
                .noOcclusion();
    }

    public static void init(ApeiraRegistrar registrar) {}
}
