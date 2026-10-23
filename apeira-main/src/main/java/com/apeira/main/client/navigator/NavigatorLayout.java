package com.apeira.main.client.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorActionPayload.Action;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.navigator.NavigatorView;
import com.apeira.main.realm.RealmRole;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * What each navigator page draws and where its buttons are, copied from the UI captures of Origin Realms' navigator,
 * realm Access, Settings and popups ({@code reference_code/ui_captures/origin_realms/}).
 *
 * <p>Origin Realms draws a page as a 6-row chest whose title is a string of font glyphs: each glyph is a 256x256 bitmap
 * with ascent 19, placed after an 8-pixel negative space, so every layer's top-left corner is at
 * {@code (leftPos, topPos - 6)}. Overlays (the selected filter and sort, page arrows, toggles, the picked visiting rule
 * and role, greyed-out buttons) are further layers chosen from the server's {@link NavigatorView}. Its buttons are the
 * chest's slots, so a button here is a set of slot indices (slot {@code i} is at {@code (8 + i % 9 * 18, 18 + i / 9 * 18)}),
 * and list entries are player heads in slots 27 to 44. The slots only say which drawn button is meant: the screen
 * hit-tests and highlights each button by its outline in the layers ({@link NavigatorBounds}).
 */
final class NavigatorLayout {
    private static final String ROOT = "textures/gui/navigator/";
    /** The slot of the first list entry; a list page shows {@link NavigatorView#PAGE_SIZE} entries from here. */
    static final int FIRST_ENTRY = 27;
    /** The slot of the head in an Edit Player or Edit Member popup. */
    static final int POPUP_HEAD = 22;
    /** The list slots of most pages: two rows from {@link #FIRST_ENTRY}. */
    private static final int[] LIST_SLOTS = IntStream.range(FIRST_ENTRY, FIRST_ENTRY + NavigatorView.PAGE_SIZE).toArray();
    /** The Homes popup's grid: three rows of five. */
    private static final int[] HOME_SLOTS = block(20, 5, 3);
    private static final String[] PRESETS = {"lush", "arid", "cold", "arctic", "tropical"};

    private NavigatorLayout() {}

    /**
     * A clickable or hoverable area of a page.
     *
     * @param slots   the chest slots it covers
     * @param tooltip its tooltip, first line the name
     * @param action  what clicking it asks the server to do; null for areas that only explain themselves
     */
    record Button(int[] slots, List<Component> tooltip, @Nullable NavigatorActionPayload action) {}

    /**
     * @param layers      textures drawn in order
     * @param buttons     checked before the entries, in order
     * @param entrySlots  the slots the view's entries are drawn in as heads, in order; empty when it shows none
     * @param pageNumbers whether the page number is drawn between the page arrows
     */
    record Page(List<Identifier> layers, List<Button> buttons, int[] entrySlots, boolean pageNumbers) {}

