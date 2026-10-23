package com.apeira.main.navigator;

import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/**
 * The pages of the navigator, Origin Realms' {@code /navigator} menu: the root menu, its sub-screens and the popups
 * drawn over them. The server decides which page a player sees ({@link NavigatorService}); the client only draws it.
 */
public enum NavigatorPage {
    /** The root menu: Realm, Open World, Homes and Spawn. */
    MAIN,
    /** Realms home: the Teleport, Access and Settings tabs over the realm list. */
    REALMS,
    /** Access, "In Realm" tab: the players in the viewer's realm now. */
    REALM_IN_REALM,
    /** Access, "Members" tab: the realm's Residents and Trusted players. */
    REALM_MEMBERS,
    /** Access, "Banned players" tab. */
    REALM_BANNED,
    /** The Settings panel over Realms home: visiting rule, chat and offline-access toggles, spawn and reset. */
    REALM_SETTINGS,
    /** Settings with the reset button opened into its Overworld, Nether and Aether buttons. */
    REALM_RESET,
    /** The region picker for an Overworld reset. */
    REALM_RESET_PRESET,
    /** Settings with the reset button turned into Confirm, after a region was picked. */
    REALM_RESET_CONFIRM,
    /** Popup over "In Realm": kick, ban or invite a player in the realm. */
    EDIT_PLAYER,
    /** Popup over "Members": kick, ban or change a member's role. */
    EDIT_MEMBER,
    /** The six open (resource) worlds. */
    OPEN_WORLDS,
    /** Popup over the profile: the player's homes, a grid of up to 15. */
    HOMES,
    /** Social: the friends list with incoming and sent friend requests (the party controls are not available yet). */
    SOCIAL,
    /** Another player's profile, opened from the friends list: realm invite or removal, friend actions, visit their realm. */
    PROFILE_OTHER,
    /** The player's own profile: their model, cosmetics and stats, and the Realms, Open Worlds and Homes buttons. */
    PROFILE,
    /** The party roster ({@code /party}): members, the leader first, then open invites, with chat, leave and the leader's tools. */
    PARTY,
    /** The party roster with a member picked by the leader: hand over the lead or kick. */
    PARTY_MEMBER,
    /** Popup over Social: the party's chat toggle and leave (disband for the leader). */
    EDIT_PARTY,
    /** Popup over Social, for the leader: hand the lead to a member or kick them. */
    EDIT_PARTY_MEMBER,
    /** Not a page: the server asks the client to close the navigator, for example after a teleport. */
    CLOSED;

    private static final IntFunction<NavigatorPage> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, NavigatorPage> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    /** Whether this is one of the three Access tabs or a popup over one. */
    public boolean isAccess() {
        return this == REALM_IN_REALM || this == REALM_MEMBERS || this == REALM_BANNED || this == EDIT_PLAYER || this == EDIT_MEMBER;
    }

    /** Whether this is the party roster or a party popup over Social. */
    public boolean isParty() {
        return this == PARTY || this == PARTY_MEMBER || this == EDIT_PARTY || this == EDIT_PARTY_MEMBER;
    }

    /** Whether this is a profile, the player's own or another's. */
    public boolean isProfile() {
        return this == PROFILE || this == PROFILE_OTHER;
    }

    /** Whether this shows the Settings panel. */
    public boolean isSettings() {
        return this == REALM_SETTINGS || this == REALM_RESET || this == REALM_RESET_PRESET || this == REALM_RESET_CONFIRM;
    }
}
