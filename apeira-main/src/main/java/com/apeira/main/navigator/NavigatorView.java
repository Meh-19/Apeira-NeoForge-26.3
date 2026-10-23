package com.apeira.main.navigator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.apeira.decor.cosmetic.Wardrobe;
import com.apeira.main.party.Party;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything the client needs to draw one navigator page, built by {@link NavigatorService} from the server's realm
 * data: the page, the list page shown and how many there are, the filter and sort picked, the list's entries (player
 * heads, up to {@link #PAGE_SIZE}), the player a popup is about, the realm's settings and a few flags. The client draws
 * it as it is and never decides what a player may see or do.
 *
 * @param filter  index into the page's filters, as {@link NavigatorService} lists them
 * @param sort    index into the page's sorts
 * @param flags   {@link #HAS_REALM}, {@link #CAN_RESET_OVERWORLD}, and what the popup's player is to the viewer
 *                ({@code TARGET_*})
 * @param setting the visiting rule's ordinal, then {@link #PRIVATE_CHAT} and {@link #ALWAYS_ACCESSIBLE} bits from 8 up
 * @param preset  the region picked for a reset, or -1
 * @param party   the viewer's party roster: its members, the leader first, then its open invites
 * @param partySize how many of {@code party} are members; the rest are invites
 * @param cosmetics what the profile's player wears, for its cosmetic slots
 */
public record NavigatorView(NavigatorPage page, int pageIndex, int pageCount, int filter, int sort, List<Entry> entries,
        Optional<Entry> target, int flags, int setting, int preset, List<Entry> party, int partySize, Wardrobe cosmetics) {
    public static final int PAGE_SIZE = 18;
    public static final int HAS_REALM = 1;
    public static final int CAN_RESET_OVERWORLD = 2;
    public static final int TARGET_MEMBER = 4;
    public static final int TARGET_INVITED = 8;
    public static final int TARGET_TRUSTED = 16;
    public static final int TARGET_FRIEND = 32;
    /** The popup's player has asked the viewer to be friends. */
    public static final int TARGET_REQUESTED = 64;
    /** The viewer has asked the popup's player to be friends. */
    public static final int TARGET_REQUEST_SENT = 128;
    public static final int TARGET_ONLINE = 256;
    /** The navigator was opened on Realms home by {@code /realms}, so there is no menu to go back to. */
    public static final int NO_BACK = 512;
    /** The viewer is in a party, leads it, or has party chat on. */
    public static final int IN_PARTY = 1 << 10;
    public static final int PARTY_LEADER = 1 << 11;
    public static final int PARTY_CHAT = 1 << 12;
    /** The profile's player has done nothing for {@link NavigatorService#AFK_MS}. */
    public static final int TARGET_AFK = 1 << 13;
    /** The popup's or profile's player is in the viewer's party, or has an open invite to it. */
    public static final int TARGET_IN_PARTY = 1 << 14;
    public static final int TARGET_PARTY_INVITED = 1 << 15;
    public static final int PRIVATE_CHAT = 1 << 8;
    public static final int ALWAYS_ACCESSIBLE = 1 << 9;

    public static final StreamCodec<RegistryFriendlyByteBuf, NavigatorView> STREAM_CODEC = StreamCodec.of((buf, view) -> {
        NavigatorPage.STREAM_CODEC.encode(buf, view.page);
        buf.writeVarInt(view.pageIndex);
        buf.writeVarInt(view.pageCount);
        buf.writeVarInt(view.filter);
        buf.writeVarInt(view.sort);
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list(PAGE_SIZE)).encode(buf, view.entries);
        ByteBufCodecs.optional(Entry.STREAM_CODEC).encode(buf, view.target);
        buf.writeVarInt(view.flags);
        buf.writeVarInt(view.setting);
        buf.writeVarInt(view.preset);
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list(Party.MAX_SIZE)).encode(buf, view.party);
        buf.writeVarInt(view.partySize);
        Wardrobe.STREAM_CODEC.encode(buf, view.cosmetics);
    }, buf -> new NavigatorView(NavigatorPage.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), Entry.STREAM_CODEC.apply(ByteBufCodecs.list(PAGE_SIZE)).decode(buf),
            ByteBufCodecs.optional(Entry.STREAM_CODEC).decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(Party.MAX_SIZE)).decode(buf), buf.readVarInt(), Wardrobe.STREAM_CODEC.decode(buf)));

    public static NavigatorView closed() {
        return new NavigatorView(NavigatorPage.CLOSED, 0, 1, 0, 0, List.of(), Optional.empty(), 0, 0, -1, List.of(), 0, Wardrobe.EMPTY);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public int visitingRule() {
        return setting & 0xFF;
    }

    public boolean setting(int bit) {
        return (setting & bit) != 0;
    }

    /**
     * One player head in a list or popup: whose head it is, the name shown and the tooltip lines under it, written by
     * the server in Origin Realms' colours.
     */
    public record Entry(UUID id, String name, List<Component> lines) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Entry::id,
                ByteBufCodecs.stringUtf8(16), Entry::name,
                ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(16)), Entry::lines,
                Entry::new);
    }
}
