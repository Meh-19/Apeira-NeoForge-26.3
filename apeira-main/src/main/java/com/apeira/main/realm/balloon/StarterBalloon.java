package com.apeira.main.realm.balloon;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;
import com.apeira.main.realm.RealmPreset;
import com.apeira.main.registry.MainRealmBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

/**
 * A realm's starter balloon: a {@link StarterBalloonBlock} and its 3x3 basket floor, eight vanilla barriers around it,
 * on which the realm's players arrive ({@link #isFootprint}; no player can break or change them, {@code RealmProtection}).
 * Standing on it opens the navigator ({@code navigator/BalloonNavigatorTrigger}). When a realm is first visited,
 * {@link #build} places the {@code apeira:starter_balloon_<wood>_platform} structure at its origin (Ren's, in the region's wood: oak, acacia,
 * spruce for the cold and arctic regions, or jungle, {@link #wood}) with beam legs down to the ground, and the balloon in
 * it; the platform is ordinary blocks and can be broken. {@code /realm setspawn} moves the balloon ({@link #move}), and
 * the realm settings paint it ({@link #paint}).
 *
 * <p>The area the balloon takes is 3x3 blocks and 2 high above a solid floor ({@link #isClear}): its basket, and room to
 * stand in it. The same check colours the client's placement cursor.
 */
public final class StarterBalloon {
    /** Blocks from the centre to the edge of the basket floor. */
    public static final int RADIUS = 1;
    /** The basket and the space above its floor. */
    public static final int HEIGHT = 2;
    /** The balloon's top is 134 pixels over its floor; the origin is cleared this high over the platform. */
    private static final int CLEAR_HEIGHT = 9;
    /** The platform structures are 5x5 and 3 high: base, floor and fence ring. */
    private static final int PLATFORM_RADIUS = 2;
    private static final int PLATFORM_HEIGHT = 3;
    /** The deepest the platform's legs reach to find the ground. */
    private static final int MAX_LEG = 24;

    private StarterBalloon() {}

    /** The wood of {@code preset}'s platform. */
    public static String wood(RealmPreset preset) {
        return switch (preset) {
            case LUSH -> "oak";
            case ARID -> "acacia";
            case COLD, ARCTIC -> "spruce";
            case TROPICAL -> "jungle";
        };
    }

    /** The structure template of a wood's platform, {@code apeira:starter_balloon_<wood>_platform}. */
    public static Identifier platform(String wood) {
        return ApeiraIds.id("starter_balloon_" + wood + "_platform");
    }

