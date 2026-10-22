package com.apeira.main.client.navigator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorActionPayload.Action;
import com.apeira.main.navigator.NavigatorPage;

import net.minecraft.resources.Identifier;

/**
 * What each navigator page draws and where its buttons are, copied from the UI captures of Origin Realms' navigator
 * ({@code reference_code/ui_captures/origin_realms/}).
 *
 * <p>Origin Realms draws a page as a 6-row chest whose title is a string of font glyphs: each glyph is a 256x256 bitmap
 * with ascent 19, placed after an 8-pixel negative space, so every layer's top-left corner is at
 * {@code (leftPos, topPos - 6)}. Its buttons are the chest's slots, so a button here is a set of slot indices (slot
 * {@code i} is at {@code (8 + i % 9 * 18, 18 + i / 9 * 18)}).
 */
final class NavigatorLayout {
    private static final String ROOT = "textures/gui/navigator/";

    private NavigatorLayout() {}

    /**
     * A clickable area of a page.
     *
     * @param slots  the chest slots it covers
     * @param key    {@code navigator.apeira.<key>} names it and {@code navigator.apeira.<key>.desc}, if present,
     *               describes it in its tooltip
     * @param action what clicking it asks the server to do
     */
    record Button(int[] slots, String key, NavigatorActionPayload action) {}

    record Page(List<Identifier> layers, List<Button> buttons) {}

    static Page of(NavigatorPage page) {
        return switch (page) {
            case MAIN -> new Page(layers("navigator/background"), List.of(
                    new Button(block(0, 4, 4), "realms", NavigatorActionPayload.open(NavigatorPage.REALMS)),
                    new Button(block(5, 4, 4), "open_worlds", NavigatorActionPayload.open(NavigatorPage.OPEN_WORLDS)),
                    new Button(block(36, 4, 2), "homes", new NavigatorActionPayload(Action.HOMES, 0)),
                    new Button(block(41, 4, 2), "spawn", new NavigatorActionPayload(Action.SPAWN, 0))));
            case REALMS -> new Page(realmsLayers(), List.of(
                    back(NavigatorPage.MAIN),
                    new Button(slots(9, 10, 11), "realm_teleport", new NavigatorActionPayload(Action.REALM_TELEPORT, 0)),
                    new Button(slots(12, 13, 14), "realm_access", NavigatorActionPayload.open(NavigatorPage.REALM_ACCESS)),
                    new Button(slots(15), "realm_settings", NavigatorActionPayload.open(NavigatorPage.REALM_SETTINGS))));
            case REALM_ACCESS -> new Page(layers("navigation", "navigation_back_enabled", "realms/navigation_realms",
                    "realms/access/background_members", "realms/filter_all", "realms/sort_alpha_asc"),
                    List.of(back(NavigatorPage.REALMS)));
            case REALM_SETTINGS -> new Page(concat(realmsLayers(),
                    layers("realms/home/settings/background", "realms/home/settings/visiting_rule_all")),
                    List.of(back(NavigatorPage.REALMS)));
            case OPEN_WORLDS -> new Page(layers("navigation", "profile/navigation_profile", "profile/background",
                    "profile/open_worlds/background", "profile/open_worlds/orange_button", "profile/open_worlds/red_button",
                    "profile/open_worlds/yellow_button"), List.of(
                    new Button(slots(7), "close", NavigatorActionPayload.open(NavigatorPage.MAIN)),
                    world(0, "red", slots(10, 11, 19, 20)),
                    world(1, "orange", slots(12, 13, 14, 21, 22, 23)),
                    world(2, "yellow", slots(15, 16, 24, 25)),
                    world(3, "green", slots(28, 29, 37, 38)),
                    world(4, "blue", slots(30, 31, 32, 39, 40, 41)),
                    world(5, "purple", slots(33, 34, 42, 43))));
        };
    }

    private static List<Identifier> realmsLayers() {
        return layers("navigation", "navigation_back_enabled", "realms/navigation_realms", "realms/home/background",
                "realms/filter_all", "realms/sort_count_desc");
    }

    private static Button back(NavigatorPage to) {
        return new Button(slots(0), "back", NavigatorActionPayload.open(to));
    }

    private static Button world(int index, String colour, int[] slots) {
        return new Button(slots, "open_world." + colour, new NavigatorActionPayload(Action.OPEN_WORLD, index));
    }

    private static List<Identifier> layers(String... paths) {
        return Arrays.stream(paths).map(path -> ApeiraIds.id(ROOT + path + ".png")).toList();
    }

    private static List<Identifier> concat(List<Identifier> first, List<Identifier> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private static int[] slots(int... slots) {
        return slots;
    }

    /** The slots of a {@code columns} by {@code rows} block whose top-left slot is {@code first}. */
    private static int[] block(int first, int columns, int rows) {
        return IntStream.range(0, columns * rows).map(i -> first + i / columns * 9 + i % columns).toArray();
    }
}
