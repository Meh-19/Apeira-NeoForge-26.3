package com.apeira.main.navigator;

import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/** A navigator button's action, sent to the server with {@link com.apeira.main.network.NavigatorActionPayload}. */
public enum NavigatorAction {
    OPEN_MAIN(NavigatorPage.MAIN),
    OPEN_REALMS(NavigatorPage.REALMS),
    OPEN_ACCESS(NavigatorPage.ACCESS),
    OPEN_MEMBERS(NavigatorPage.MEMBERS),
    OPEN_SETTINGS(NavigatorPage.SETTINGS),
    OPEN_RESOURCE_WORLDS(NavigatorPage.RESOURCE_WORLDS),
    /** Home management; homes are not added yet. */
    HOMES(null),
    /** Teleports the player to their realm's spawn (their respawn point until realms exist). */
    SPAWN(null),
    /** Joins one of the six open worlds, by index; open worlds are not added yet. */
    OPEN_WORLD(null);

    private static final IntFunction<NavigatorAction> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, NavigatorAction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final @Nullable NavigatorPage page;

    NavigatorAction(@Nullable NavigatorPage page) {
        this.page = page;
    }

    /** The page this action opens, or null when it does something else. */
    public @Nullable NavigatorPage page() {
        return page;
    }
}
