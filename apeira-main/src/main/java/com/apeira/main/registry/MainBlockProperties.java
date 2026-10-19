package com.apeira.main.registry;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The block properties of the legacy Xenon mod's blocks ({@code net.meh.xenon.block.BlockBuilders}), one method per
 * Xenon builder, named as Xenon named them. Xenon's {@code burnable()} is vanilla's {@code ignitedByLava()}.
 */
final class MainBlockProperties {
    private MainBlockProperties() {}

    /** Xenon's stone: needs a pickaxe, stone's strength and sound. */
    static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM);
    }

    static BlockBehaviour.Properties netherBrick() {
        return BlockBehaviour.Properties.of().requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)
                .strength(1.5F, 6.0F).sound(SoundType.NETHER_BRICKS);
    }

    static BlockBehaviour.Properties terracottaShingles() {
        return BlockBehaviour.Properties.of().requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)
                .strength(1.25F, 4.2F).sound(SoundType.STONE);
    }

    static BlockBehaviour.Properties quiltedWool() {
        return BlockBehaviour.Properties.of().ignitedByLava().instrument(NoteBlockInstrument.GUITAR).strength(0.8F)
                .sound(SoundType.WOOL);
    }

    /** A crystal cluster, like an amethyst cluster: no collision, light 5, popped off by pistons. */
    static BlockBehaviour.Properties crystal() {
        return BlockBehaviour.Properties.of().sound(SoundType.AMETHYST_CLUSTER).strength(1.5F).noCollision()
                .lightLevel(state -> 5).pushReaction(PushReaction.POPPED).noOcclusion();
    }

    /** Crystal blocks, bricks and the prism lantern glow faintly (light 5). */
    static BlockBehaviour.Properties crystalBlock() {
        return BlockBehaviour.Properties.of().randomTicks().strength(1.5F).sound(SoundType.AMETHYST).lightLevel(state -> 5)
                .requiresCorrectToolForDrops();
    }

    static BlockBehaviour.Properties crystalLantern() {
        return crystalBlock().lightLevel(state -> 15);
    }

    static BlockBehaviour.Properties bone() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.XYLOPHONE).requiresCorrectToolForDrops().strength(2.0F)
                .sound(SoundType.BONE_BLOCK);
    }

    /** Xenon's crates, baskets and bundles are logs: strength 2/3, burning in lava. */
    static BlockBehaviour.Properties crates() {
        return log(SoundType.WOOD);
    }

    static BlockBehaviour.Properties bambooCrates() {
        return log(SoundType.BAMBOO_WOOD);
    }

    static BlockBehaviour.Properties cactusCrates() {
        return log(SoundType.WOOL);
    }

    private static BlockBehaviour.Properties log(SoundType sound) {
        return BlockBehaviour.Properties.of().ignitedByLava().strength(2.0F, 3.0F).instrument(NoteBlockInstrument.BASS).sound(sound);
    }

    static BlockBehaviour.Properties shoji() {
        return BlockBehaviour.Properties.of().instabreak().sound(SoundType.SCAFFOLDING);
    }

    static BlockBehaviour.Properties table() {
        return BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(2.0F, 3.0F).instrument(NoteBlockInstrument.BASS)
                .sound(SoundType.WOOD);
    }

    static BlockBehaviour.Properties shroomlight() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).strength(1.0F).lightLevel(state -> 15)
                .sound(SoundType.SHROOMLIGHT);
    }

    static BlockBehaviour.Properties grass() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).strength(0.6F).sound(SoundType.GRASS)
                .pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties dampMossBlock() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).strength(0.1F).sound(SoundType.MOSS)
                .pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties sand() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5F)
                .sound(SoundType.SAND);
    }

    static BlockBehaviour.Properties packedSand() {
        return BlockBehaviour.Properties.of().strength(0.5F, 0.5F).sound(SoundType.SAND);
    }

    static BlockBehaviour.Properties packedDirt() {
        return BlockBehaviour.Properties.of().strength(0.5F, 0.5F).sound(SoundType.GRAVEL);
    }

    /** Stone bricks: needs a pickaxe. */
    static BlockBehaviour.Properties stoneBricks() {
        return BlockBehaviour.Properties.of().requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM).strength(1.5F, 6.0F)
                .sound(SoundType.STONE);
    }

    /** Leaves, which Xenon gave no random ticks: they never decay on their own. */
    static BlockBehaviour.Properties leaves() {
        return BlockBehaviour.Properties.of().ignitedByLava().strength(0.2F).instrument(NoteBlockInstrument.HAT).sound(SoundType.GRASS)
                .noOcclusion();
    }

    /** Grasses, bushes and flowers: no collision, broken at once, offset like vanilla grass. */
    static BlockBehaviour.Properties plants() {
        return plantsNoOffset().offsetType(BlockBehaviour.OffsetType.XZ);
    }

    static BlockBehaviour.Properties plantsNoOffset() {
        return BlockBehaviour.Properties.of().noCollision().noOcclusion().instabreak().sound(SoundType.GRASS).ignitedByLava()
                .pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties smallLily() {
        return BlockBehaviour.Properties.of().noCollision().mapColor(MapColor.PLANT).instabreak().sound(SoundType.GRASS).noOcclusion()
                .pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties bramble() {
        return plants().randomTicks();
    }

    static BlockBehaviour.Properties vines() {
        return BlockBehaviour.Properties.of().noCollision().instabreak().randomTicks().sound(SoundType.CAVE_VINES).noOcclusion();
    }

    static BlockBehaviour.Properties mossHang() {
        return vines().pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties mossCover() {
        return BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.MOSS).pushReaction(PushReaction.POPPED).noCollision()
                .noOcclusion();
    }

    static BlockBehaviour.Properties stickPlaceable() {
        return BlockBehaviour.Properties.of().noCollision().noOcclusion().instabreak().sound(SoundType.WOOD);
    }

    static BlockBehaviour.Properties ash() {
        return BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.SOUL_SAND).pushReaction(PushReaction.POPPED).noCollision()
                .noOcclusion();
    }

    /** Xenon's frost hid the faces under it through its block class; here that is {@code noOcclusion()}. */
    static BlockBehaviour.Properties frost() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).strength(0.1F).noCollision().sound(SoundType.SNOW)
                .pushReaction(PushReaction.POPPED).noOcclusion();
    }

    /** Rocks and pebbles. */
    static BlockBehaviour.Properties rockItem() {
        return BlockBehaviour.Properties.of().strength(0.0F).noCollision().offsetType(BlockBehaviour.OffsetType.XZ).sound(SoundType.STONE)
                .noOcclusion();
    }

    static BlockBehaviour.Properties echoGlass() {
        return BlockBehaviour.Properties.of().pushReaction(PushReaction.IMMOVEABLE).instrument(NoteBlockInstrument.HAT).strength(0.3F)
                .sound(SoundType.STONE).isValidSpawn(Blocks::never).isRedstoneConductor((state, level, pos) -> false).noOcclusion()
                .isSuffocating((state, level, pos) -> false);
    }

    static BlockBehaviour.Properties mushroom() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD)
                .ignitedByLava();
    }

    /** Leaf litter. */
    static BlockBehaviour.Properties natural() {
        return BlockBehaviour.Properties.of().noCollision().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties naturalMushroom() {
        return BlockBehaviour.Properties.of().noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties turkey() {
        return BlockBehaviour.Properties.of().noCollision().strength(0.3F).sound(SoundType.STONE).pushReaction(PushReaction.POPPED);
    }

    static BlockBehaviour.Properties turkeySkeleton() {
        return BlockBehaviour.Properties.of().noCollision().instabreak().sound(SoundType.BONE_BLOCK).pushReaction(PushReaction.POPPED);
    }
}
