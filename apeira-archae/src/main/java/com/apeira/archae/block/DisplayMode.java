package com.apeira.archae.block;

/**
 * How {@code ArtifactRenderer} lays an artifact's item model into its block, as Argon's
 * {@code FurnitureDisplayMode} did. Argon's models are Origin Realms item display models, made to be shown in the
 * {@code fixed} display context lying on a face of the block; each mode turns the model onto one face.
 */
public enum DisplayMode {
    /** Hung under the top face, turned by the block's {@link ArtifactBlock#ROTATION}. */
    TOP_FACE,
    /** Lying on the bottom face, turned by the block's rotation. */
    FLOOR,
    /** On the face the block's {@code facing} points away from, against the wall it was placed on. */
    WALL,
    /** Lying on the top face of the block, turned by the block's rotation. */
    BLOCK_UP
}
