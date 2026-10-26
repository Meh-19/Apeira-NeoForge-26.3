package com.apeira.main.realm;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * What a player may do in a realm, from {@link RealmAccess#roleOf}. Residents only break and place blocks; Trusted
 * players and the owner also open containers, break furniture and touch entities; visitors only look around, and mobs
 * ignore them. Checked by {@link RealmProtection} and {@link RealmVisitors}.
 */
public enum RealmRole implements StringRepresentable {
    VISITOR("visitor"),
    RESIDENT("resident"),
    TRUSTED("trusted"),
    OWNER("owner");

    public static final Codec<RealmRole> CODEC = StringRepresentable.fromEnum(RealmRole::values);

    private final String name;

    RealmRole(String name) {
        this.name = name;
    }

    /** Whether this role is a member's, one that can build. */
    public boolean canBuild() {
        return this != VISITOR;
    }

    /** Whether this role may open containers and break furniture. */
    public boolean canUseContainers() {
        return this == TRUSTED || this == OWNER;
    }

    /** Whether this role may use, hit or ride entities other than players and hostile mobs. */
    public boolean canTouchEntities() {
        return this == TRUSTED || this == OWNER;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
