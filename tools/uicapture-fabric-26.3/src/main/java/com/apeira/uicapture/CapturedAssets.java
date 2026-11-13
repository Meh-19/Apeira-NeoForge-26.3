package com.apeira.uicapture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Copies the resources a capture refers to (textures with their {@code .mcmeta}, font definitions) out of the loaded
 * resource packs, the server pack included, into {@code assets/<namespace>/<path>} under the capture folder, and lists
 * each one in the capture's {@code assets} section with the pack it came from.
 */
final class CapturedAssets {
    private final ResourceManager resources;
    private final @Nullable Path root;
    private final JsonObject index = new JsonObject();

    /** {@code root} null copies nothing (for lookups that only read the packs). */
    CapturedAssets(ResourceManager resources, @Nullable Path root) {
        this.resources = resources;
        this.root = root;
    }

    JsonObject index() {
        return index;
    }

    /** Copies the first of {@code candidates} that exists; returns its id, or null when none does (a dynamic texture). */
    @Nullable Identifier copy(Identifier... candidates) {
        for (Identifier id : candidates) {
            if (index.has(id.toString())) {
                return id;
            }
            Optional<Resource> resource = resources.getResource(id);
            if (resource.isPresent()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("pack", resource.get().sourcePackId());
                entry.addProperty("file", write(id, resource.get()));
                resources.getResource(id.withPath(id.getPath() + ".mcmeta")).ifPresent(meta -> {
                    entry.addProperty("mcmeta", write(id.withPath(id.getPath() + ".mcmeta"), meta));
                });
                index.add(id.toString(), entry);
                return id;
            }
        }
        return null;
    }

    private String write(Identifier id, Resource resource) {
        String relative = "assets/" + id.getNamespace() + "/" + id.getPath();
        if (root == null) {
            return relative;
        }
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            return "(not copied: path leaves the capture folder)";
        }
        try (InputStream in = resource.open()) {
            Files.createDirectories(file.getParent());
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            return relative;
        } catch (IOException e) {
            UiCaptureClient.LOGGER.warn("Could not copy {}", id, e);
            return "(not copied: " + e.getMessage() + ")";
        }
    }

    /** Where a GUI sprite's image is: {@code textures/gui/sprites/<path>.png}. */
    static Identifier guiSprite(Identifier sprite) {
        return sprite.withPath(path -> "textures/gui/sprites/" + path + ".png");
    }

    /** Where an atlas sprite's image may be: under {@code textures/gui/sprites/} for the GUI atlas, else {@code textures/}. */
    static Identifier[] atlasSprite(Identifier atlas, Identifier sprite) {
        Identifier plain = sprite.withPath(path -> "textures/" + path + ".png");
        return atlas.getPath().contains("gui") ? new Identifier[] {guiSprite(sprite), plain} : new Identifier[] {plain, guiSprite(sprite)};
    }
}
