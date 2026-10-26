package com.apeira.uicapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Writes what the game knows of the joined server's data as a datapack, under {@code apeira_exports/datapack/} in the
 * game directory: {@code /apeiradev export_datapack [namespace]}. In singleplayer (or a LAN world one hosts) everything
 * comes from the integrated server: loot tables, predicates and item modifiers, recipes, advancements, every datapack
 * registry (worldgen, dimension types and dimensions included), every registry's tags and every structure template.
 * On a remote server only what servers send clients exists, so the export is the synchronized registries (with their
 * network codecs: biomes have climate and effects but no features or spawns), every synchronized tag (flattened, as the
 * client receives them), the advancements the player can see (criteria as {@code minecraft:impossible}, since servers
 * send only their names) and the recipe book's displays (a report, not recipes); loot tables, recipes, worldgen
 * features, dimensions and structure templates stay on the server and are listed as skipped.
 * {@code /apeiradev export_structure <id>} saves the {@code /apeiradev area} as a structure template, from the client's
 * own copy of the world, so a live server's builds can be taken too.
 * Files are overwritten, never deleted; {@code export_report.json} lists the counts, what was skipped and what failed.
 */
final class DatapackExporter {
    static final String FOLDER = "apeira_exports/datapack";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private DatapackExporter() {
    }

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("apeiradev")
                .then(ClientCommandManager.literal("export_datapack")
                        .executes(context -> export(context, null))
                        .then(ClientCommandManager.argument("namespace", StringArgumentType.word())
                                .executes(context -> export(context, StringArgumentType.getString(context, "namespace")))))
                .then(ClientCommandManager.literal("export_structure")
                        .then(ClientCommandManager.argument("id", IdentifierArgument.id())
                                .executes(context -> exportStructure(context, context.getArgument("id", Identifier.class))))));
    }

    /** What one export wrote: counts per category, per registry, and what was skipped or failed. */
    private static final class Report {
        final Path root;
        final @Nullable String namespace;
        final Map<String, Integer> counts = new LinkedHashMap<>();
        final Map<String, Integer> registries = new TreeMap<>();
        final List<String> skipped = new ArrayList<>();
        final List<String> failed = new ArrayList<>();
        int files;

        Report(Path root, @Nullable String namespace) {
            this.root = root;
            this.namespace = namespace;
            for (String category : List.of("loot_table", "predicate", "item_modifier", "recipe", "recipe_book_display", "tags", "advancement", "registry", "structure")) {
                counts.put(category, 0);
            }
        }

        boolean wanted(Identifier id) {
            return namespace == null || namespace.equals(id.getNamespace());
        }

        void count(String category) {
            counts.merge(category, 1, Integer::sum);
        }

        /** Writes {@code data/<namespace>/<folder>/<path>.<extension>}. */
        Path file(Identifier id, String folder, String extension) throws IOException {
            Path file = root.resolve("data").resolve(id.getNamespace()).resolve(folder).resolve(id.getPath() + extension);
            Files.createDirectories(file.getParent());
            files++;
            return file;
        }

        void json(Identifier id, String folder, JsonElement json) throws IOException {
            Files.writeString(file(id, folder, ".json"), GSON.toJson(json), StandardCharsets.UTF_8);
        }

        void fail(String what, String why) {
            failed.add(what + ": " + why);
            UiCaptureClient.LOGGER.warn("[export_datapack] {} failed: {}", what, why);
        }
    }

    private static int export(CommandContext<FabricClientCommandSource> context, @Nullable String namespace) {
        Minecraft minecraft = Minecraft.getInstance();
        Path root = minecraft.gameDirectory.toPath().resolve(FOLDER);
        Report report = new Report(root, namespace);
        IntegratedServer server = minecraft.getSingleplayerServer();
        context.getSource().sendFeedback(Component.translatable("message.apeira.datapack.started",
                server != null ? "integrated server" : "live server", root.toString()));
        try {
            Files.createDirectories(root);
            if (server != null) {
                // The server's data is read on its own thread; the client only waits.
                server.submit(() -> {
                    exportServer(server, report);
                    return null;
                }).join();
            } else {
                exportClient(minecraft, report);
            }
            writePack(report, server != null);
        } catch (Exception e) {
            UiCaptureClient.LOGGER.error("Datapack export failed", e);
            context.getSource().sendError(Component.translatable("message.apeira.datapack.failed", String.valueOf(e.getMessage())));
            return 0;
        }
        Map<String, Integer> c = report.counts;
        Component done = Component.translatable("message.apeira.datapack.done", c.get("loot_table"), c.get("recipe"), c.get("tags"),
                c.get("advancement"), c.get("registry"), c.get("structure"), report.files, report.failed.size(), root.toString());
        context.getSource().sendFeedback(done);
        UiCaptureClient.LOGGER.info("[export_datapack] {}", done.getString());
        for (String skipped : report.skipped) {
            context.getSource().sendFeedback(Component.translatable("message.apeira.datapack.skipped", skipped).withStyle(ChatFormatting.GRAY));
            UiCaptureClient.LOGGER.info("[export_datapack] skipped: {}", skipped);
        }
        return report.files;
    }

    // Integrated server: everything the server has loaded.

    private static void exportServer(IntegratedServer server, Report report) {
        HolderLookup.Provider registries = server.registryAccess();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        HolderLookup.Provider reloadable = server.reloadableRegistries().lookup();
        RegistryOps<JsonElement> reloadableOps = reloadable.createSerializationContext(JsonOps.INSTANCE);
        elements(reloadable, Registries.LOOT_TABLE, LootTable.DIRECT_CODEC, reloadableOps, "loot_table", report);
        elements(reloadable, Registries.PREDICATE, LootItemCondition.DIRECT_CODEC, reloadableOps, "predicate", report);
        elements(reloadable, Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC, reloadableOps, "item_modifier", report);

        for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
            Identifier id = recipe.id().identifier();
            if (report.wanted(id)) {
                write(report, id, Registries.elementsDirPath(Registries.RECIPE), Recipe.CODEC, recipe.value(), ops, "recipe");
            }
        }
        for (AdvancementHolder advancement : server.getAdvancements().getAllAdvancements()) {
            if (report.wanted(advancement.id())) {
                write(report, advancement.id(), Registries.elementsDirPath(Registries.ADVANCEMENT), Advancement.CODEC, advancement.value(), ops, "advancement");
            }
        }

        Set<ResourceKey<?>> seen = new LinkedHashSet<>();
        for (List<RegistryDataLoader.RegistryData<?>> list : List.of(RegistryDataLoader.WORLDGEN_REGISTRIES, RegistryDataLoader.DIMENSION_REGISTRIES)) {
            for (RegistryDataLoader.RegistryData<?> data : list) {
                if (seen.add(data.key())) {
                    registry(registries, data, ops, report);
                }
            }
        }
        registries.listRegistries().forEach(lookup -> tags(lookup, report));

        var structures = server.getStructureManager();
        structures.listTemplates().filter(report::wanted).sorted().forEach(id -> {
            try {
                Optional<StructureTemplate> template = structures.get(id);
                if (template.isEmpty()) {
                    report.fail("structure " + id, "not loadable");
                    return;
                }
                NbtIo.writeCompressed(template.get().save(new CompoundTag()), report.file(id, "structure", ".nbt"));
                report.count("structure");
            } catch (Exception e) {
                report.fail("structure " + id, String.valueOf(e.getMessage()));
            }
        });
    }

    private static <T> void elements(HolderLookup.Provider provider, ResourceKey<? extends Registry<? extends T>> key, Codec<T> codec,
            RegistryOps<JsonElement> ops, String category, Report report) {
        Optional<? extends HolderLookup.RegistryLookup<T>> lookup = provider.lookup(key);
        if (lookup.isEmpty()) {
            report.skipped.add(key.identifier() + " (not loaded)");
            return;
        }
        String folder = Registries.elementsDirPath(key);
        lookup.get().listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            if (report.wanted(id)) {
                write(report, id, folder, codec, holder.value(), ops, category);
            }
        });
    }

    // Live server: only what was sent to the client.

    private static void exportClient(Minecraft minecraft, Report report) {
        var connection = minecraft.getConnection();
        if (connection == null) {
            throw new IllegalStateException("not connected");
        }
        HolderLookup.Provider registries = connection.registryAccess();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        for (RegistryDataLoader.RegistryData<?> data : RegistryDataLoader.SYNCHRONIZED_REGISTRIES) {
            registry(registries, data, ops, report);
        }
        registries.listRegistries().forEach(lookup -> tags(lookup, report));

        String folder = Registries.elementsDirPath(Registries.ADVANCEMENT);
        for (AdvancementNode node : connection.getAdvancements().getTree().nodes()) {
            AdvancementHolder holder = node.holder();
            if (!report.wanted(holder.id())) {
                continue;
            }
            try {
                report.json(holder.id(), folder, sentAdvancement(holder.value(), ops));
                report.count("advancement");
            } catch (Exception e) {
                report.fail("advancement " + holder.id(), String.valueOf(e.getMessage()));
            }
        }

        // Recipes stay on the server since 1.21.2; the recipe book's displays are what the player was shown of them.
        JsonArray displays = new JsonArray();
        if (minecraft.player != null) {
            for (RecipeCollection collection : minecraft.player.getRecipeBook().getCollections()) {
                for (RecipeDisplayEntry entry : collection.getRecipes()) {
                    JsonObject json = new JsonObject();
                    json.addProperty("category", String.valueOf(entry.category()));
                    entry.group().ifPresent(group -> json.addProperty("group", group));
                    DataResult<JsonElement> display = RecipeDisplay.CODEC.encodeStart(ops, entry.display());
                    display.result().ifPresentOrElse(value -> json.add("display", value),
                            () -> json.addProperty("error", display.error().map(DataResult.Error::message).orElse("?")));
                    displays.add(json);
                    report.count("recipe_book_display");
                }
            }
        }
        try {
            Files.writeString(report.root.resolve("recipe_book_displays.json"), GSON.toJson(displays), StandardCharsets.UTF_8);
        } catch (IOException e) {
            report.fail("recipe_book_displays.json", String.valueOf(e.getMessage()));
        }

        report.skipped.add("loot tables, predicates and item modifiers (server-only)");
        report.skipped.add("recipes (server-only since 1.21.2; the recipe book's displays are in recipe_book_displays.json)");
        report.skipped.add("worldgen features, noise settings, structures, template pools and dimensions (not synchronized; biomes lack features and spawns)");
        report.skipped.add("structure templates (server-only; save builds with /apeiradev export_structure)");
        report.skipped.add("advancement criteria and rewards (servers send only criterion names)");
    }

    /** An advancement as a client has it: parent, display and requirements; each criterion an impossible trigger. */
    private static JsonObject sentAdvancement(Advancement advancement, RegistryOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        advancement.parent().ifPresent(parent -> json.addProperty("parent", parent.toString()));
        advancement.display().ifPresent(display -> json.add("display", DisplayInfo.CODEC.encodeStart(ops, display).getOrThrow()));
        JsonObject criteria = new JsonObject();
        for (List<String> group : advancement.requirements().requirements()) {
            for (String name : group) {
                JsonObject criterion = new JsonObject();
                criterion.addProperty("trigger", "minecraft:impossible");
                criteria.add(name, criterion);
            }
        }
        json.add("criteria", criteria);
        json.add("requirements", AdvancementRequirements.CODEC.encodeStart(ops, advancement.requirements()).getOrThrow());
        if (advancement.sendsTelemetryEvent()) {
            json.addProperty("sends_telemetry_event", true);
        }
        return json;
    }

    // Shared.

    private static <T> void registry(HolderLookup.Provider registries, RegistryDataLoader.RegistryData<T> data, RegistryOps<JsonElement> ops, Report report) {
        Optional<? extends HolderLookup.RegistryLookup<T>> lookup = registries.lookup(data.key());
        if (lookup.isEmpty()) {
            return;
        }
        String folder = Registries.elementsDirPath(data.key());
        lookup.get().listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            if (report.wanted(id) && write(report, id, folder, data.elementCodec(), holder.value(), ops, "registry")) {
                report.registries.merge(data.key().identifier().toString(), 1, Integer::sum);
            }
        });
    }

    /** Every tag of a registry, flattened to element ids (nested tags arrive already expanded). */
    private static <T> void tags(HolderLookup.RegistryLookup<T> lookup, Report report) {
        String folder = Registries.tagsDirPath(lookup.key());
        lookup.listTags().forEach(tag -> {
            Identifier id = tag.key().location();
            if (!report.wanted(id)) {
                return;
            }
            Set<String> values = new TreeSet<>();
            for (Holder<T> holder : tag) {
                holder.unwrapKey().ifPresent(key -> values.add(key.identifier().toString()));
            }
            JsonArray array = new JsonArray();
            values.forEach(array::add);
            JsonObject json = new JsonObject();
            json.add("values", array);
            try {
                report.json(id, folder, json);
                report.count("tags");
            } catch (IOException e) {
                report.fail("tag " + lookup.key().identifier() + " #" + id, String.valueOf(e.getMessage()));
            }
        });
    }

    private static <T> boolean write(Report report, Identifier id, String folder, Codec<T> codec, T value, RegistryOps<JsonElement> ops, String category) {
        String what = folder + " " + id;
        try {
            DataResult<JsonElement> result = codec.encodeStart(ops, value);
            Optional<JsonElement> json = result.result();
            if (json.isEmpty()) {
                report.fail(what, result.error().map(DataResult.Error::message).orElse("?"));
                return false;
            }
            report.json(id, folder, json.get());
            report.count(category);
            return true;
        } catch (Exception e) {
            report.fail(what, String.valueOf(e.getMessage()));
            return false;
        }
    }

    /** {@code pack.mcmeta} (this version's data pack format) and {@code export_report.json}. */
    private static void writePack(Report report, boolean integrated) throws IOException {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Apeira datapack export");
        pack.addProperty("pack_format", SharedConstants.DATA_PACK_FORMAT_MAJOR);
        JsonArray format = new JsonArray();
        format.add(SharedConstants.DATA_PACK_FORMAT_MAJOR);
        format.add(SharedConstants.DATA_PACK_FORMAT_MINOR);
        pack.add("min_format", format);
        pack.add("max_format", format);
        JsonObject mcmeta = new JsonObject();
        mcmeta.add("pack", pack);
        Files.writeString(report.root.resolve("pack.mcmeta"), GSON.toJson(mcmeta), StandardCharsets.UTF_8);

        JsonObject json = new JsonObject();
        json.addProperty("source", integrated ? "integrated_server" : "live_server");
        json.addProperty("namespace", report.namespace == null ? "*" : report.namespace);
        json.addProperty("files", report.files);
        json.add("counts", GSON.toJsonTree(report.counts));
        json.add("registries", GSON.toJsonTree(report.registries));
        json.add("skipped", GSON.toJsonTree(report.skipped));
        json.add("failed", GSON.toJsonTree(report.failed));
        Files.writeString(report.root.resolve("export_report.json"), GSON.toJson(json), StandardCharsets.UTF_8);
    }

    // The recording area as a structure template.

    private static int exportStructure(CommandContext<FabricClientCommandSource> context, Identifier id) {
        BlockPos[] area = ArmorStandRecorder.area();
        Minecraft minecraft = Minecraft.getInstance();
        if (area == null || minecraft.level == null) {
            context.getSource().sendError(Component.translatable("message.apeira.datapack.no_area"));
            return 0;
        }
        BlockPos size = area[1].subtract(area[0]).offset(1, 1, 1);
        try {
            String author = minecraft.player == null ? "?" : minecraft.player.getName().getString();
            StructureTemplate template = new StructureTemplate();
            template.setAuthor(author);
            CompoundTag tag;
            try {
                template.fillFromWorld(minecraft.level, area[0], size, true, List.of());
                tag = template.save(new CompoundTag());
            } catch (Exception e) {
                // An entity that cannot be saved client side: keep the blocks.
                UiCaptureClient.LOGGER.warn("Structure export with entities failed, saving blocks only", e);
                template = new StructureTemplate();
                template.setAuthor(author);
                template.fillFromWorld(minecraft.level, area[0], size, false, List.of());
                tag = template.save(new CompoundTag());
            }
            Report report = new Report(minecraft.gameDirectory.toPath().resolve(FOLDER), null);
            Path file = report.file(id, "structure", ".nbt");
            NbtIo.writeCompressed(tag, file);
            context.getSource().sendFeedback(Component.translatable("message.apeira.datapack.structure",
                    tag.getListOrEmpty("blocks").size(), tag.getListOrEmpty("entities").size(), id.toString(), file.toString()));
            return 1;
        } catch (Exception e) {
            UiCaptureClient.LOGGER.error("Structure export failed", e);
            context.getSource().sendError(Component.translatable("message.apeira.datapack.failed", String.valueOf(e.getMessage())));
            return 0;
        }
    }
}
