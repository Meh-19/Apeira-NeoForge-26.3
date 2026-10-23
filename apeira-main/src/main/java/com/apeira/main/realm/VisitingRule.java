package com.apeira.main.realm;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * Who may visit a realm, the Settings panel's four buttons in order. Only {@link #EVERYONE} lists the realm in the
 * public realm list. Members and the owner may always enter.
 */
public enum VisitingRule implements StringRepresentable {
    EVERYONE("everyone"),
    FRIENDS("friends"),
    MEMBERS("members"),
    NOBODY("nobody");

    public static final Codec<VisitingRule> CODEC = StringRepresentable.fromEnum(VisitingRule::values);

    private final String name;

    VisitingRule(String name) {
        this.name = name;
    }

    public static VisitingRule byId(int id) {
        VisitingRule[] values = values();
        return id >= 0 && id < values.length ? values[id] : EVERYONE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
