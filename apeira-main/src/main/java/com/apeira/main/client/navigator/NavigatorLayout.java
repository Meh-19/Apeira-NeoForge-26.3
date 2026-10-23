package com.apeira.main.client.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorActionPayload.Action;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.navigator.NavigatorView;
import com.apeira.main.realm.RealmRole;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

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
    /** The party roster's cells: two rows of seven. */
    private static final int[] PARTY_SLOTS = IntStream.concat(IntStream.rangeClosed(19, 25), IntStream.rangeClosed(28, 34)).toArray();
    /** Origin Realms' title colours on profiles: white for a button, dark grey for an empty cosmetic cell. */
    private static final int ACTIVE = 0xFFFFFF;
    private static final int EMPTY_SLOT = 0x555555;
    /** Social's party row: its cells in {@code social/background} sit 3 pixels below slots 12-15, so the heads do too. */
    private static final int SOCIAL_PARTY_DY = 3;
    /** A profile's cosmetic slots down its left edge: hat, back, prop (the hand slot) and balloon, in {@link CosmeticSlot} order. */
    static final int[] COSMETIC_SLOTS = {18, 27, 36, 45};
    static final String[] COSMETIC_NAMES = {"hat", "back", "prop", "balloon"};
    /** The status light in the model panel's bottom-right corner. */
    static final int STATUS_SLOT = 49;
    /** The Profile Info icon's slot, in the model panel's top right corner: the player's stat panel. */
    static final int INFO_SLOT = 22;

    private NavigatorLayout() {}

    /**
     * A clickable or hoverable area of a page.
     *
     * @param slots   the chest slots it covers
     * @param tooltip its tooltip, first line the name
     * @param action  what clicking it asks the server to do; null for areas that only explain themselves
     * @param lit     whether hovering it lights its face; the profile's model panel only shows its tooltip
     */
    record Button(int[] slots, List<Component> tooltip, @Nullable NavigatorActionPayload action, boolean lit) {
        Button(int[] slots, List<Component> tooltip, @Nullable NavigatorActionPayload action) {
            this(slots, tooltip, action, true);
        }
    }

    /**
     * A player head drawn in {@code slot}, outside the page's list: a party member or invite; {@code dim} for an invite.
     * {@code dy} moves it down to its drawn cell where the texture's cell is not on the slot grid.
     */
    record Head(int slot, UUID id, ResolvableProfile skin, boolean dim, int dy) {}

    /**
     * @param layers      textures drawn in order
     * @param buttons     checked before the entries, in order
     * @param entrySlots  the slots the view's entries are drawn in as heads, in order; empty when it shows none
     * @param pageNumbers whether the page number is drawn between the page arrows
     * @param heads       further heads to draw (party rosters), whose buttons are among {@code buttons}
     * @param picked      a slot drawn picked (the leader's pick on the roster), or -1
     * @param profile     whether the page is a profile, drawn with the player's model, cosmetic slots and status
     */
    record Page(List<Identifier> layers, List<Button> buttons, int[] entrySlots, boolean pageNumbers, List<Head> heads, int picked,
            boolean profile) {
        Page(List<Identifier> layers, List<Button> buttons, int[] entrySlots, boolean pageNumbers) {
            this(layers, buttons, entrySlots, pageNumbers, List.of(), -1, false);
        }
    }

    /** {@code self} is the viewing player, left out of Social's party row. */
    static Page of(NavigatorView view, UUID self) {
        List<Identifier> layers = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();
        List<Head> heads = new ArrayList<>();
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
                buttons.add(button(slots(7), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                for (int i = 0; i < colours.length; i++) {
                    layer(layers, "profile/open_worlds/" + colours[i] + "_button");
                    buttons.add(button(slots[i], "open_world." + colours[i], new NavigatorActionPayload(Action.OPEN_WORLD, i)));
                }
            }
            case HOMES -> {
                layer(layers, "navigation", "profile/navigation_profile", "profile/background", "profile/homes/background");
                buttons.add(button(slots(15), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                // Homes are only set by command: a free cell of the grid says how ("CMD: /sethome [name]"), as on the server.
                int[] free = IntStream.range(view.entries().size(), HOME_SLOTS.length).map(i -> HOME_SLOTS[i]).toArray();
                cells(free, hint("hint.sethome"), buttons);
                return new Page(List.copyOf(layers), List.copyOf(buttons), HOME_SLOTS, false);
            }
            case SOCIAL, EDIT_PARTY, EDIT_PARTY_MEMBER -> socialPage(view, self, layers, buttons, heads);
            case PROFILE_OTHER -> {
                profilePage(view, layers, buttons);
                return new Page(List.copyOf(layers), List.copyOf(buttons), new int[0], false, List.of(), -1, true);
            }
            case PROFILE -> {
                ownProfilePage(view, layers, buttons);
                return new Page(List.copyOf(layers), List.copyOf(buttons), new int[0], false, List.of(), -1, true);
            }
            case PARTY, PARTY_MEMBER -> {
                partyPage(view, layers, buttons, heads);
                int picked = -1;
                if (page == NavigatorPage.PARTY_MEMBER && view.target().isPresent()) {
                    picked = heads.stream().filter(head -> head.id().equals(view.target().get().id())).mapToInt(Head::slot).findFirst().orElse(-1);
                }
                return new Page(List.copyOf(layers), List.copyOf(buttons), new int[0], false, List.copyOf(heads), picked, false);
            }
            default -> realmPage(view, layers, buttons);
        }
        boolean showsList = switch (page) {
            case REALMS, REALM_IN_REALM, REALM_MEMBERS, REALM_BANNED, SOCIAL -> true;
            default -> false;
        };
        return new Page(List.copyOf(layers), List.copyOf(buttons), showsList ? LIST_SLOTS : new int[0], showsList, List.copyOf(heads), -1, false);
    }

    /**
     * Social: the party row (the Party button, lit in a party, and up to four other members) over the friends list with
     * requests, filtered All, Online, Offline or Requests; and its party popups. The Requests filter has no overlay texture.
     */
    private static void socialPage(NavigatorView view, UUID self, List<Identifier> layers, List<Button> buttons, List<Head> heads) {
        layer(layers, "navigation", "social/navigation_social", "social/background");
        boolean inParty = view.has(NavigatorView.IN_PARTY);
        if (inParty) {
            layer(layers, "social/party_on");
        }
        listControls(view, layers);
        String[] filters = {"all", "online", "offline"};
        if (view.filter() < filters.length) {
            layer(layers, "social/filter_" + filters[view.filter()]);
        }
        layer(layers, view.sort() == 0 ? "social/sort_online" : "social/sort_alpha_asc");
        // The party popups come first so they take their slots from the page under them.
        switch (view.page()) {
            case EDIT_PARTY -> {
                layer(layers, "social/edit_party");
                boolean chat = view.has(NavigatorView.PARTY_CHAT);
                if (chat) {
                    layer(layers, "social/party_chat");
                }
                buttons.add(button(slots(15), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                buttons.add(button(slots(21), "party_chat.info", null));
                buttons.add(new Button(slots(22, 23), options("party_chat", chat ? 0 : 1, "party", "global"), NavigatorActionPayload.of(Action.PARTY_CHAT)));
                buttons.add(unavailable(slots(30, 31), "party_warp"));
                boolean leader = view.has(NavigatorView.PARTY_LEADER);
                buttons.add(button(slots(32), leader ? "party_disband" : "party_leave",
                        NavigatorActionPayload.of(leader ? Action.PARTY_DISBAND : Action.PARTY_LEAVE)));
                return;
            }
            case EDIT_PARTY_MEMBER -> {
                layer(layers, "social/edit_member");
                buttons.add(button(slots(24), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
                buttons.add(button(slots(30), "party_transfer", NavigatorActionPayload.of(Action.PARTY_TRANSFER)));
                view.target().ifPresent(target -> {
                    heads.add(new Head(31, target.id(), target.skin(), false, 0));
                    buttons.add(new Button(slots(31), entryTooltip(target), null));
                });
                buttons.add(button(slots(32), "party_kick", NavigatorActionPayload.of(Action.PARTY_KICK)));
                return;
            }
            default -> {}
        }
        navigationBar(NavigatorPage.SOCIAL, true, buttons);
        buttons.add(button(slots(9, 10, 11), inParty ? "party.manage" : "party.create", NavigatorActionPayload.of(Action.PARTY)));
        // The other members, in roster order; the free cells say how to invite.
        int[] row = slots(12, 13, 14, 15);
        List<NavigatorView.Entry> others = view.party().subList(0, view.partySize()).stream().filter(entry -> !entry.id().equals(self)).toList();
        int shown = Math.min(row.length, others.size());
        for (int i = 0; i < shown; i++) {
            partyHead(others.get(i), row[i], false, SOCIAL_PARTY_DY, heads, buttons);
        }
        cells(Arrays.copyOfRange(row, shown, row.length), hint("hint.party"), buttons);
        buttons.add(button(slots(17), "learn_social", null));
        buttons.add(button(slots(45, 46), "previous_page", new NavigatorActionPayload(Action.PAGE, -1)));
        buttons.add(button(slots(50, 51), "next_page", new NavigatorActionPayload(Action.PAGE, 1)));
        buttons.add(new Button(slots(52), options("filter", view.filter(), "all", "online", "offline", "requests"), NavigatorActionPayload.of(Action.FILTER)));
        buttons.add(new Button(slots(53), options("sort", view.sort(), "last_online", "alpha_asc"), NavigatorActionPayload.of(Action.SORT)));
        emptyHint(view, buttons, "hint.friend");
    }

    /**
     * The party roster (Origin Realms' party menu): members in the two rows of seven, the leader first in its purple
     * frame, then open invites; chat, leave and help along the top, and for the leader hand-over, kick (lit once a member
     * is picked) and disband.
     */
    private static void partyPage(NavigatorView view, List<Identifier> layers, List<Button> buttons, List<Head> heads) {
        layer(layers, "party/menu");
        boolean inParty = view.has(NavigatorView.IN_PARTY);
        boolean leader = view.has(NavigatorView.PARTY_LEADER);
        boolean picked = view.page() == NavigatorPage.PARTY_MEMBER;
        if (inParty) {
            layer(layers, "party/party_leader");
        }
        if (view.has(NavigatorView.PARTY_CHAT)) {
            layer(layers, "party/party_chat_on");
        }
        if (leader) {
            layer(layers, "party/party_leader_options");
            if (picked) {
                layer(layers, "party/party_leader_selected");
            }
        }
        buttons.add(button(slots(0), "close", NavigatorActionPayload.of(Action.CLOSE_POPUP)));
        if (leader) {
            buttons.add(button(slots(1), picked ? "party_transfer" : "party_transfer.pick", picked ? NavigatorActionPayload.of(Action.PARTY_TRANSFER) : null));
            buttons.add(button(slots(2), picked ? "party_kick" : "party_kick.pick", picked ? NavigatorActionPayload.of(Action.PARTY_KICK) : null));
            buttons.add(button(slots(3), "party_disband", NavigatorActionPayload.of(Action.PARTY_DISBAND)));
        }
        if (inParty) {
            boolean chat = view.has(NavigatorView.PARTY_CHAT);
            buttons.add(new Button(slots(5), options("party_chat", chat ? 0 : 1, "party", "global"), NavigatorActionPayload.of(Action.PARTY_CHAT)));
            buttons.add(unavailable(slots(6), "party_warp"));
            buttons.add(button(slots(7), "party_leave", NavigatorActionPayload.of(Action.PARTY_LEAVE)));
        }
        buttons.add(button(slots(8), "learn_party", null));
        int[] cells = PARTY_SLOTS;
        int shown = Math.min(cells.length, view.party().size());
        for (int i = 0; i < shown; i++) {
            partyHead(view.party().get(i), cells[i], i >= view.partySize(), 0, heads, buttons);
        }
        cells(Arrays.copyOfRange(cells, shown, cells.length), hint("hint.party"), buttons);
    }

    /** A party member's or invite's head in {@code slot}: its tooltip, and a click sends it to the server. */
    private static void partyHead(NavigatorView.Entry entry, int slot, boolean invite, int dy, List<Head> heads, List<Button> buttons) {
        heads.add(new Head(slot, entry.id(), entry.skin(), invite, dy));
        buttons.add(new Button(slots(slot), entryTooltip(entry), new NavigatorActionPayload(Action.PARTY_ENTRY, 0, Optional.of(entry.id()), false, "")));
    }

    /**
     * The player's own profile (Origin Realms' "Your Profile"), with the captured titles and lore: cosmetic cells that
     * open the wardrobe, featured badges, the stat panel and status in the model panel, Teleport to your Realm, Open
     * World, Homes, Teleport to Realm Spawn (Origin Realms' Teleport to Origin Isles: the viewed player's realm spawn,
     * which on one's own profile is one's own), Journal, Gravestones and Player settings. Badges, the journal,
     * gravestones and settings do nothing yet.
     */
    private static void ownProfilePage(NavigatorView view, List<Identifier> layers, List<Button> buttons) {
        layer(layers, "navigation", "navigation_back_enabled", "profile/navigation_profile", "profile/background");
        navigationBar(NavigatorPage.PROFILE, true, buttons);
        profileCommon(view, buttons, true);
        buttons.add(titled(slots(19, 20, 21), "featured_badge", ACTIVE, null));
        buttons.add(titled(slots(23, 24, 32, 33), "realm_teleport.own", ACTIVE, NavigatorActionPayload.of(Action.REALM_TELEPORT)));
        buttons.add(new Button(slots(25, 26, 34, 35), List.of(title("open_worlds", ACTIVE), NavigatorService.action(0, "open_world_teleport"),
                NavigatorService.action(1, "open_world_list")), NavigatorActionPayload.open(NavigatorPage.OPEN_WORLDS)));
        buttons.add(titled(slots(41, 42), "homes", ACTIVE, NavigatorActionPayload.of(Action.HOMES)));
        buttons.add(titled(slots(43, 44), "realm_spawn", ACTIVE, NavigatorActionPayload.of(Action.VISIT)));
        buttons.add(titled(slots(50, 51), "journal", NavigatorService.LABEL, null));
        buttons.add(titled(slots(52), "gravestones", NavigatorService.LABEL, null));
        buttons.add(titled(slots(53), "player_settings", NavigatorService.LABEL, null));
    }

    /**
     * What both profiles share: the cosmetic cells ("Equip a prop cosmetic in the / wardrobe at Cosmo's Cosmetics" on
     * one's own, "Not holding prop" on another's, or the worn item's name), the player's name over the model panel, the
     * stat panel on the info icon and the status.
     */
    private static void profileCommon(NavigatorView view, List<Button> buttons, boolean own) {
        CosmeticSlot[] worn = CosmeticSlot.values();
        for (int i = 0; i < COSMETIC_SLOTS.length; i++) {
            ItemStack stack = i < worn.length ? view.cosmetics().get(worn[i]) : ItemStack.EMPTY;
            List<Component> lines = new ArrayList<>();
            if (!stack.isEmpty()) {
                lines.add(stack.getHoverName());
            } else if (own) {
                lines.add(title("cosmetic_slot.equip." + COSMETIC_NAMES[i], EMPTY_SLOT));
                lines.add(title("cosmetic_slot.equip.where", EMPTY_SLOT));
            } else {
                lines.add(title("cosmetic_slot.none." + COSMETIC_NAMES[i], EMPTY_SLOT));
            }
            buttons.add(new Button(slots(COSMETIC_SLOTS[i]), lines, own && i < worn.length ? NavigatorActionPayload.of(Action.WARDROBE) : null));
        }
        view.target().ifPresent(target -> {
            // Origin Realms names the player on every cell of the model panel that holds nothing else. The panel is not
            // lit on hover (no white box over the model); its tooltip still shows.
            int[] panel = own ? slots(28, 29, 30, 31, 37, 38, 39, 40, 46, 47, 48) : slots(19, 20, 21, 28, 29, 30, 31, 37, 38, 39, 40, 46, 47, 48);
            buttons.add(new Button(panel, List.of(Component.literal(target.name()).withStyle(Style.EMPTY.withColor(ACTIVE).withItalic(false))), null,
                    false));
            buttons.add(new Button(slots(INFO_SLOT), target.lines(), null));
            boolean online = view.has(NavigatorView.TARGET_ONLINE);
            String status = view.has(NavigatorView.TARGET_AFK) ? "status.afk" : online ? "status.online" : "status.offline";
            buttons.add(titled(slots(STATUS_SLOT), status, online ? NavigatorService.ACTION : NavigatorService.LABEL, null));
        });
    }

    /**
     * Another player's profile (Origin Realms' {@code profile_other}): realm invite or removal, the friend button
     * (send, accept or decline a request, or unfriend; red when they are a friend) and a visit to their realm. Party
     * and trade invites, cosmetics and auctions are not available yet.
     */
    private static void profilePage(NavigatorView view, List<Identifier> layers, List<Button> buttons) {
        layer(layers, "profile/profile_other/background");
        boolean partied = view.has(NavigatorView.TARGET_IN_PARTY) || view.has(NavigatorView.TARGET_PARTY_INVITED);
        if (partied) {
            layer(layers, "profile/profile_other/party_invite");
        }
        boolean member = view.has(NavigatorView.TARGET_MEMBER);
        boolean friend = view.has(NavigatorView.TARGET_FRIEND);
        if (friend) {
            layer(layers, "profile/profile_other/remove_friend");
        }
        if (member) {
            layer(layers, "profile/profile_other/remove_realm");
        }
        profileCommon(view, buttons, false);
        // The texture's buttons: Party Invite and Trade Invite across slots 23-26 and 32-35, then two rows of pairs.
        buttons.add(partied ? button(slots(23, 24, 25, 26), view.has(NavigatorView.TARGET_IN_PARTY) ? "party_invite.member" : "party_invite.invited", null)
                : button(slots(23, 24, 25, 26), "party_invite", NavigatorActionPayload.of(Action.PARTY_INVITE)));
        buttons.add(titled(slots(32, 33, 34, 35), "trade_invite", ACTIVE, null));
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
        buttons.add(titled(slots(50, 51), "view_auctions", NavigatorService.LABEL, null));
        buttons.add(titled(slots(52, 53), "teleport_realm", ACTIVE, NavigatorActionPayload.of(Action.VISIT)));
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
                    || page == NavigatorPage.PROFILE ? NavigatorPage.MAIN : NavigatorPage.REALMS)));
        }
        String[] bar = {"events", "profile", "badges", "realms_tab", "social", "auction_house"};
        for (int i = 0; i < bar.length; i++) {
            buttons.add(switch (i) {
                case 1 -> button(slots(2), "profile", NavigatorActionPayload.open(NavigatorPage.PROFILE));
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
        cells(empty, hint(hint), buttons);
    }

    /**
     * One button per slot, all with the same tooltip: cells drawn with the same icon are still separate buttons, so
     * each lights up and is outlined on its own.
     */
    private static void cells(int[] slots, List<Component> tooltip, List<Button> buttons) {
        for (int slot : slots) {
            buttons.add(new Button(slots(slot), tooltip, null));
        }
    }

    /** "CMD: /friend add [name]", the tooltip of an empty cell. */
    private static List<Component> hint(String hint) {
        return List.of(Component.translatable("navigator.apeira.hint.cmd").withStyle(Style.EMPTY.withColor(NavigatorService.LABEL))
                .append(Component.translatable("navigator.apeira." + hint).withColor(NavigatorService.VALUE)));
    }

    /** A head's tooltip: the name, then the server's lines. */
    static List<Component> entryTooltip(NavigatorView.Entry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.name()).withColor(NavigatorService.LABEL));
        lines.addAll(entry.lines());
        return lines;
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

    /** A button as Origin Realms' profiles title theirs: the title alone, in {@code color}. */
    private static Button titled(int[] slots, String key, int color, @Nullable NavigatorActionPayload action) {
        return new Button(slots, List.of(title(key, color)), action);
    }

    private static Component title(String key, int color) {
        return Component.translatable("navigator.apeira." + key).withStyle(Style.EMPTY.withColor(color).withItalic(false));
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
