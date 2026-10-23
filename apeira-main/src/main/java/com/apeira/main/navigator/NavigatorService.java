package com.apeira.main.navigator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.cosmetic.Wardrobe;
import com.apeira.decor.registry.DecorAttachments;
import com.apeira.main.home.HomeData;
import com.apeira.main.home.HomeService;
import com.apeira.main.navigator.NavigatorActionPayload.Action;
import com.apeira.main.party.Party;
import com.apeira.main.party.PartyService;
import com.apeira.main.realm.DummyPlayers;
import com.apeira.main.realm.RealmAccess;
import com.apeira.main.realm.RealmData;
import com.apeira.main.realm.RealmPool;
import com.apeira.main.realm.RealmPreset;
import com.apeira.main.realm.RealmRole;
import com.apeira.main.realm.RealmService;
import com.apeira.main.realm.VisitingRule;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The server side of the navigator. It keeps each player's place in the menu (page, list page, filter, sort, the
 * player a popup is about, the region picked for a reset), applies their clicks through {@link RealmService}, which
 * checks every permission, and sends back the page as a {@link NavigatorView}. The client sends only which button was
 * clicked; lists, roles and settings always come from the server's realm data.
 */
public final class NavigatorService {
    /** Origin Realms' tooltip colours: labels and names, values, and the green action line. */
    public static final int LABEL = 0xA7AEBA;
    public static final int VALUE = 0xFFFFFF;
    public static final int ACTION = 0x85CC16;
    /** How long a player does nothing (no movement, chat or clicks) before their profile shows them AFK. */
    public static final long AFK_MS = 5 * 60_000;
    /** The mouse-button glyphs in front of an action line (left, right, middle click). */
    private static final FontDescription KEYS = new FontDescription.Resource(ApeiraIds.id("tooltip_keys"));
    /** "September 05, 2022", the Created line of a home. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH);
    /** Origin Realms' profile join date: "November 21, 2021 (06:32 PM)". */
    private static final DateTimeFormatter JOIN_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy (hh:mm a)", Locale.ENGLISH);

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** When each online player entered the level they are in, for "Joined" in the In Realm list. */
    private static final Map<UUID, Long> ARRIVALS = new HashMap<>();

    private NavigatorService() {}

    private static final class Session {
        NavigatorPage page = NavigatorPage.MAIN;
        int pageIndex;
        int filter;
        int sort;
        @Nullable UUID target;
        int preset = -1;
        /** Opened straight on Realms home by {@code /realms}: its Back button is greyed out and does nothing. */
        boolean realmsRoot;
        /** Where closing another player's profile goes: Social, the party roster, or {@link NavigatorPage#CLOSED} when it was opened from the world. */
        NavigatorPage profileReturn = NavigatorPage.SOCIAL;

        void go(NavigatorPage to) {
            if (listPage(to) != listPage(page)) {
                pageIndex = 0;
                filter = 0;
                sort = defaultSort(listPage(to));
            }
            page = to;
        }
    }

    /** Opens the navigator on {@code page} for {@code player}, as the {@code /navigator} and {@code /realm} commands do. */
    public static void open(ServerPlayer player, NavigatorPage page) {
        open(player, page, false);
    }

    /** Opens Realms home as the navigator's root, as {@code /realms} does: there is no Back to the root menu. */
    public static void openRealms(ServerPlayer player) {
        open(player, NavigatorPage.REALMS, true);
    }

