package com.apeira.decor.furniture;

import static com.apeira.decor.furniture.Furniture.piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Every furniture piece. The legacy Crates mod, ORFurniture and CosmoLib pieces were purged to make way for the
 * unified crate system; the pieces here are the Origin Realms server pack's (names and crates from Ren's uicapture of
 * the {@code /catalog} "Reward Furniture" pages). Shapes are in pixels, facing north; they are first guesses, to be
 * tuned with apeira-dev's furniture wand.
 */
final class FurnitureCatalogue {
    private FurnitureCatalogue() {}

    static List<Furniture> all() {
        List<Furniture> all = new ArrayList<>();
        badgerCrate(all);
        return List.copyOf(all);
    }

    /**
     * The Badger Crate's furniture, {@code models/custom/furniture/badger/} in the server pack. Each is drawn as a large
     * piece: its {@code item/furniture/<name>_placed} model is the pack model with the server's item display
     * transform (its {@code fixed} display, scale halved) as the {@code none} display.
     */
    private static void badgerCrate(List<Furniture> all) {
        all.add(piece("outdoor_rocking_chair", "badger_crate").seat(8).large(1, 0, 0).shape(
                1, 0, 1, 15, 8, 15,
                1, 8, 12, 15, 16, 15));
        all.add(piece("wedge_tent", "badger_crate").large(1, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("sash_banner", "badger_crate").large(1, 0, 0).shape(
                6, 0, 6, 10, 16, 10));
        all.add(piece("wooden_cart", "badger_crate").large(1, 0, 0).shape(
                0, 0, 0, 16, 14, 16));
        all.add(piece("stall_cart", "badger_crate").large(1, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("wooden_windmill", "badger_crate").large(1, 0, 0).shape(
                4, 0, 4, 12, 16, 12));
    }
}
