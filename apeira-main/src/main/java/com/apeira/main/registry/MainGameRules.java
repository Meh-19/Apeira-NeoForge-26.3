package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Apeira's game rules, per world: set in the Create World screen or with {@code /gamerule}. */
public final class MainGameRules {
    /**
     * {@code apeira:realm_presets} (default true): players without a realm get the onboarding region picker when they
     * join, and the server keeps a pool of pre-generated realms ({@link com.apeira.main.realm.RealmPool}). Off, as for a
     * superflat, debug or test world, nobody is sent the picker, {@code /realm create} refuses and no pool realm is made
     * or generated; realms that already exist keep working. The Create World screen's World tab has a switch for it
     * ({@code mixin/client/CreateWorldTabMixin}).
     */
    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> REALM_PRESETS = Apeira.REGISTRAR.forRegistry(Registries.GAME_RULE)
            .register("realm_presets", () -> new GameRule<>(GameRuleCategory.MISC, GameRuleType.BOOL, BoolArgumentType.bool(),
                    (visitor, rule) -> visitor.visitBoolean(rule), Codec.BOOL, value -> value ? 1 : 0, true, FeatureFlagSet.of()));

    private MainGameRules() {}

    public static void init(ApeiraRegistrar registrar) {}

    /** Whether {@code server}'s world uses realm presets ({@link #REALM_PRESETS}). */
    public static boolean realmPresets(MinecraftServer server) {
        return server.getGameRules().get(REALM_PRESETS.get());
    }
}