    static Page of(NavigatorView view) {
        List<Identifier> layers = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();
        NavigatorPage page = view.page();
        switch (page) {
            case MAIN -> {
                layer(layers, view.has(NavigatorView.HAS_REALM) ? "navigator/background" : "navigator/no_realm");
                buttons.add(button(block(0, 4, 4), "realms", NavigatorActionPayload.open(NavigatorPage.REALMS)));
                buttons.add(button(block(5, 4, 4), "open_worlds", NavigatorActionPayload.open(NavigatorPage.OPEN_WORLDS)));
                buttons.add(button(block(36, 4, 2), "homes", NavigatorActionPayload.of(Action.HOMES)));
                buttons.add(button(block(41, 4, 2), "spawn", NavigatorActionPayload.of(Action.SPAWN)));
            }
            case OPEN_WORLDS -> {
                layer(layers, "navigation", "profile/navigation_profile", "profile/background", "profile/open_worlds/background");
                String[] colours = {"red", "orange", "yellow", "green", "blue", "purple"};
                int[][] slots = {{10, 11, 19, 20}, {12, 13, 14, 21, 22, 23}, {15, 16, 24, 25}, {28, 29, 37, 38}, {30, 31, 32, 39, 40, 41},
                        {33, 34, 42, 43}};
                buttons.add(button(slots(7), "close", NavigatorActionPayload.open(NavigatorPage.MAIN)));
                for (int i = 0; i < colours.length; i++) {
                    layer(layers, "profile/open_worlds/" + colours[i] + "_button");
                    buttons.add(button(slots[i], "open_world." + colours[i], new NavigatorActionPayload(Action.OPEN_WORLD, i)));
                }
            }
            case HOMES -> {
                layer(layers, "navigation", "profile/navigation_profile", "profile/background", "profile/homes/background");
                buttons.add(button(slots(15), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                // Every free cell of the grid sets a home where the player stands.
                int[] free = IntStream.range(view.entries().size(), HOME_SLOTS.length).map(i -> HOME_SLOTS[i]).toArray();
                if (free.length > 0) {
                    buttons.add(button(free, "set_home", NavigatorActionPayload.of(Action.SET_HOME)));
                }
                return new Page(List.copyOf(layers), List.copyOf(buttons), HOME_SLOTS, false);
            }
            case SOCIAL -> socialPage(view, layers, buttons);
            case PROFILE_OTHER -> profilePage(view, layers, buttons);
            default -> realmPage(view, layers, buttons);
        }
        boolean showsList = switch (page) {
            case REALMS, REALM_IN_REALM, REALM_MEMBERS, REALM_BANNED, SOCIAL -> true;
            default -> false;
        };
        return new Page(List.copyOf(layers), List.copyOf(buttons), showsList ? LIST_SLOTS : new int[0], showsList);
    }

    /**
     * Social: the friends list with requests, filtered All, Online, Offline or Requests. The party controls above it
     * are not available yet. Only the captured filter (All) and sort (Last Online) have overlay textures.
     */
    private static void socialPage(NavigatorView view, List<Identifier> layers, List<Button> buttons) {
        layer(layers, "navigation", "social/navigation_social", "social/background");
        listControls(view, layers);
        if (view.filter() == 0) {
            layer(layers, "social/filter_all");
        }
        if (view.sort() == 0) {
            layer(layers, "social/sort_online");
        }
        navigationBar(NavigatorPage.SOCIAL, true, buttons);
        buttons.add(unavailable(slots(9, 10, 11, 12, 13, 14, 15), "party"));
        buttons.add(button(slots(17), "learn_social", null));
        buttons.add(button(slots(45, 46), "previous_page", new NavigatorActionPayload(Action.PAGE, -1)));
        buttons.add(button(slots(50, 51), "next_page", new NavigatorActionPayload(Action.PAGE, 1)));
        buttons.add(new Button(slots(52), options("filter", view.filter(), "all", "online", "offline", "requests"), NavigatorActionPayload.of(Action.FILTER)));
        buttons.add(new Button(slots(53), options("sort", view.sort(), "last_online", "alpha_asc"), NavigatorActionPayload.of(Action.SORT)));
        emptyHint(view, buttons, "hint.friend");
    }

    /**
     * Another player's profile (Origin Realms' {@code profile_other}): realm invite or removal, the friend button
     * (send, accept or decline a request, or unfriend; red when they are a friend) and a visit to their realm. Party
     * and trade invites, cosmetics and auctions are not available yet.
     */
    private static void profilePage(NavigatorView view, List<Identifier> layers, List<Button> buttons) {
        layer(layers, "profile/profile_other/background");
        boolean member = view.has(NavigatorView.TARGET_MEMBER);
        boolean friend = view.has(NavigatorView.TARGET_FRIEND);
        if (friend) {
            layer(layers, "profile/profile_other/remove_friend");
        }
        if (member) {
            layer(layers, "profile/profile_other/remove_realm");
        }
        // The texture's buttons: Party Invite and Trade Invite across slots 23-26 and 32-35, then two rows of pairs.
        buttons.add(unavailable(slots(23, 24, 25, 26), "party_invite"));
        buttons.add(unavailable(slots(32, 33, 34, 35), "trade_invite"));
        if (member) {
            buttons.add(button(slots(41, 42), "remove_realm", NavigatorActionPayload.of(Action.REMOVE_MEMBER)));
        } else if (view.has(NavigatorView.TARGET_INVITED)) {
            buttons.add(button(slots(41, 42), "invite_player.invited", null));
        } else {
            buttons.add(button(slots(41, 42), "invite_player", NavigatorActionPayload.of(Action.INVITE)));
        }
        String friendKey = friend ? "unfriend" : view.has(NavigatorView.TARGET_REQUESTED) ? "friend_request.incoming"
                : view.has(NavigatorView.TARGET_REQUEST_SENT) ? "friend_request.sent" : "friend_request.send";
        buttons.add(button(slots(43, 44), friendKey, NavigatorActionPayload.of(Action.FRIEND)));
        // The info icon in the panel's corner shows whether they are online.
        view.target().ifPresent(target -> buttons.add(new Button(slots(22), List.copyOf(target.lines()), null)));
        buttons.add(unavailable(slots(50, 51), "view_auctions"));
        buttons.add(button(slots(52, 53), "teleport_realm", NavigatorActionPayload.of(Action.VISIT)));
    }

    /** The Realms pages: the navigation bar, a tab background, list controls, and the page's panel or popup. */
    private static void realmPage(NavigatorView view, List<Identifier> layers, List<Button> buttons) {
        NavigatorPage page = view.page();
        NavigatorPage list = NavigatorService.listPage(page);
        boolean noBack = page == NavigatorPage.REALMS && view.has(NavigatorView.NO_BACK);
        // Without the back_enabled layer the navigation bar's own arrow shows, greyed out.
        layer(layers, noBack ? new String[] {"navigation"} : new String[] {"navigation", "navigation_back_enabled"});
        layer(layers, "realms/navigation_realms", switch (list) {
            case REALM_IN_REALM -> "realms/access/background_in_realm";
            case REALM_MEMBERS -> "realms/access/background_members";
            case REALM_BANNED -> "realms/access/background_banned";
            default -> "realms/home/background";
        });
        listControls(view, layers);
        layer(layers, "realms/filter_" + NavigatorService.filterName(page, view.filter()), "realms/sort_" + NavigatorService.sortName(page, view.sort()));

        // Popups and panels come first so they take their slots from the page under them.
        switch (page) {
            case EDIT_PLAYER -> {
                layer(layers, "realms/access/edit_player/background");
                boolean invited = view.has(NavigatorView.TARGET_MEMBER) || view.has(NavigatorView.TARGET_INVITED);
                if (invited) {
                    layer(layers, "realms/access/edit_player/invite_off");
                }
                buttons.add(button(slots(15), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                buttons.add(button(slots(21), "kick_player", NavigatorActionPayload.of(Action.KICK)));
                buttons.add(button(slots(23), "ban_member", NavigatorActionPayload.of(Action.BAN)));
                buttons.add(button(slots(30, 31, 32), invited ? "invite_player.off" : "invite_player",
                        invited ? null : NavigatorActionPayload.of(Action.INVITE)));
                return;
            }
            case EDIT_MEMBER -> {
                layer(layers, "realms/access/edit_member/background",
                        view.has(NavigatorView.TARGET_TRUSTED) ? "realms/access/edit_member/role_trusted" : "realms/access/edit_member/role_resident");
                buttons.add(button(slots(15), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                buttons.add(button(slots(21), "kick_member", NavigatorActionPayload.of(Action.REMOVE_MEMBER)));
                buttons.add(button(slots(23), "ban_member", NavigatorActionPayload.of(Action.BAN)));
                buttons.add(new Button(slots(30, 31, 32), roleTooltip("trusted", "open_chests", "break_blocks", "place_blocks"),
                        new NavigatorActionPayload(Action.SET_ROLE, RealmRole.TRUSTED.ordinal())));
                buttons.add(new Button(slots(39, 40, 41), roleTooltip("resident", "break_blocks", "place_blocks"),
                        new NavigatorActionPayload(Action.SET_ROLE, RealmRole.RESIDENT.ordinal())));
                return;
            }
            case REALM_RESET_PRESET -> {
                settingsLayers(view, layers);
                layer(layers, "realms/home/settings/preset_modal");
                buttons.add(button(slots(24), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                int[] presetSlots = {30, 31, 32, 39, 40};
                for (int i = 0; i < PRESETS.length; i++) {
                    buttons.add(button(slots(presetSlots[i]), "preset." + PRESETS[i], new NavigatorActionPayload(Action.RESET_PRESET, i)));
                }
                return;
            }
            default -> {}
        }

        navigationBar(page, !noBack, buttons);

        if (page.isSettings()) {
            settingsLayers(view, layers);
            buttons.add(button(slots(15), "realm_settings.close", NavigatorActionPayload.open(NavigatorPage.REALMS)));
            String[] rules = {"everyone", "friends", "members", "nobody"};
            for (int i = 0; i < rules.length; i++) {
                buttons.add(new Button(slots(18 + i), options("visiting_rule", i, rules), new NavigatorActionPayload(Action.VISITING_RULE, i)));
            }
            buttons.add(button(slots(28), "realm_chat.info", null));
            buttons.add(new Button(slots(29, 30), options("realm_chat", view.setting(NavigatorView.PRIVATE_CHAT) ? 0 : 1, "private", "global"),
                    NavigatorActionPayload.of(Action.TOGGLE_CHAT)));
            buttons.add(button(slots(37), "realm_access_toggle.info", null));
            buttons.add(new Button(slots(38, 39), options("realm_access_toggle", view.setting(NavigatorView.ALWAYS_ACCESSIBLE) ? 0 : 1,
                    "always", "online"), NavigatorActionPayload.of(Action.TOGGLE_ACCESS)));
            buttons.add(button(slots(45), "set_spawn", NavigatorActionPayload.of(Action.SET_SPAWN)));
            switch (page) {
                case REALM_RESET -> {
                    boolean overworld = view.has(NavigatorView.CAN_RESET_OVERWORLD);
                    buttons.add(button(slots(46), overworld ? "reset.overworld" : "reset.overworld.off", new NavigatorActionPayload(Action.RESET_DIMENSION, 0)));
                    buttons.add(button(slots(47), "reset.nether.off", new NavigatorActionPayload(Action.RESET_DIMENSION, 1)));
                    buttons.add(button(slots(48), "reset.aether.off", new NavigatorActionPayload(Action.RESET_DIMENSION, 2)));
                }
                case REALM_RESET_CONFIRM -> buttons.add(new Button(slots(46, 47, 48), List.of(
                        name("reset.confirm"), grey(Component.translatable("navigator.apeira.reset.confirm.desc",
                                Component.translatable("navigator.apeira.preset." + PRESETS[Math.clamp(view.preset(), 0, PRESETS.length - 1)])))),
                        NavigatorActionPayload.of(Action.RESET_CONFIRM)));
                default -> buttons.add(button(slots(46, 47, 48), "reset", new NavigatorActionPayload(Action.RESET_DIMENSION, 0)));
            }
            buttons.add(unavailable(slots(51, 52, 53), "balloon_color"));
            return;
        }

        if (page == NavigatorPage.REALMS) {
            buttons.add(button(slots(9, 10, 11), "realm_teleport", NavigatorActionPayload.of(Action.REALM_TELEPORT)));
            buttons.add(button(slots(12, 13, 14), "realm_access", NavigatorActionPayload.open(NavigatorPage.REALM_IN_REALM)));
            buttons.add(button(slots(15), "realm_settings", NavigatorActionPayload.open(NavigatorPage.REALM_SETTINGS)));
            buttons.add(button(slots(17), "learn_realms", null));
        } else {
            buttons.add(button(slots(9, 10), "access.in_realm", NavigatorActionPayload.open(NavigatorPage.REALM_IN_REALM)));
            buttons.add(button(slots(11, 12), "access.members", NavigatorActionPayload.open(NavigatorPage.REALM_MEMBERS)));
            buttons.add(button(slots(13, 14), "access.banned", NavigatorActionPayload.open(NavigatorPage.REALM_BANNED)));
        }
        buttons.add(button(slots(45, 46), "previous_page", new NavigatorActionPayload(Action.PAGE, -1)));
        buttons.add(button(slots(50, 51), "next_page", new NavigatorActionPayload(Action.PAGE, 1)));
        String[] filters = filterNames(list);
        String[] sorts = sortNames(list);
        buttons.add(new Button(slots(52), options("filter", view.filter(), filters), NavigatorActionPayload.of(Action.FILTER)));
        buttons.add(new Button(slots(53), options("sort", view.sort(), sorts), NavigatorActionPayload.of(Action.SORT)));
        String hint = page == NavigatorPage.REALM_MEMBERS ? "hint.invite" : page == NavigatorPage.REALM_BANNED ? "hint.ban" : null;
        if (hint != null) {
            emptyHint(view, buttons, hint);
        }
    }

    /** The page arrows that are lit, when there is a page that way. */
    private static void listControls(NavigatorView view, List<Identifier> layers) {
        if (view.pageIndex() > 0) {
            layer(layers, "prev_page_on");
        }
        if (view.pageIndex() < view.pageCount() - 1) {
            layer(layers, "next_page_on");
        }
    }

    /**
     * Slot 0 goes back, unless {@code back} is false (Realms home opened by {@code /realms}): then it is not a button.
     * Realms and Social open their tabs and the others say they are not available yet; the store is one button over
     * slots 7 and 8.
     */
    private static void navigationBar(NavigatorPage page, boolean back, List<Button> buttons) {
        if (back) {
            buttons.add(button(slots(0), "back", NavigatorActionPayload.open(page == NavigatorPage.REALMS || page == NavigatorPage.SOCIAL
                    ? NavigatorPage.MAIN : NavigatorPage.REALMS)));
        }
        String[] bar = {"events", "profile", "badges", "realms_tab", "social", "auction_house"};
        for (int i = 0; i < bar.length; i++) {
            buttons.add(switch (i) {
                case 3 -> button(slots(4), "realms_tab", NavigatorActionPayload.open(NavigatorPage.REALMS));
                case 4 -> button(slots(5), "social", NavigatorActionPayload.open(NavigatorPage.SOCIAL));
                default -> unavailable(slots(i + 1), bar[i]);
            });
        }
        buttons.add(unavailable(slots(7, 8), "store"));
    }

    /** Empty list slots say how to add someone, as Origin Realms' do. */
    private static void emptyHint(NavigatorView view, List<Button> buttons, String hint) {
        int[] empty = IntStream.range(FIRST_ENTRY + view.entries().size(), FIRST_ENTRY + NavigatorView.PAGE_SIZE).toArray();
        if (empty.length > 0) {
            buttons.add(new Button(empty, List.of(Component.translatable("navigator.apeira.hint.cmd")
                    .withStyle(Style.EMPTY.withColor(NavigatorService.LABEL))
                    .append(Component.translatable("navigator.apeira." + hint).withColor(NavigatorService.VALUE))), null));
        }
    }

    private static void settingsLayers(NavigatorView view, List<Identifier> layers) {
        String[] rules = {"all", "friends", "members", "nobody"};
        layer(layers, "realms/home/settings/background", "realms/home/settings/visiting_rule_" + rules[Math.clamp(view.visitingRule(), 0, 3)]);
        if (view.setting(NavigatorView.PRIVATE_CHAT)) {
            layer(layers, "realms/home/settings/local_chat");
        }
        if (view.visitingRule() == 3) {
            layer(layers, "realms/home/settings/accessibility_off");
        } else if (view.setting(NavigatorView.ALWAYS_ACCESSIBLE)) {
            layer(layers, "realms/home/settings/accessibility");
        }
        switch (view.page()) {
            case REALM_RESET -> {
                layer(layers, "realms/home/settings/reset_dimensions");
                if (!view.has(NavigatorView.CAN_RESET_OVERWORLD)) {
                    layer(layers, "realms/home/settings/reset_overworld_off");
                }
                layer(layers, "realms/home/settings/reset_nether_off", "realms/home/settings/reset_aether_off");
            }
            case REALM_RESET_CONFIRM -> layer(layers, "realms/home/settings/reset_confirm");
            default -> {}
        }
    }

    private static String[] filterNames(NavigatorPage list) {
        return switch (list) {
            case REALMS -> new String[] {"all", "added_to", "friends"};
            case REALM_MEMBERS -> new String[] {"all", "resident", "trusted"};
            default -> new String[] {"all"};
        };
    }

    private static String[] sortNames(NavigatorPage list) {
        return switch (list) {
            case REALMS -> new String[] {"alpha_asc", "count_asc", "count_desc"};
            case REALM_IN_REALM -> new String[] {"alpha_asc", "rank"};
            case REALM_MEMBERS -> new String[] {"alpha_asc", "last_online"};
            case REALM_BANNED -> new String[] {"time_asc", "time_desc"};
            default -> new String[] {"alpha_asc"};
        };
    }

    /** "Filter by" with its options as bullets, the picked one white and the rest grey-blue. */
    private static List<Component> options(String key, int picked, String... options) {
        List<Component> lines = new ArrayList<>();
        lines.add(name(key));
        for (int i = 0; i < options.length; i++) {
            int color = i == picked ? NavigatorService.VALUE : NavigatorService.LABEL;
            lines.add(Component.literal("• ").append(Component.translatable("navigator.apeira." + key + "." + options[i]))
                    .withStyle(Style.EMPTY.withColor(color).withItalic(false)));
        }
        return lines;
    }

    private static List<Component> roleTooltip(String role, String... perks) {
        List<Component> lines = new ArrayList<>();
        lines.add(name("role_button." + role));
        for (String perk : perks) {
            lines.add(Component.literal("• ").append(Component.translatable("navigator.apeira.perk." + perk))
                    .withStyle(Style.EMPTY.withColor(NavigatorService.VALUE).withItalic(false)));
        }
        return lines;
    }

    private static Button button(int[] slots, String key, @Nullable NavigatorActionPayload action) {
        List<Component> lines = new ArrayList<>();
        lines.add(name(key));
        String desc = "navigator.apeira." + key + ".desc";
        if (net.minecraft.locale.Language.getInstance().has(desc)) {
            lines.add(grey(Component.translatable(desc)));
        }
        return new Button(slots, lines, action);
    }

    /** A button for a feature Apeira does not have yet: it names the feature and says so, and does nothing. */
    private static Button unavailable(int[] slots, String key) {
        return new Button(slots, List.of(name(key), grey(Component.translatable("navigator.apeira.not_available"))), null);
    }

    private static Component name(String key) {
        return Component.translatable("navigator.apeira." + key).withColor(NavigatorService.LABEL);
    }

    private static Component grey(Component text) {
        return text.copy().withStyle(Style.EMPTY.withColor(NavigatorService.VALUE).withItalic(false));
    }

    private static void layer(List<Identifier> layers, String... paths) {
        for (String path : paths) {
            layers.add(ApeiraIds.id(ROOT + path + ".png"));
        }
    }

    private static int[] slots(int... slots) {
        return slots;
    }

    /** The slots of a {@code columns} by {@code rows} block whose top-left slot is {@code first}. */
    private static int[] block(int first, int columns, int rows) {
        return IntStream.range(0, columns * rows).map(i -> first + i / columns * 9 + i % columns).toArray();
    }
}
