package com.apeira.decor.crate;

import java.util.Map;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * Loads {@link CrateLoot} from {@code data/<namespace>/crate_loot/}, on server start and on {@code /reload}. Server
 * side only: clients never roll.
 */
public final class CrateLootManager extends SimpleJsonResourceReloadListener<CrateLoot> {
    public static final String DIRECTORY = "crate_loot";

    private static volatile Map<Identifier, CrateLoot> loot = Map.of();

    private CrateLootManager() {
        super(CrateLoot.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    /** The loot of the crate with this block id, or {@link CrateLoot#EMPTY} if it has none. */
    public static CrateLoot get(Identifier crate) {
        return loot.getOrDefault(crate, CrateLoot.EMPTY);
    }

    public static void addListener(AddServerReloadListenersEvent event) {
        event.addListener(ApeiraIds.id(DIRECTORY), new CrateLootManager());
    }

    @Override
    protected void apply(Map<Identifier, CrateLoot> prepared, ResourceManager manager, ProfilerFiller profiler) {
        loot = Map.copyOf(prepared);
    }
}