    /**
     * Opens a profile from the world, as {@code /profile}, the profile key and a Shift-right-click on a player do: the
     * viewer's own when {@code target} is null or the viewer, else {@code target}'s. Closing it closes the navigator.
     */
    public static void openProfile(ServerPlayer player, @Nullable UUID target) {
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), uuid -> new Session());
        session.realmsRoot = false;
        if (target == null || target.equals(player.getUUID())) {
            session.go(NavigatorPage.PROFILE);
        } else {
            session.target = target;
            session.profileReturn = NavigatorPage.CLOSED;
            session.go(NavigatorPage.PROFILE_OTHER);
        }
        send(player, session);
    }

    private static void open(ServerPlayer player, NavigatorPage page, boolean realmsRoot) {
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), uuid -> new Session());
        if (needsRealm(page) && !hasRealm(player)) {
            RealmService.fail(player, "realm.none");
            page = NavigatorPage.MAIN;
            realmsRoot = false;
        }
        session.realmsRoot = realmsRoot;
        session.go(page);
        send(player, session);
    }

    /**
     * Applies a click in the navigator. The menu's next view shows what changed, so success lines are left out of chat
     * ({@link RealmService#quietly}); errors are still written, in red.
     */
    public static void handle(ServerPlayer player, NavigatorActionPayload payload) {
        RealmService.quietly(player, () -> apply(player, payload));
    }

    private static void apply(ServerPlayer player, NavigatorActionPayload payload) {
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), uuid -> new Session());
        NavigatorPage page = session.page;
        int argument = payload.argument();
        switch (payload.action()) {
            case OPEN_PAGE -> {
                NavigatorPage[] pages = NavigatorPage.values();
                NavigatorPage to = argument >= 0 && argument < pages.length ? pages[argument] : NavigatorPage.MAIN;
                if (session.realmsRoot && page == NavigatorPage.REALMS && to == NavigatorPage.MAIN) {
                    // Back is disabled on a Realms home opened by /realms.
                } else if (isTab(to)) {
                    if (needsRealm(to) && !hasRealm(player)) {
                        RealmService.fail(player, "realm.none");
                    } else {
                        session.go(to);
                    }
                }
            }
            case PAGE -> session.pageIndex += Integer.signum(argument);
            case FILTER -> {
                session.filter = (session.filter + 1) % filters(listPage(page)).length;
                session.pageIndex = 0;
            }
            case SORT -> {
                session.sort = (session.sort + 1) % sorts(listPage(page)).length;
                session.pageIndex = 0;
            }
            case SPAWN, REALM_TELEPORT -> {
                if (!RealmPool.sendHome(player)) {
                    if (payload.action() == Action.SPAWN) {
                        player.teleport(player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
                        // An arrival line, as "Welcome home!" is: the menu closes, so it is not redundant.
                        player.sendSystemMessage(Component.translatable("message.apeira.navigator.spawn").withStyle(ChatFormatting.GREEN));
                    } else {
                        RealmService.fail(player, "realm.none");
                    }
                }
                close(player);
                return;
            }
            case HOMES -> session.go(NavigatorPage.HOMES);
            case OPEN_WORLD -> unavailable(player, "open_world");
            case ENTRY -> {
                if (payload.target().isPresent() && entry(player, session, payload.target().get(), argument, payload.secondary())) {
                    return;
                }
            }
            case CLOSE_POPUP -> {
                if (page == NavigatorPage.PARTY || page == NavigatorPage.PROFILE_OTHER && session.profileReturn == NavigatorPage.CLOSED) {
                    // The roster's close button, and a profile opened from the world, have no page under them.
                    close(player);
                    return;
                }
                session.page = closePopup(session);
            }
            case KICK -> {
                if (page == NavigatorPage.EDIT_PLAYER && session.target != null && RealmService.kick(player, session.target)) {
                    session.page = NavigatorPage.REALM_IN_REALM;
                }
            }
            case BAN -> {
                if ((page == NavigatorPage.EDIT_PLAYER || page == NavigatorPage.EDIT_MEMBER) && session.target != null
                        && RealmService.ban(player, session.target)) {
                    session.page = page == NavigatorPage.EDIT_PLAYER ? NavigatorPage.REALM_IN_REALM : NavigatorPage.REALM_MEMBERS;
                }
            }
            case INVITE -> {
                if ((page == NavigatorPage.EDIT_PLAYER || page == NavigatorPage.PROFILE_OTHER) && session.target != null) {
                    RealmService.invite(player, session.target);
                }
            }
            case SET_ROLE -> {
                RealmRole[] roles = RealmRole.values();
                if (page == NavigatorPage.EDIT_MEMBER && session.target != null && argument >= 0 && argument < roles.length) {
                    RealmService.setRole(player, session.target, roles[argument]);
                }
            }
            case REMOVE_MEMBER -> {
                if (page == NavigatorPage.EDIT_MEMBER && session.target != null && RealmService.removeMember(player, session.target)) {
                    session.page = NavigatorPage.REALM_MEMBERS;
                } else if (page == NavigatorPage.PROFILE_OTHER && session.target != null) {
                    RealmService.removeMember(player, session.target);
                }
            }
            case VISITING_RULE -> {
                if (page.isSettings()) {
                    RealmService.setVisitingRule(player, VisitingRule.byId(argument));
                }
            }
            case TOGGLE_CHAT -> {
                if (page.isSettings()) {
                    RealmService.togglePrivateChat(player);
                }
            }
            case TOGGLE_ACCESS -> {
                if (page.isSettings()) {
                    RealmService.toggleAlwaysAccessible(player);
                }
            }
            case SET_SPAWN -> {
                if (page.isSettings()) {
                    RealmService.setSpawn(player);
                }
            }
            case RESET_DIMENSION -> resetDimension(player, session, argument);
            case RESET_PRESET -> {
                RealmPreset[] presets = RealmPreset.values();
                if (page == NavigatorPage.REALM_RESET_PRESET && argument >= 0 && argument < presets.length) {
                    session.preset = argument;
                    session.page = NavigatorPage.REALM_RESET_CONFIRM;
                }
            }
            case RESET_CONFIRM -> {
                if (page == NavigatorPage.REALM_RESET_CONFIRM && session.preset >= 0
                        && RealmService.resetOverworld(player, RealmPreset.values()[session.preset])) {
                    close(player);
                    return;
                }
            }
            case SET_HOME -> {
                if (page == NavigatorPage.HOMES) {
                    HomeService.setNext(player);
                }
            }
            case RENAME_HOME -> {
                if (page == NavigatorPage.HOMES) {
                    HomeService.rename(player, argument, payload.text());
                }
            }
            case FRIEND -> {
                if (page == NavigatorPage.PROFILE_OTHER && session.target != null) {
                    friendAction(player, session.target, payload.secondary(), false);
                }
            }
            case VISIT -> {
                // The viewed player's realm spawn: Teleport to their Realm on another's profile, Teleport to Realm Spawn on
                // one's own. The owner is the server's own session target (or the player), never one the client names,
                // and RealmService#visit checks the visiting rules.
                UUID owner = page == NavigatorPage.PROFILE ? player.getUUID() : page == NavigatorPage.PROFILE_OTHER ? session.target : null;
                if (owner != null && RealmService.visit(player, owner)) {
                    close(player);
                    return;
                }
            }
            case PARTY -> {
                if (page == NavigatorPage.SOCIAL) {
                    if (PartyService.partyOf(player.getUUID()).isPresent()) {
                        session.page = NavigatorPage.EDIT_PARTY;
                    } else {
                        PartyService.create(player);
                    }
                }
            }
            case PARTY_ENTRY -> payload.target().ifPresent(target -> partyEntry(player, session, target));
            case PARTY_INVITE -> {
                if (page == NavigatorPage.PROFILE_OTHER && session.target != null) {
                    PartyService.invite(player, session.target);
                }
            }
            case PARTY_CHAT -> {
                if (page.isParty()) {
                    PartyService.toggleChat(player);
                }
            }
            case PARTY_LEAVE -> {
                if (page.isParty()) {
                    PartyService.leave(player);
                }
            }
            case PARTY_DISBAND -> {
                if (page.isParty()) {
                    PartyService.disband(player);
                }
            }
            case PARTY_KICK, PARTY_TRANSFER -> {
                if ((page == NavigatorPage.PARTY_MEMBER || page == NavigatorPage.EDIT_PARTY_MEMBER) && session.target != null) {
                    if (payload.action() == Action.PARTY_KICK) {
                        PartyService.kick(player, session.target);
                    } else {
                        PartyService.transfer(player, session.target);
                    }
                    session.page = page == NavigatorPage.PARTY_MEMBER ? NavigatorPage.PARTY : NavigatorPage.SOCIAL;
                }
            }
            case WARDROBE -> {
                // The client opens the wardrobe itself, through decor's payload.
            }
        }
        send(player, session);
    }

    /**
     * A head clicked in a list, {@code argument} being its place in the shown list times two, plus one with Shift.
     * Returns true when the navigator was closed (the player travelled).
     */
    private static boolean entry(ServerPlayer player, Session session, UUID target, int argument, boolean secondary) {
        boolean shift = (argument & 1) != 0;
        MinecraftServer server = player.level().getServer();
        RealmData data = RealmService.data(server);
        switch (session.page) {
            case REALMS -> {
                if (RealmService.visit(player, target)) {
                    close(player);
                    return true;
                }
            }
            case REALM_IN_REALM -> {
                if (inRealm(player).contains(target)) {
                    session.target = target;
                    session.page = NavigatorPage.EDIT_PLAYER;
                }
            }
            case REALM_MEMBERS -> {
                if (hasRealm(player) && data.access(player.getUUID()).members().containsKey(target)) {
                    session.target = target;
                    session.page = NavigatorPage.EDIT_MEMBER;
                }
            }
            case REALM_BANNED -> RealmService.unban(player, target);
            case HOMES -> {
                // Homes are picked by their place in the grid, which is the order they were set in.
                int index = argument >> 1;
                if (secondary) {
                    HomeService.delete(player, index);
                } else if (HomeService.teleport(player, index)) {
                    close(player);
                    return true;
                }
            }
            case SOCIAL -> {
                UUID self = player.getUUID();
                if (data.areFriends(self, target) || data.hasFriendRequest(target, self) || data.hasFriendRequest(self, target)) {
                    if (shift || secondary || data.hasFriendRequest(target, self)) {
                        friendAction(player, target, secondary, true);
                    } else {
                        session.target = target;
                        session.profileReturn = NavigatorPage.SOCIAL;
                        session.page = NavigatorPage.PROFILE_OTHER;
                    }
                }
            }
            default -> {}
        }
        return false;
    }

    /** The page under a popup, which closing it goes back to. */
    private static NavigatorPage closePopup(Session session) {
        return switch (session.page) {
            case HOMES, OPEN_WORLDS -> NavigatorPage.PROFILE;
            case PROFILE_OTHER -> session.profileReturn;
            case PARTY_MEMBER -> NavigatorPage.PARTY;
            case EDIT_PARTY, EDIT_PARTY_MEMBER -> NavigatorPage.SOCIAL;
            case EDIT_PLAYER -> NavigatorPage.REALM_IN_REALM;
            case EDIT_MEMBER -> NavigatorPage.REALM_MEMBERS;
            case REALM_RESET, REALM_RESET_PRESET, REALM_RESET_CONFIRM -> NavigatorPage.REALM_SETTINGS;
            default -> session.page;
        };
    }

    /**
     * A party head clicked on Social's party row or the roster. The leader picks a member (on the roster, the picked one
     * again drops the pick) or takes back an invite; anyone else's click on a member opens that member's profile.
     */
    private static void partyEntry(ServerPlayer player, Session session, UUID target) {
        NavigatorPage page = session.page;
        Optional<Party> found = PartyService.partyOf(player.getUUID());
        if (found.isEmpty() || !(page == NavigatorPage.SOCIAL || page == NavigatorPage.PARTY || page == NavigatorPage.PARTY_MEMBER)
                || target.equals(player.getUUID())) {
            return;
        }
        Party party = found.get();
        boolean leader = party.isLeader(player.getUUID());
        if (party.members().contains(target)) {
            if (leader && page == NavigatorPage.PARTY_MEMBER && target.equals(session.target)) {
                session.page = NavigatorPage.PARTY;
            } else if (leader) {
                session.target = target;
                session.page = page == NavigatorPage.SOCIAL ? NavigatorPage.EDIT_PARTY_MEMBER : NavigatorPage.PARTY_MEMBER;
            } else {
                session.target = target;
                session.profileReturn = page == NavigatorPage.SOCIAL ? NavigatorPage.SOCIAL : NavigatorPage.PARTY;
                session.page = NavigatorPage.PROFILE_OTHER;
            }
        } else if (leader && party.isInvited(target)) {
            PartyService.cancelInvite(player, target);
        }
    }

    /**
     * The friend button of a profile, or a click on the friends list: unfriend a friend (from the list only with Shift),
     * accept a request (decline it with a right-click), take back a sent request with a right-click, or send one.
     */
    private static void friendAction(ServerPlayer player, UUID target, boolean secondary, boolean fromList) {
        RealmData data = RealmService.data(player.level().getServer());
        UUID self = player.getUUID();
        if (data.areFriends(self, target)) {
            RealmService.removeFriend(player, target);
        } else if (data.hasFriendRequest(target, self)) {
            if (secondary) {
                RealmService.declineFriend(player, target);
            } else {
                RealmService.acceptFriend(player, target);
            }
        } else if (data.hasFriendRequest(self, target)) {
            if (secondary) {
                RealmService.cancelFriendRequest(player, target);
            }
        } else if (!fromList) {
            RealmService.requestFriend(player, target);
        }
    }

    private static void resetDimension(ServerPlayer player, Session session, int dimension) {
        if (session.page == NavigatorPage.REALM_SETTINGS) {
            session.page = NavigatorPage.REALM_RESET;
        } else if (session.page == NavigatorPage.REALM_RESET) {
            if (dimension != 0) {
                // Realm nethers and the Aether do not exist yet, so their buttons stay greyed out.
                RealmService.fail(player, "realm.reset.travel",
                        Component.translatable("navigator.apeira.dimension." + (dimension == 1 ? "nether" : "aether")));
            } else if (RealmService.canResetOverworld(player)) {
                session.page = NavigatorPage.REALM_RESET_PRESET;
            } else {
                RealmService.fail(player, "realm.reset.travel", Component.translatable("navigator.apeira.dimension.overworld"));
            }
        }
    }

    private static void send(ServerPlayer player, Session session) {
        ApeiraPackets.sendToPlayer(player, new OpenNavigatorPayload(view(player, session)));
    }

    private static void close(ServerPlayer player) {
        ApeiraPackets.sendToPlayer(player, new OpenNavigatorPayload(NavigatorView.closed()));
    }

    private static void unavailable(ServerPlayer player, String feature) {
        RealmService.fail(player, "navigator.unavailable", Component.translatable("navigator.apeira." + feature));
    }

    /** Pages a client may ask for by name; popups and reset steps are reached only through their actions. */
    private static boolean isTab(NavigatorPage page) {
        return switch (page) {
            case MAIN, REALMS, REALM_IN_REALM, REALM_MEMBERS, REALM_BANNED, REALM_SETTINGS, OPEN_WORLDS, HOMES, SOCIAL, PROFILE, PARTY -> true;
            default -> false;
        };
    }

    private static boolean needsRealm(NavigatorPage page) {
        return page.isAccess() || page.isSettings();
    }

    private static boolean hasRealm(ServerPlayer player) {
        return RealmService.data(player.level().getServer()).realmOf(player.getUUID()).isPresent();
    }

    /** The page whose list a page shows: popups show their tab's list and Settings shows the realm list. */
    public static NavigatorPage listPage(NavigatorPage page) {
        return switch (page) {
            case EDIT_PLAYER -> NavigatorPage.REALM_IN_REALM;
            case EDIT_MEMBER -> NavigatorPage.REALM_MEMBERS;
            case PROFILE_OTHER, EDIT_PARTY, EDIT_PARTY_MEMBER -> NavigatorPage.SOCIAL;
            default -> page.isSettings() ? NavigatorPage.REALMS : page;
        };
    }

    private static String[] filters(NavigatorPage page) {
        return switch (page) {
            case REALMS -> new String[] {"all", "added_to", "friends"};
            case REALM_MEMBERS -> new String[] {"all", "resident", "trusted"};
            case SOCIAL -> new String[] {"all", "online", "offline", "requests"};
            default -> new String[] {"all"};
        };
    }

    private static String[] sorts(NavigatorPage page) {
        return switch (page) {
            case REALMS -> new String[] {"alpha_asc", "count_asc", "count_desc"};
            case REALM_IN_REALM -> new String[] {"alpha_asc", "rank"};
            case REALM_MEMBERS -> new String[] {"alpha_asc", "time_desc"};
            case REALM_BANNED -> new String[] {"time_asc", "time_desc"};
            case SOCIAL -> new String[] {"last_online", "alpha_asc"};
            default -> new String[] {"alpha_asc"};
        };
    }

    private static int defaultSort(NavigatorPage page) {
        return page == NavigatorPage.REALMS ? 2 : page == NavigatorPage.REALM_BANNED ? 1 : 0;
    }

    /** The players in {@code owner}'s realm right now, other than the owner, dummy players included. */
    private static List<UUID> inRealm(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        List<UUID> players = new ArrayList<>(RealmService.data(server).realmOf(owner.getUUID()).map(realm -> server.getLevel(realm.levelKey()))
                .map(level -> level.players().stream().filter(player -> player != owner).map(player -> player.getUUID()).toList())
                .orElse(List.of()));
        DummyPlayers.all().stream().filter(dummy -> DummyPlayers.realmOf(server, dummy).filter(owner.getUUID()::equals).isPresent())
                .forEach(dummy -> players.add(dummy.id()));
        return players;
    }

    private static NavigatorView view(ServerPlayer player, Session session) {
        MinecraftServer server = player.level().getServer();
        RealmData data = RealmService.data(server);
        UUID self = player.getUUID();
        boolean hasRealm = data.realmOf(self).isPresent();
        RealmAccess access = data.access(self);
        if (needsRealm(session.page) && !hasRealm) {
            session.go(NavigatorPage.MAIN);
        }
        Optional<Party> party = PartyService.partyOf(self);
        boolean leads = party.filter(theirs -> theirs.isLeader(self)).isPresent();
        // A party popup or pick is dropped once the party, the lead or the picked member is gone.
        boolean picked = session.target != null && party.filter(theirs -> theirs.members().contains(session.target)).isPresent();
        if (session.page == NavigatorPage.EDIT_PARTY && party.isEmpty() || session.page == NavigatorPage.EDIT_PARTY_MEMBER && !(leads && picked)) {
            session.page = NavigatorPage.SOCIAL;
        } else if (session.page == NavigatorPage.PARTY_MEMBER && !(leads && picked)) {
            session.page = NavigatorPage.PARTY;
        }
        NavigatorPage page = session.page;
        List<NavigatorView.Entry> all = entries(player, data, access, listPage(page), session.filter, session.sort);
        int pageCount = Math.max(1, (all.size() + NavigatorView.PAGE_SIZE - 1) / NavigatorView.PAGE_SIZE);
        session.pageIndex = Math.clamp(session.pageIndex, 0, pageCount - 1);
        List<NavigatorView.Entry> shown = all.subList(session.pageIndex * NavigatorView.PAGE_SIZE,
                Math.min(all.size(), (session.pageIndex + 1) * NavigatorView.PAGE_SIZE));

        Optional<NavigatorView.Entry> target = Optional.empty();
        Wardrobe cosmetics = Wardrobe.EMPTY;
        int flags = (hasRealm ? NavigatorView.HAS_REALM : 0) | (RealmService.canResetOverworld(player) ? NavigatorView.CAN_RESET_OVERWORLD : 0);
        if ((page == NavigatorPage.EDIT_PLAYER || page == NavigatorPage.EDIT_MEMBER) && session.target != null) {
            UUID id = session.target;
            RealmRole role = access.roleOf(self, id);
            List<Component> lines = new ArrayList<>();
            RealmAccess.Member member = access.members().get(id);
            Long arrived = ARRIVALS.get(id);
            if (member != null) {
                lines.add(label("joined", Component.literal(duration(System.currentTimeMillis() - member.joined()))));
            } else if (arrived != null) {
                lines.add(label("joined", Component.literal(duration(System.currentTimeMillis() - arrived))));
            }
            lines.add(label("role", role(role)));
            target = Optional.of(new NavigatorView.Entry(id, data.nameOf(id), lines));
            flags |= (role.canBuild() ? NavigatorView.TARGET_MEMBER : 0) | (access.isInvited(id) ? NavigatorView.TARGET_INVITED : 0)
                    | (role == RealmRole.TRUSTED ? NavigatorView.TARGET_TRUSTED : 0);
        } else if (page.isProfile()) {
            UUID id = page == NavigatorPage.PROFILE || session.target == null ? self : session.target;
            target = Optional.of(new NavigatorView.Entry(id, data.nameOf(id), stats(server, data, id)));
            flags |= (access.roleOf(self, id).canBuild() ? NavigatorView.TARGET_MEMBER : 0) | (access.isInvited(id) ? NavigatorView.TARGET_INVITED : 0)
                    | (data.areFriends(self, id) ? NavigatorView.TARGET_FRIEND : 0)
                    | (data.hasFriendRequest(id, self) ? NavigatorView.TARGET_REQUESTED : 0)
                    | (data.hasFriendRequest(self, id) ? NavigatorView.TARGET_REQUEST_SENT : 0)
                    | (DummyPlayers.isOnline(server, id) ? NavigatorView.TARGET_ONLINE : 0)
                    | (isAfk(server, id) ? NavigatorView.TARGET_AFK : 0)
                    | (party.filter(theirs -> theirs.members().contains(id)).isPresent() ? NavigatorView.TARGET_IN_PARTY : 0)
                    | (party.filter(theirs -> theirs.isInvited(id)).isPresent() ? NavigatorView.TARGET_PARTY_INVITED : 0);
            ServerPlayer profiled = server.getPlayerList().getPlayer(id);
            cosmetics = profiled == null ? Wardrobe.EMPTY : profiled.getData(DecorAttachments.WARDROBE);
        } else if ((page == NavigatorPage.PARTY_MEMBER || page == NavigatorPage.EDIT_PARTY_MEMBER) && session.target != null) {
            target = Optional.of(new NavigatorView.Entry(session.target, data.nameOf(session.target), List.of(onlineLine(server, data, session.target,
                    System.currentTimeMillis()))));
        }
        List<NavigatorView.Entry> roster = new ArrayList<>();
        int partySize = 0;
        if (party.isPresent()) {
            flags |= NavigatorView.IN_PARTY | (leads ? NavigatorView.PARTY_LEADER : 0) | (PartyService.chatOn(self) ? NavigatorView.PARTY_CHAT : 0);
            roster = partyEntries(server, data, party.get(), self);
            partySize = party.get().members().size();
        }
        if (session.realmsRoot && page == NavigatorPage.REALMS) {
            flags |= NavigatorView.NO_BACK;
        }
        int setting = access.visitingRule().ordinal() | (access.privateChat() ? NavigatorView.PRIVATE_CHAT : 0)
                | (access.alwaysAccessible() ? NavigatorView.ALWAYS_ACCESSIBLE : 0);
        return new NavigatorView(page, session.pageIndex, pageCount, session.filter, session.sort, skinned(server, data, shown),
                target.map(entry -> skinned(server, data, entry)), flags, setting, page == NavigatorPage.REALM_RESET_CONFIRM ? session.preset : -1,
                skinned(server, data, roster), partySize, cosmetics);
    }

    private static List<NavigatorView.Entry> skinned(MinecraftServer server, RealmData data, List<NavigatorView.Entry> entries) {
        return entries.stream().map(entry -> skinned(server, data, entry)).toList();
    }

    /**
     * {@code entry} with the skin of the player whose head it is ({@link NavigatorView.Entry#id()}, which for a home is
     * its owner): an online player's own profile when it carries their skin, as the server got it from Mojang, else their
     * name, which the client looks up as a player head does (dummies' mannequins wear their name's skin the same way).
     * A bare UUID is the last resort: it only finds a skin for an online-mode account.
     */
    static NavigatorView.Entry skinned(MinecraftServer server, RealmData data, NavigatorView.Entry entry) {
        ServerPlayer online = server.getPlayerList().getPlayer(entry.id());
        if (online != null && !online.getGameProfile().properties().isEmpty()) {
            return entry.withSkin(ResolvableProfile.createResolved(online.getGameProfile()));
        }
        return data.known(entry.id()).map(known -> entry.withSkin(ResolvableProfile.createUnresolved(known.name()))).orElse(entry);
    }

    /** The party roster: members, the leader first, then open invites, with what clicking each does for {@code viewer}. */
    private static List<NavigatorView.Entry> partyEntries(MinecraftServer server, RealmData data, Party party, UUID viewer) {
        long now = System.currentTimeMillis();
        boolean leads = party.isLeader(viewer);
        List<NavigatorView.Entry> entries = new ArrayList<>();
        for (UUID member : party.members()) {
            List<Component> lines = new ArrayList<>();
            lines.add(label("party_role", Component.translatable(party.isLeader(member) ? "navigator.apeira.lore.party_leader"
                    : "navigator.apeira.lore.party_member")));
            lines.add(onlineLine(server, data, member, now));
            if (!member.equals(viewer)) {
                lines.add(Component.empty());
                lines.add(action(0, leads ? "edit_party_member" : "view_profile"));
            }
            entries.add(new NavigatorView.Entry(member, data.nameOf(member), lines));
        }
        for (UUID invited : PartyService.openInvites(party)) {
            List<Component> lines = new ArrayList<>();
            lines.add(label("party_role", Component.translatable("navigator.apeira.lore.party_invited")));
            if (leads) {
                lines.add(Component.empty());
                lines.add(action(0, "cancel_party_invite"));
            }
            entries.add(new NavigatorView.Entry(invited, data.nameOf(invited), lines));
        }
        return entries;
    }

    /**
     * The profile's stat panel, shown on its info icon as Origin Realms' is: "Playtime: ..." as its title, then the join
     * date. Origin Realms also lists rubies, Discord and the streak highscore, which Apeira does not have.
     */
    private static List<Component> stats(MinecraftServer server, RealmData data, UUID id) {
        List<Component> lines = new ArrayList<>();
        lines.add(label("playtime", Component.literal(duration(playTicks(server, id) * 50))));
        data.known(id).map(RealmData.KnownPlayer::firstSeen).filter(first -> first > 0).ifPresent(first -> lines.add(label("join_date",
                Component.literal(JOIN_DATE.format(Instant.ofEpochMilli(first).atZone(ZoneId.systemDefault()))))));
        return lines;
    }

    /** {@code id}'s play time in ticks: an online player's live statistic, else the one saved in their stats file, else 0. */
    private static long playTicks(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME);
        }
        Path file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id + ".json");
        return Files.isRegularFile(file) ? new ServerStatsCounter(server, file).getValue(Stats.CUSTOM, Stats.PLAY_TIME) : 0;
    }

    /** Whether {@code id} is online and has done nothing for {@link #AFK_MS}. Dummy players are never AFK. */
    public static boolean isAfk(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        return player != null && Util.getMillis() - player.getLastActionTime() >= AFK_MS;
    }

    private static List<NavigatorView.Entry> entries(ServerPlayer player, RealmData data, RealmAccess access, NavigatorPage page, int filter,
            int sort) {
        MinecraftServer server = player.level().getServer();
        UUID self = player.getUUID();
        long now = System.currentTimeMillis();
        List<NavigatorView.Entry> entries = new ArrayList<>();
        switch (page) {
            case REALMS -> {
                record Listed(UUID owner, String name, int players, RealmAccess access) {}
                List<Listed> realms = new ArrayList<>();
                data.owners().forEach((owner, realm) -> {
                    RealmAccess theirs = data.access(owner);
                    RealmRole role = theirs.roleOf(owner, self);
                    boolean keep = switch (filter) {
                        case 1 -> role == RealmRole.RESIDENT || role == RealmRole.TRUSTED;
                        case 2 -> data.areFriends(self, owner);
                        default -> true;
                    };
                    if (keep && RealmService.listedFor(server, owner, player)) {
                        ServerLevel level = server.getLevel(realm.levelKey());
                        realms.add(new Listed(owner, data.nameOf(owner), level == null ? 0 : level.players().size(), theirs));
                    }
                });
                Comparator<Listed> byName = Comparator.comparing(listed -> listed.name().toLowerCase());
                realms.sort(switch (sort) {
                    case 1 -> Comparator.comparingInt(Listed::players).thenComparing(byName);
                    case 2 -> Comparator.comparingInt(Listed::players).reversed().thenComparing(byName);
                    default -> byName;
                });
                for (Listed listed : realms) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(label("players", Component.literal(Integer.toString(listed.players()))));
                    if (listed.access().created() > 0) {
                        lines.add(label("created", Component.literal(duration(now - listed.access().created()))));
                    }
                    RealmRole role = listed.access().roleOf(listed.owner(), self);
                    if (role != RealmRole.VISITOR) {
                        lines.add(label("role", role(role)));
                    }
                    lines.add(Component.empty());
                    lines.add(action(0, "visit_realm"));
                    entries.add(new NavigatorView.Entry(listed.owner(), listed.name(), lines));
                }
            }
            case REALM_IN_REALM -> {
                List<UUID> players = new ArrayList<>(inRealm(player));
                Comparator<UUID> byName = Comparator.comparing(other -> data.nameOf(other).toLowerCase());
                players.sort(sort == 1 ? Comparator.<UUID, RealmRole>comparing(other -> access.roleOf(self, other)).reversed()
                        .thenComparing(byName) : byName);
                for (UUID other : players) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(label("joined", Component.literal(duration(now - ARRIVALS.getOrDefault(other, now)))));
                    lines.add(label("role", role(access.roleOf(self, other))));
                    lines.add(Component.empty());
                    lines.add(action(0, "edit_player"));
                    entries.add(new NavigatorView.Entry(other, data.nameOf(other), lines));
                }
            }
            case REALM_MEMBERS -> {
                List<Map.Entry<UUID, RealmAccess.Member>> members = new ArrayList<>(access.members().entrySet());
                members.removeIf(member -> filter == 1 && member.getValue().role() != RealmRole.RESIDENT
                        || filter == 2 && member.getValue().role() != RealmRole.TRUSTED);
                Comparator<Map.Entry<UUID, RealmAccess.Member>> byName = Comparator.comparing(member -> data.nameOf(member.getKey()).toLowerCase());
                members.sort(sort == 1 ? Comparator.<Map.Entry<UUID, RealmAccess.Member>>comparingLong(member -> lastOnline(server, data, member.getKey()))
                        .reversed().thenComparing(byName) : byName);
                for (Map.Entry<UUID, RealmAccess.Member> member : members) {
                    UUID id = member.getKey();
                    List<Component> lines = new ArrayList<>();
                    lines.add(onlineLine(server, data, id, now));
                    lines.add(label("joined_ago", Component.literal(duration(now - member.getValue().joined()))));
                    lines.add(label("role", role(member.getValue().role())));
                    lines.add(Component.empty());
                    lines.add(action(0, "edit_member"));
                    entries.add(new NavigatorView.Entry(id, data.nameOf(id), lines));
                }
            }
            case REALM_BANNED -> {
                List<Map.Entry<UUID, Long>> banned = new ArrayList<>(access.banned().entrySet());
                Comparator<Map.Entry<UUID, Long>> byTime = Map.Entry.comparingByValue();
                banned.sort(sort == 1 ? byTime.reversed() : byTime);
                for (Map.Entry<UUID, Long> ban : banned) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(label("banned", Component.literal(duration(now - ban.getValue()))));
                    lines.add(Component.empty());
                    lines.add(action(0, "unban"));
                    entries.add(new NavigatorView.Entry(ban.getKey(), data.nameOf(ban.getKey()), lines));
                }
            }
            case HOMES -> {
                List<HomeData.Home> homes = HomeService.homesOf(player);
                for (HomeData.Home home : homes) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(label("created_on", Component.literal(DATE.format(Instant.ofEpochMilli(home.created()).atZone(ZoneId.systemDefault())))));
                    lines.add(label("world", HomeService.worldName(server, home)));
                    lines.add(label("position", Component.literal(position(home))));
                    lines.add(Component.empty());
                    lines.add(action(0, "teleport"));
                    lines.add(action(1, "delete"));
                    lines.add(action(0, "rename"));
                    entries.add(new NavigatorView.Entry(HomeService.icon(server, home, self), home.name(), lines));
                }
            }
            case SOCIAL -> {
                Set<UUID> incoming = data.requestsTo(self);
                Set<UUID> sent = data.requestsFrom(self);
                List<UUID> friends = new ArrayList<>(data.friendsOf(self));
                friends.removeIf(id -> filter == 1 && !DummyPlayers.isOnline(server, id) || filter == 2 && DummyPlayers.isOnline(server, id)
                        || filter == 3);
                Comparator<UUID> byName = Comparator.comparing(id -> data.nameOf(id).toLowerCase());
                Comparator<UUID> order = sort == 0 ? Comparator.<UUID>comparingLong(id -> lastOnline(server, data, id)).reversed().thenComparing(byName)
                        : byName;
                friends.sort(order);
                if (filter == 0 || filter == 3) {
                    // Requests come first, incoming before sent, as they ask for an answer.
                    incoming.stream().sorted(order).forEach(id -> entries.add(new NavigatorView.Entry(id, data.nameOf(id), List.of(
                            label("request", Component.translatable("navigator.apeira.lore.request.incoming")), Component.empty(),
                            action(0, "accept_friend"), action(1, "decline_friend")))));
                    sent.stream().sorted(order).forEach(id -> entries.add(new NavigatorView.Entry(id, data.nameOf(id), List.of(
                            label("request", Component.translatable("navigator.apeira.lore.request.sent")), Component.empty(),
                            action(0, "view_profile"), action(1, "cancel_friend")))));
                }
                for (UUID id : friends) {
                    entries.add(new NavigatorView.Entry(id, data.nameOf(id), List.of(onlineLine(server, data, id, now), Component.empty(),
                            action(0, "view_profile"), action(0, "unfriend"))));
                }
            }
            default -> {}
        }
        return entries;
    }

    /** "x, y, z" of a home, rounded down to its block. */
    private static String position(HomeData.Home home) {
        return (int) Math.floor(home.position().x) + ", " + (int) Math.floor(home.position().y) + ", " + (int) Math.floor(home.position().z);
    }

    /** "Online: Now", "AFK: 7 minutes" or "Last Online: 3 hours and 4 minutes ago". */
    private static Component onlineLine(MinecraftServer server, RealmData data, UUID id, long now) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null && isAfk(server, id)) {
            return label("afk", Component.literal(duration(Util.getMillis() - player.getLastActionTime())));
        }
        return DummyPlayers.isOnline(server, id) ? label("online", Component.translatable("navigator.apeira.lore.now"))
                : label("last_online", Component.literal(duration(now - data.known(id).map(RealmData.KnownPlayer::lastSeen).orElse(now))));
    }

    private static long lastOnline(MinecraftServer server, RealmData data, UUID player) {
        return DummyPlayers.isOnline(server, player) ? Long.MAX_VALUE : data.known(player).map(RealmData.KnownPlayer::lastSeen).orElse(0L);
    }

    /** "Label: value", the label grey-blue and the value white, as Origin Realms writes its lore. */
    private static Component label(String key, Component value) {
        return Component.translatable("navigator.apeira.lore." + key, value.copy().withStyle(Style.EMPTY.withColor(VALUE)))
                .withStyle(Style.EMPTY.withColor(LABEL).withItalic(false));
    }

    /** A green action line behind a mouse glyph: 0 left click, 1 right click. */
    public static Component action(int button, String key) {
        return Component.empty().withStyle(Style.EMPTY.withItalic(false))
                .append(Component.literal(Character.toString(0xE100 + button)).withStyle(Style.EMPTY.withFont(KEYS).withColor(VALUE)))
                .append(" ")
                .append(Component.translatable("navigator.apeira.action." + key).withStyle(Style.EMPTY.withColor(ACTION)));
    }

    public static MutableComponent role(RealmRole role) {
        int color = switch (role) {
            case OWNER -> 0xFFAA00;
            case TRUSTED -> ACTION;
            case RESIDENT -> 0x55AAFF;
            case VISITOR -> LABEL;
        };
        return Component.translatable("navigator.apeira.role." + role.getSerializedName()).withColor(color);
    }

    /** Origin Realms' durations: the two largest units, "1 minute and 3 seconds", "4 years and 11 months". */
    static String duration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long[] sizes = {365L * 24 * 3600, 30L * 24 * 3600, 24 * 3600, 3600, 60, 1};
        String[] names = {"year", "month", "day", "hour", "minute", "second"};
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < sizes.length && parts.size() < 2; i++) {
            long count = seconds / sizes[i];
            if (count > 0) {
                parts.add(count + " " + names[i] + (count == 1 ? "" : "s"));
                seconds -= count * sizes[i];
            } else if (!parts.isEmpty()) {
                break;
            }
        }
        return parts.isEmpty() ? "0 seconds" : String.join(" and ", parts);
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ARRIVALS.put(event.getEntity().getUUID(), System.currentTimeMillis());
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ARRIVALS.put(event.getEntity().getUUID(), System.currentTimeMillis());
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        ARRIVALS.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SESSIONS.clear();
        ARRIVALS.clear();
    }

    /** The filter and sort names a page cycles through, for the client's layer textures ({@code realms/filter_<name>}). */
    public static String filterName(NavigatorPage page, int filter) {
        String[] names = filters(listPage(page));
        return names[Math.clamp(filter, 0, names.length - 1)];
    }

    public static String sortName(NavigatorPage page, int sort) {
        String[] names = sorts(listPage(page));
        return names[Math.clamp(sort, 0, names.length - 1)];
    }
}