    /**
     * Builds the platform and the balloon at the realm's origin. Ren's platforms are 5x5 and 3 high: a beam-framed base,
     * the floor with steps up on its north side, and a fence ring around a 3x3 opening, where the balloon's basket
     * stands. The base replaces the top block at 0, 0, the space over the platform is cleared first, and beams reach
     * down from the base's corners to the ground. The balloon is turned so its front (north as Ren built it) faces
     * {@code facing}, the way the arriving player looks, and the platform half a turn further, so its steps (north as
     * Ren built it) are behind the balloon. Returns the balloon's position, its floor's
     * centre.
     */
    public static BlockPos build(ServerLevel level, RealmPreset preset, Direction facing) {
        level.getChunk(0, 0);
        int top = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0), level.getSeaLevel() + 1);
        BlockPos base = new BlockPos(0, top - 1, 0);
        String wood = wood(preset);
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-PLATFORM_RADIUS, 1, -PLATFORM_RADIUS),
                base.offset(PLATFORM_RADIUS, CLEAR_HEIGHT, PLATFORM_RADIUS))) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        BlockPos corner = base.offset(-PLATFORM_RADIUS, 0, -PLATFORM_RADIUS);
        Optional<StructureTemplate> template = level.getStructureTemplateManager().get(platform(wood));
        if (template.isPresent()) {
            // Turned about the platform's centre column, so it stays on the same 5x5.
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation(facing.getOpposite()))
                    .setRotationPivot(new BlockPos(PLATFORM_RADIUS, 0, PLATFORM_RADIUS));
            template.get().placeInWorld(level, corner, corner, settings, RandomSource.create(), Block.UPDATE_CLIENTS);
        } else {
            Apeira.LOGGER.warn("Missing structure {}; building a plain platform", platform(wood));
            BlockState planks = block(Identifier.withDefaultNamespace(wood + "_planks"), Blocks.OAK_PLANKS);
            for (BlockPos pos : BlockPos.betweenClosed(corner, base.offset(PLATFORM_RADIUS, PLATFORM_HEIGHT - 2, PLATFORM_RADIUS))) {
                level.setBlock(pos, planks, Block.UPDATE_CLIENTS);
            }
        }
        // Legs under the base's corners, down to the ground (or the sea floor).
        BlockState leg = block(ApeiraIds.id(wood + "_beam"), Blocks.OAK_LOG);
        for (int x : new int[] {-PLATFORM_RADIUS, PLATFORM_RADIUS}) {
            for (int z : new int[] {-PLATFORM_RADIUS, PLATFORM_RADIUS}) {
                BlockPos.MutableBlockPos pos = base.offset(x, -1, z).mutable();
                for (int depth = 0; depth < MAX_LEG && pos.getY() > level.getMinY() && level.getBlockState(pos).canBeReplaced(); depth++) {
                    level.setBlock(pos, leg, Block.UPDATE_CLIENTS);
                    pos.move(0, -1, 0);
                }
            }
        }
        // Inside the fence ring, on the floor.
        BlockPos balloon = base.above(PLATFORM_HEIGHT - 1);
        placeBlocks(level, balloon, facing);
        return balloon;
    }

    /** The turn that takes the platform's north front to {@code facing}. */
    static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** {@code id}'s default state, standing up if it has an axis, or {@code fallback}'s when it is not registered. */
    private static BlockState block(Identifier id, Block fallback) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        BlockState state = (block == Blocks.AIR ? fallback : block).defaultBlockState();
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : state;
    }

    /**
     * Whether the balloon may stand with its floor's centre at {@code balloon}: inside the world border and the build
     * height, and {@link #isClear}. The client's placement cursor and the server ask the same.
     */
    public static boolean canPlace(Level level, BlockPos balloon, @Nullable BlockPos current) {
        return level.getWorldBorder().isWithinBounds(new AABB(balloon).inflate(RADIUS, 0, RADIUS))
                && level.isInWorldBounds(balloon.above(HEIGHT))
                && isClear(level, balloon, current);
    }

    /**
     * Whether the balloon fits with its floor's centre at {@code balloon}: nothing with a collision box (nor any liquid)
     * in its 3x3x2 area, except the balloon at {@code current} that is being moved, and a solid block under the centre
     * that is not part of that balloon.
     */
    public static boolean isClear(BlockGetter level, BlockPos balloon, @Nullable BlockPos current) {
        BlockPos below = balloon.below();
        BlockState floor = level.getBlockState(below);
        if (floor.getCollisionShape(level, below).isEmpty() || isBalloon(floor)) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(balloon.offset(-RADIUS, 0, -RADIUS), balloon.offset(RADIUS, HEIGHT - 1, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (current != null && isBalloon(state) && pos.getY() == current.getY() && Math.abs(pos.getX() - current.getX()) <= RADIUS
                    && Math.abs(pos.getZ() - current.getZ()) <= RADIUS) {
                continue;
            }
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBalloon(BlockState state) {
        return state.is(MainRealmBlocks.STARTER_BALLOON.get()) || state.is(Blocks.BARRIER);
    }

    /** Whether {@code pos} is a cell of the balloon's 3x3 footprint at {@code balloon}: the balloon or one of its barriers. */
    public static boolean isFootprint(BlockPos balloon, BlockPos pos) {
        return pos.getY() == balloon.getY() && Math.abs(pos.getX() - balloon.getX()) <= RADIUS && Math.abs(pos.getZ() - balloon.getZ()) <= RADIUS;
    }

    /**
     * Moves the balloon to {@code balloon} (its floor's centre): the old one at {@code current}, if any, is removed first.
     * Its front faces {@code facing}. The caller has checked {@link #isClear}.
     */
    public static void move(ServerLevel level, @Nullable BlockPos current, BlockPos balloon, Direction facing, @Nullable Integer paint) {
        if (current != null) {
            remove(level, current);
        }
        placeBlocks(level, balloon, facing);
        paint(level, balloon, paint);
    }

    /** The balloon at {@code balloon}, facing {@code facing}, and the eight barriers of its floor around it. */
    static void placeBlocks(ServerLevel level, BlockPos balloon, Direction facing) {
        BlockState part = Blocks.BARRIER.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(balloon.offset(-RADIUS, 0, -RADIUS), balloon.offset(RADIUS, 0, RADIUS))) {
            if (!pos.equals(balloon)) {
                level.setBlock(pos, part, Block.UPDATE_ALL);
            }
        }
        level.setBlock(balloon, MainRealmBlocks.STARTER_BALLOON.get().defaultBlockState().setValue(StarterBalloonBlock.FACING, facing),
                Block.UPDATE_ALL);
    }

    /** Removes the balloon at {@code balloon} and its floor, if they are still there. */
    public static void remove(ServerLevel level, BlockPos balloon) {
        if (level.getBlockState(balloon).is(MainRealmBlocks.STARTER_BALLOON.get())) {
            // Its floor goes with it (StarterBalloonBlock#affectNeighborsAfterRemoval).
            level.setBlock(balloon, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            removeParts(level, balloon);
        }
    }

    /** Removes the barrier floor around the balloon at {@code balloon}. */
    static void removeParts(ServerLevel level, BlockPos balloon) {
        for (BlockPos pos : BlockPos.betweenClosed(balloon.offset(-RADIUS, 0, -RADIUS), balloon.offset(RADIUS, 0, RADIUS))) {
            if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Paints the balloon at {@code balloon}, or clears its paint with {@code null}, loading its chunk if needed. */
    public static void paint(ServerLevel level, BlockPos balloon, @Nullable Integer paint) {
        if (level.getBlockEntity(balloon) instanceof StarterBalloonBlockEntity entity) {
            entity.setPaint(paint);
        }
    }
}
