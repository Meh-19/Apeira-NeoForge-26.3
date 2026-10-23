package com.apeira.main.navigator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.navigator.NavigatorActionPayload.Action;
import com.apeira.main.realm.RealmAccess;
import com.apeira.main.realm.RealmData;
import com.apeira.main.realm.RealmPool;
import com.apeira.main.realm.RealmPreset;
import com.apeira.main.realm.RealmRole;
import com.apeira.main.realm.RealmService;
import com.apeira.main.realm.VisitingRule;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
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
    /** The mouse-button glyphs in front of an action line (left, right, middle click). */
    private static final FontDescription KEYS = new FontDescription.Resource(ApeiraIds.id("tooltip_keys"));

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
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), uuid -> new Session());
        if (needsRealm(page) && !hasRealm(player)) {
            player.sendSystemMessage(Component.translatable("message.apeira.realm.none"));
            page = NavigatorPage.MAIN;
        }
        session.go(page);
        send(player, session);
    }

    public static void handle(ServerPlayer player, NavigatorActionPayload payload) {
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), uuid -> new Session());
        NavigatorPage page = session.page;
        int argument = payload.argument();
        switch (payload.action()) {
            case OPEN_PAGE -> {
                NavigatorPage[] pages = NavigatorPage.values();
                NavigatorPage to = argument >= 0 && argument < pages.length ? pages[argument] : NavigatorPage.MAIN;
                if (isTab(to)) {
                    if (needsRealm(to) && !hasRealm(player)) {
                        player.sendSystemMessage(Component.translatable("message.apeira.realm.none"));
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
                        player.sendSystemMessage(Component.translatable("message.apeira.navigator.spawn"));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.apeira.realm.none"));
                    }
                }
                close(player);
                return;
            }
            case HOMES -> unavailable(player, "homes");
            case OPEN_WORLD -> unavailable(player, "open_world");
            case ENTRY -> {
                if (payload.target().isPresent() && entry(player, session, payload.target().get())) {
                    return;
                }
            }
            case CLOSE_POPUP -> session.page = switch (page) {
                case EDIT_PLAYER -> NavigatorPage.REALM_IN_REALM;
                case EDIT_MEMBER -> NavigatorPage.REALM_MEMBERS;
                case REALM_RESET, REALM_RESET_PRESET, REALM_RESET_CONFIRM -> NavigatorPage.REALM_SETTINGS;
                default -> page;
            };
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
                if (page == NavigatorPage.EDIT_PLAYER && session.target != null) {
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
        }
        send(player, session);
    }

    /** A head clicked in a list. Returns true when the navigator was closed (the player travelled). */
    private static boolean entry(ServerPlayer player, Session session, UUID target) {
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
                if (inRealm(player).stream().anyMatch(other -> other.getUUID().equals(target))) {
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
            default -> {}
        }
        return false;
    }

    private static void resetDimension(ServerPlayer player, Session session, int dimension) {
        if (session.page == NavigatorPage.REALM_SETTINGS) {
            session.page = NavigatorPage.REALM_RESET;
        } else if (session.page == NavigatorPage.REALM_RESET) {
            if (dimension != 0) {
                // Realm nethers and the Aether do not exist yet, so their buttons stay greyed out.
                player.sendSystemMessage(Component.translatable("message.apeira.realm.reset.travel",
                        Component.translatable("navigator.apeira.dimension." + (dimension == 1 ? "nether" : "aether"))));
            } else if (RealmService.canResetOverworld(player)) {
                session.page = NavigatorPage.REALM_RESET_PRESET;
            } else {
                player.sendSystemMessage(Component.translatable("message.apeira.realm.reset.travel",
                        Component.translatable("navigator.apeira.dimension.overworld")));
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
        player.sendSystemMessage(Component.translatable("message.apeira.navigator.unavailable", Component.translatable("navigator.apeira." + feature)));
    }

    /** Pages a client may ask for by name; popups and reset steps are reached only through their actions. */
    private static boolean isTab(NavigatorPage page) {
        return switch (page) {
            case MAIN, REALMS, REALM_IN_REALM, REALM_MEMBERS, REALM_BANNED, REALM_SETTINGS, OPEN_WORLDS -> true;
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
            default -> page.isSettings() ? NavigatorPage.REALMS : page;
        };
    }

    private static String[] filters(NavigatorPage page) {
        return switch (page) {
            case REALMS -> new String[] {"all", "added_to", "friends"};
            case REALM_MEMBERS -> new String[] {"all", "resident", "trusted"};
            default -> new String[] {"all"};
        };
    }

    private static String[] sorts(NavigatorPage page) {
        return switch (page) {
            case REALMS -> new String[] {"alpha_asc", "count_asc", "count_desc"};
            case REALM_IN_REALM -> new String[] {"alpha_asc", "rank"};
            case REALM_MEMBERS -> new String[] {"alpha_asc", "time_desc"};
            case REALM_BANNED -> new String[] {"time_asc", "time_desc"};
            default -> new String[] {"alpha_asc"};
        };
    }

    private static int defaultSort(NavigatorPage page) {
        return page == NavigatorPage.REALMS ? 2 : page == NavigatorPage.REALM_BANNED ? 1 : 0;
    }

    /** The players in {@code owner}'s realm right now, other than the owner. */
    private static List<ServerPlayer> inRealm(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        return RealmService.data(server).realmOf(owner.getUUID()).map(realm -> server.getLevel(realm.levelKey()))
                .map(level -> level.players().stream().filter(player -> player != owner).map(ServerPlayer.class::cast).toList())
                .orElse(List.of());
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
        NavigatorPage page = session.page;
        List<NavigatorView.Entry> all = entries(player, data, access, listPage(page), session.filter, session.sort);
        int pageCount = Math.max(1, (all.size() + NavigatorView.PAGE_SIZE - 1) / NavigatorView.PAGE_SIZE);
        session.pageIndex = Math.clamp(session.pageIndex, 0, pageCount - 1);
        List<NavigatorView.Entry> shown = all.subList(session.pageIndex * NavigatorView.PAGE_SIZE,
                Math.min(all.size(), (session.pageIndex + 1) * NavigatorView.PAGE_SIZE));

        Optional<NavigatorView.Entry> target = Optional.empty();
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
        }
        int setting = access.visitingRule().ordinal() | (access.privateChat() ? NavigatorView.PRIVATE_CHAT : 0)
                | (access.alwaysAccessible() ? NavigatorView.ALWAYS_ACCESSIBLE : 0);
        return new NavigatorView(page, session.pageIndex, pageCount, session.filter, session.sort, List.copyOf(shown), target, flags,
                setting, page == NavigatorPage.REALM_RESET_CONFIRM ? session.preset : -1);
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
                List<ServerPlayer> players = new ArrayList<>(inRealm(player));
                Comparator<ServerPlayer> byName = Comparator.comparing(other -> other.getName().getString().toLowerCase());
                players.sort(sort == 1 ? Comparator.<ServerPlayer, RealmRole>comparing(other -> access.roleOf(self, other.getUUID())).reversed()
                        .thenComparing(byName) : byName);
                for (ServerPlayer other : players) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(label("joined", Component.literal(duration(now - ARRIVALS.getOrDefault(other.getUUID(), now)))));
                    lines.add(label("role", role(access.roleOf(self, other.getUUID()))));
                    lines.add(Component.empty());
                    lines.add(action(0, "edit_player"));
                    entries.add(new NavigatorView.Entry(other.getUUID(), other.getName().getString(), lines));
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
                    lines.add(server.getPlayerList().getPlayer(id) != null ? label("online", Component.translatable("navigator.apeira.lore.now"))
                            : label("last_online", Component.literal(duration(now - data.known(id).map(RealmData.KnownPlayer::lastSeen).orElse(now)))));
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
            default -> {}
        }
        return entries;
    }

    private static long lastOnline(MinecraftServer server, RealmData data, UUID player) {
        return server.getPlayerList().getPlayer(player) != null ? Long.MAX_VALUE : data.known(player).map(RealmData.KnownPlayer::lastSeen).orElse(0L);
    }

    /** "Label: value", the label grey-blue and the value white, as Origin Realms writes its lore. */
    private static Component label(String key, Component value) {
        return Component.translatable("navigator.apeira.lore." + key, value.copy().withStyle(Style.EMPTY.withColor(VALUE)))
                .withStyle(Style.EMPTY.withColor(LABEL).withItalic(false));
    }

    /** A green action line behind a mouse glyph: 0 left click, 1 right click. */
    private static Component action(int button, String key) {
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
