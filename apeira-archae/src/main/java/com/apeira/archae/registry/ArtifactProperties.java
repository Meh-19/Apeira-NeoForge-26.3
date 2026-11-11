package com.apeira.archae.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;

/**
 * The block properties of Argon's artifacts ({@code net.meh.argon.block.BlockBuilders}), one constant per Argon
 * builder. Its {@code luminance} is the light level; every artifact lets light through ({@code noOcclusion}).
 *
 * <p>Argon's {@code breakInstantly()} artifacts (wood, decorations, pots, lanterns, torches, candles, cobwebs) take some
 * breaking here instead, since breaking any block of a multiblock artifact breaks all of it: wood takes 1 (the decor
 * furniture's strength), cobwebs 0.3 and the others 0.5, Argon's {@code defaultArtifact} stone. Argon's own strengths
 * (0.5, and 2 for hard wood) are kept.
 */
enum ArtifactProperties implements Supplier<BlockBehaviour.Properties> {
    /** Argon's {@code defaultArtifact}: stone, strength 0.5. */
    DEFAULT {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(0.5F).sound(SoundType.STONE);
        }
    },
    DEFAULT_NO_COLLISION {
        @Override
        public BlockBehaviour.Properties get() {
            return DEFAULT.get().noCollision();
        }
    },
    DEFAULT_LUMINANCE {
        @Override
        public BlockBehaviour.Properties get() {
            return DEFAULT.get().lightLevel(state -> 10);
        }
    },
    /** Argon's {@code defaultDecorationLuminanceArtifact}: no collision, light 10. */
    DECORATION_LUMINANCE {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(DECORATION_STRENGTH).noCollision().sound(SoundType.STONE).lightLevel(state -> 10);
        }
    },
    /** Argon's {@code woodenArtifact}: wood, strength 1 (Argon's broke at once), as the decor furniture. */
    WOODEN {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(1.0F).pushReaction(PushReaction.PUSH_PULL).sound(SoundType.WOOD);
        }
    },
    /** Argon's {@code hardWoodenArtifact}: wood, strength 2 (Argon's {@code strength} overrides its instant break). */
    HARD_WOODEN {
        @Override
        public BlockBehaviour.Properties get() {
            return WOODEN.get().strength(2.0F);
        }
    },
    WOODEN_NO_COLLISION {
        @Override
        public BlockBehaviour.Properties get() {
            return WOODEN.get().noCollision();
        }
    },
    /** Argon's {@code decorationArtifact}: stone, no collision. */
    DECORATION {
        @Override
        public BlockBehaviour.Properties get() {
            return DECORATION_COLLISION.get().noCollision();
        }
    },
    /** Argon's {@code decorationColArtifact}: a decoration with collision. */
    DECORATION_COLLISION {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(DECORATION_STRENGTH).pushReaction(PushReaction.PUSH_PULL).sound(SoundType.STONE);
        }
    },
    /** Argon's {@code potArtifact}: a decorated pot's sound, no collision. */
    POT {
        @Override
        public BlockBehaviour.Properties get() {
            return POT_COLLISION.get().noCollision();
        }
    },
    POT_COLLISION {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(DECORATION_STRENGTH).pushReaction(PushReaction.PUSH_PULL).sound(SoundType.DECORATED_POT);
        }
    },
    /** Argon's {@code lanternArtifact}: a lantern's sound, no collision, light 10. */
    LANTERN {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(DECORATION_STRENGTH).noCollision().pushReaction(PushReaction.PUSH_PULL).sound(SoundType.LANTERN).lightLevel(state -> 10);
        }
    },
    /** Argon's {@code torchArtifact}: a lantern artifact with wood's sound. */
    TORCH {
        @Override
        public BlockBehaviour.Properties get() {
            return LANTERN.get().sound(SoundType.WOOD);
        }
    },
    CANDLE {
        @Override
        public BlockBehaviour.Properties get() {
            return LANTERN.get().sound(SoundType.CANDLE);
        }
    },
    /** Argon's {@code silkArtifact}: cobwebs, broken by pistons. */
    SILK {
        @Override
        public BlockBehaviour.Properties get() {
            return base().strength(SILK_STRENGTH).noCollision().pushReaction(PushReaction.POPPED).sound(SoundType.COBWEB);
        }
    };

    /** Hand-breaking takes 0.75 seconds. */
    private static final float DECORATION_STRENGTH = 0.5F;
    private static final float SILK_STRENGTH = 0.3F;

    private static BlockBehaviour.Properties base() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).noOcclusion();
    }
}
