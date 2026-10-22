package com.apeira.main.navigator;

import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/**
 * A page of the navigator menu. Each is drawn as Origin Realms draws it: the vanilla 9x6 chest with its panels as glyphs
 * of the {@code apeira:navigator} font in the title, so the captured textures line up as they did on the server.
 */
public enum NavigatorPage {
    /** The root menu: Realm, Open World, Homes and Spawn. */
    MAIN,
    /** The Realms homepage. */
    REALMS,
    /** Realm access, "In Realm" tab. */
    ACCESS,
    /** Realm access, "Members" tab. */
    MEMBERS,
    /** Realm settings. */
    SETTINGS,
    /** The six open (resource) worlds. */
    RESOURCE_WORLDS;

    private static final IntFunction<NavigatorPage> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, NavigatorPage> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    /** The page the navigation bar's back button returns to. */
    public NavigatorPage parent() {
        return switch (this) {
            case MAIN, REALMS, RESOURCE_WORLDS -> MAIN;
            case ACCESS, MEMBERS, SETTINGS -> REALMS;
        };
    }
}
