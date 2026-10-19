package com.apeira.archae.assembly;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.apeira.archae.network.AssemblyRecipesPayload;
import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * The assembly table's recipes, loaded from {@code data/<namespace>/assembly/} on server start and on {@code /reload}
 * and sent to every client on login and reload ({@link AssemblyRecipesPayload}), since the table's screen shows them.
 *
 * <p>Recipes are kept in the order of their result's item id, so the server and client menus number a fragment's
 * recipes alike (Argon's came out of a hash map, in no fixed order).
 */
public final class AssemblyRecipes extends SimpleJsonResourceReloadListener<AssemblyRecipe> {
    public static final String DIRECTORY = "assembly";
    private static final Comparator<AssemblyRecipe> ORDER = Comparator.comparing(recipe -> BuiltInRegistries.ITEM.getKey(recipe.result()));

    private static volatile List<AssemblyRecipe> server = List.of();
    private static volatile List<AssemblyRecipe> client = List.of();

    private AssemblyRecipes() {
        super(AssemblyRecipe.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    /** The recipes that take {@code fragment}, on the side {@code level} is on. */
    public static List<AssemblyRecipe> forFragment(Level level, Item fragment) {
        return (level.isClientSide() ? client : server).stream().filter(recipe -> recipe.fragment() == fragment).toList();
    }

    /** Replaces the client's recipes with the server's. */
    public static void setClientRecipes(List<AssemblyRecipe> recipes) {
        client = List.copyOf(recipes);
    }

    public static void addListener(AddServerReloadListenersEvent event) {
        event.addListener(ApeiraIds.id("assembly_recipes"), new AssemblyRecipes());
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        AssemblyRecipesPayload payload = new AssemblyRecipesPayload(server);
        event.getRelevantPlayers().forEach(player -> ApeiraPackets.sendToPlayer(player, payload));
    }

    @Override
    protected void apply(Map<Identifier, AssemblyRecipe> prepared, ResourceManager manager, ProfilerFiller profiler) {
        server = prepared.values().stream().sorted(ORDER).toList();
    }
}
