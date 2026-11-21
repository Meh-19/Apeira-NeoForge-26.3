package com.apeira.decor.model;

import java.util.Locale;

/**
 * Tags in model bone, group and cube names: the words after the first underscore-separated part, in any order, so
 * {@code door_tintable_ignore} and {@code door_ignore_tintable} both carry {@code tintable} and {@code ignore}.
 *
 * <ul>
 *   <li>{@code _tintable} (and the starter balloon's {@code _tint}): the bone takes the piece's paint.</li>
 *   <li>{@code _ignore} ({@link #IGNORE}): the bone, cube or group, and everything under it, is drawn but left out of
 *       every generated hitbox. The generators are {@code scripts/artifact_footprints.py} (furniture and artifact
 *       footprints, crates' and every future model's included) and {@code scripts/parse_blockbench_hitboxes.py},
 *       through {@code asset_common.hitbox_ignored}; this is the same rule for Java code reading models.</li>
 * </ul>
 */
public final class BoneTags {
    /** Left out of hitboxes. */
    public static final String IGNORE = "ignore";

    private BoneTags() {}

    /** Whether {@code name} carries {@code tag} ({@code "tint"}, {@code "tintable"}, {@code "ignore"}) as one of its words. */
    public static boolean has(String name, String tag) {
        String[] words = name.toLowerCase(Locale.ROOT).split("_");
        for (int i = 1; i < words.length; i++) {
            if (words[i].equals(tag)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a bone, cube or group named {@code name} is left out of hitboxes: its name contains {@code _ignore}. */
    public static boolean hitboxIgnored(String name) {
        return name.toLowerCase(Locale.ROOT).contains("_" + IGNORE);
    }
}
