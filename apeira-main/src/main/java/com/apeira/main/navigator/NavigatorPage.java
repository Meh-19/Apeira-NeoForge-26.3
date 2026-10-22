package com.apeira.main.navigator;

import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/**
 * The pages of the navigator, Origin Realms' {@code /navigator} menu: the root menu and the sub-screens its buttons
 * open. The server decides which page a player sees ({@link OpenNavigatorPayload}); the client only draws it.
 */
public enum NavigatorPage {
    /** The root menu: Realm, Open World, Homes and Spawn. */
    MAIN,
    /** Realms home: the Teleport, Access and Settings tabs over the public realm list. */
    REALMS,
    /** The realm's member list. */
    REALM_ACCESS,
    /** The realm settings panel over Realms home. */
    REALM_SETTINGS,
    /** The six open (resource) worlds. */
    OPEN_WORLDS;

    private static final IntFunction<NavigatorPage> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, NavigatorPage> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
}
