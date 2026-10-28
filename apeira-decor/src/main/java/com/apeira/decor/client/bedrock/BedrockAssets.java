package com.apeira.decor.client.bedrock;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.ApeiraDecor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * Bedrock models and animations from resource packs, for {@link BedrockRenderer}: {@code assets/<namespace>/bedrock/
 * models/<path>.geo.json} as {@code <namespace>:<path>}, and {@code bedrock/animations/<path>.animation.json} as
 * {@code <namespace>:<path>}, each file's animations by name. Reloaded with the other client resources (F3+T); a file
 * that does not parse is logged and skipped.
 */
public final class BedrockAssets extends SimplePreparableReloadListener<BedrockAssets.Loaded> {
    private static final FileToIdConverter MODELS = new FileToIdConverter("bedrock/models", ".geo.json");
    private static final FileToIdConverter ANIMATIONS = new FileToIdConverter("bedrock/animations", ".animation.json");
    private static Loaded loaded = new Loaded(Map.of(), Map.of());

    record Loaded(Map<Identifier, BedrockModel> models, Map<Identifier, Map<String, BedrockAnimation>> animations) {}

    /** Registers the loader; called from the decor client's constructor. */
    public static void register(AddClientReloadListenersEvent event) {
        event.addListener(ApeiraDecor.REGISTRAR.id("bedrock_assets"), new BedrockAssets());
    }

    public static @Nullable BedrockModel model(Identifier id) {
        return loaded.models.get(id);
    }

    public static @Nullable BedrockAnimation animation(Identifier file, String name) {
        return loaded.animations.getOrDefault(file, Map.of()).get(name);
    }

    @Override
    protected Loaded prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, BedrockModel> models = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : MODELS.listMatchingResources(manager).entrySet()) {
            JsonObject json = read(entry.getKey(), entry.getValue());
            if (json != null) {
                try {
                    models.put(MODELS.fileToId(entry.getKey()), BedrockModel.parse(json));
                } catch (RuntimeException e) {
                    ApeiraDecor.LOGGER.error("Could not read Bedrock model {}", entry.getKey(), e);
                }
            }
        }
        Map<Identifier, Map<String, BedrockAnimation>> animations = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : ANIMATIONS.listMatchingResources(manager).entrySet()) {
            JsonObject json = read(entry.getKey(), entry.getValue());
            if (json != null) {
                try {
                    animations.put(ANIMATIONS.fileToId(entry.getKey()), BedrockAnimation.parseAll(json));
                } catch (RuntimeException e) {
                    ApeiraDecor.LOGGER.error("Could not read Bedrock animations {}", entry.getKey(), e);
                }
            }
        }
        return new Loaded(Map.copyOf(models), Map.copyOf(animations));
    }

    private static @Nullable JsonObject read(Identifier file, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            ApeiraDecor.LOGGER.error("Could not read {}", file, e);
            return null;
        }
    }

    @Override
    protected void apply(Loaded prepared, ResourceManager manager, ProfilerFiller profiler) {
        loaded = prepared;
    }
}
