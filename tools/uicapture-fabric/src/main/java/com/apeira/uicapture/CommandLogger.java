package com.apeira.uicapture;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

/**
 * The command logger, a client command: {@code /apeiracommands} writes the command tree the server sent (every command
 * the player may run, with its subcommands, arguments and redirects) to {@code logs/apeira_command_capture.json}, and
 * {@code /apeiracommands <root>} also lists that command's usages in chat. Each root command is marked {@code vanilla}
 * (the client's own copy of the vanilla commands has it, also under {@code minecraft:}), {@code client} (a client
 * command of an installed mod, such as this one) or {@code server} (the server's plugins), and named after the plugin
 * namespace the server also sent it under ({@code essentials:pay}). Each has its usages, one per runnable path in the
 * style of {@code /apeiradev commands} (arguments as {@code <name>}, sibling literals ending a command as {@code a|b|c},
 * redirects as {@code -> target}), and its full tree: literals and arguments with their parser id and properties
 * ({@code brigadier:integer}, min and max), custom suggestions, whether the node runs and its redirect.
 *
 * <p>Plugin commands often reach the client as a single greedy argument that asks the server for completions
 * ({@code minecraft:ask_server}), so their subcommands are not in the tree. The server's answers are recorded
 * ({@code mixin/ClientSuggestionProviderMixin}): those the chat box gets while one types a command, and those of
 * {@code /apeiracommands probe [root]}, which asks the server to complete each server command (or one), then each
 * completion it gets, a few words deep, one request every {@link #PROBE_INTERVAL} ticks so servers' tab-complete spam
 * limits are not reached. The answers go in the log under {@code server_suggestions} (input, start of the completed word,
 * completions with their tooltips) and in {@code /apeiracommands <root>}'s chat lines. Probing sends only those
 * completion requests, as typing in the chat box does; nothing else is sent.
 */
public final class CommandLogger {
    static final String LOG = "logs/apeira_command_capture.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int MAX_DEPTH = 32;
    /** Probe request ids start here, far above the chat box's own (which count up from 0), so answers are told apart. */
    private static final int PROBE_ID_BASE = 0x40000000;
    /** Ticks between probe requests: Paper allows about one completion request a tick before it kicks for spam. */
    static final int PROBE_INTERVAL = 5;
    private static final int PROBE_TIMEOUT = 100;
    private static final int PROBE_MAX_REQUESTS = 400;
    /** How many words after the root command a probe completes. */
    private static final int PROBE_MAX_DEPTH = 4;
    /** How many of one answer's completions a probe follows; an answer that is mostly player names follows one. */
    private static final int PROBE_MAX_BRANCHES = 48;

    /** The server's completions by input ("/realm "), for the current connection. */
    private static final Map<String, JsonObject> SUGGESTIONS = new TreeMap<>();
    private static @Nullable ClientPacketListener suggestionsConnection;
    private static int chatRequest = -1;
    private static @Nullable String chatInput;

    private record Probe(String input, int depth) {}

    private static final ArrayDeque<Probe> PROBES = new ArrayDeque<>();
    private static final Set<String> PROBED = new HashSet<>();
    private static boolean probing;
    private static @Nullable Probe inFlight;
    private static int inFlightId;
    private static int nextProbeId = PROBE_ID_BASE;
    private static int waited;
    private static int cooldown;
    private static int sent;
    private static int answeredCount;
    private static int timeouts;

    private CommandLogger() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("apeiracommands")
                .executes(context -> log(context, null))
                .then(ClientCommandManager.literal("probe")
                        .executes(context -> probe(context, null))
                        .then(ClientCommandManager.argument("root", StringArgumentType.word())
                                .suggests(CommandLogger::suggestRoots)
                                .executes(context -> probe(context, StringArgumentType.getString(context, "root")))))
                .then(ClientCommandManager.argument("root", StringArgumentType.word())
                        .suggests(CommandLogger::suggestRoots)
                        .executes(context -> log(context, StringArgumentType.getString(context, "root")))));
    }

    private static CompletableFuture<Suggestions> suggestRoots(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.getCommands().getRoot().getChildren().forEach(node -> builder.suggest(node.getName()));
        }
        return builder.buildFuture();
    }

    private static int log(CommandContext<FabricClientCommandSource> context, @Nullable String only) {
        return write(only, context.getSource()::sendFeedback, context.getSource()::sendError);
    }

    private static int write(@Nullable String only, Consumer<Component> feedback, Consumer<Component> error) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return 0;
        }
        forgetOldConnection(connection);
        CommandDispatcher<?> server = connection.getCommands();
        Set<String> vanilla = vanillaRoots(connection);
        Set<String> client = new HashSet<>();
        CommandDispatcher<FabricClientCommandSource> clientCommands = ClientCommandManager.getActiveDispatcher();
        if (clientCommands != null) {
            clientCommands.getRoot().getChildren().forEach(node -> client.add(node.getName()));
        }

        List<CommandNode<?>> roots = new ArrayList<>(server.getRoot().getChildren());
        roots.sort(Comparator.comparing(CommandNode::getName));
        JsonArray commands = new JsonArray();
        int serverCount = 0;
        int usageCount = 0;
        List<String> shown = null;
        for (CommandNode<?> root : roots) {
            String name = root.getName();
            String plain = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
            String source = client.contains(name) ? "client"
                    : vanilla.contains(name) || (name.startsWith("minecraft:") && vanilla.contains(plain)) ? "vanilla" : "server";
            JsonObject command = new JsonObject();
            command.addProperty("name", name);
            command.addProperty("source", source);
            String namespace = namespace(name, roots);
            if (namespace != null && !source.equals("vanilla")) {
                command.addProperty("namespace", namespace);
            }
            List<String> usages = new ArrayList<>();
            walk(root, "/" + name, usages, 0);
            JsonArray usageArray = new JsonArray();
            usages.forEach(usageArray::add);
            command.add("usages", usageArray);
            command.add("tree", node(root, 0));
            commands.add(command);
            if (source.equals("server")) {
                serverCount++;
                usageCount += usages.size();
            }
            if (name.equals(only)) {
                shown = usages;
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("time", LocalDateTime.now().toString());
        root.addProperty("server", minecraft.getCurrentServer() == null ? "singleplayer" : minecraft.getCurrentServer().ip);
        root.addProperty("vanilla_known", !vanilla.isEmpty());
        root.add("commands", commands);
        JsonArray answers = new JsonArray();
        SUGGESTIONS.values().forEach(answers::add);
        root.add("server_suggestions", answers);
        Path file = FabricLoader.getInstance().getGameDir().resolve(LOG);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("Command log failed", e);
            error.accept(Component.translatable("message.apeira.command_logger.failed", String.valueOf(e.getMessage())));
            return 0;
        }
        UiCaptureClient.LOGGER.info("[command logger] {} commands ({} from the server, {} usages) written to {}", roots.size(), serverCount, usageCount, file);
        feedback.accept(Component.translatable("message.apeira.command_logger.done",
                roots.size(), serverCount, usageCount, file.getFileName().toString()).withStyle(ChatFormatting.GOLD));
        if (only != null) {
            if (shown == null) {
                error.accept(Component.translatable("message.apeira.command_logger.unknown", only));
                return 0;
            }
            List<String> lines = new ArrayList<>(shown);
            // The server's completions under the command, as "/realm invite|kick|ban".
            for (JsonObject answer : SUGGESTIONS.values()) {
                String input = answer.get("input").getAsString();
                if (input.startsWith("/" + only + " ") && answer.getAsJsonArray("suggestions").size() > 0) {
                    List<String> words = new ArrayList<>();
                    answer.getAsJsonArray("suggestions").forEach(suggestion -> words.add(suggestion.getAsJsonObject().get("text").getAsString()));
                    lines.add(input.substring(0, answer.get("start").getAsInt()) + String.join("|", words) + "  (server)");
                }
            }
            for (String usage : lines) {
                feedback.accept(Component.literal(usage).withStyle(ChatFormatting.GRAY));
                UiCaptureClient.LOGGER.info("[command logger] {}", usage);
            }
        }
        return roots.size();
    }

    private static int probe(CommandContext<FabricClientCommandSource> context, @Nullable String only) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return 0;
        }
        if (probing) {
            context.getSource().sendError(Component.translatable("message.apeira.command_logger.probe_busy"));
            return 0;
        }
        forgetOldConnection(connection);
        Set<String> skip = new HashSet<>(vanillaRoots(connection));
        CommandDispatcher<FabricClientCommandSource> clientCommands = ClientCommandManager.getActiveDispatcher();
        if (clientCommands != null) {
            clientCommands.getRoot().getChildren().forEach(node -> skip.add(node.getName()));
        }
        List<String> roots = new ArrayList<>();
        for (CommandNode<?> node : connection.getCommands().getRoot().getChildren()) {
            String name = node.getName();
            if (only != null ? name.equals(only) : !skip.contains(name) && !name.startsWith("minecraft:") && !node.getChildren().isEmpty()) {
                roots.add(name);
            }
        }
        if (roots.isEmpty()) {
            context.getSource().sendError(Component.translatable("message.apeira.command_logger.unknown", only == null ? "-" : only));
            return 0;
        }
        roots.sort(null);
        PROBES.clear();
        PROBED.clear();
        roots.forEach(root -> queue("/" + root + " ", 1));
        probing = true;
        inFlight = null;
        cooldown = 0;
        sent = 0;
        answeredCount = 0;
        timeouts = 0;
        context.getSource().sendFeedback(Component.translatable("message.apeira.command_logger.probe_start", roots.size(), PROBE_INTERVAL)
                .withStyle(ChatFormatting.GOLD));
        UiCaptureClient.LOGGER.info("[command logger] probing {} commands", roots.size());
        return roots.size();
    }

    private static void queue(String input, int depth) {
        if (PROBED.add(input)) {
            PROBES.add(new Probe(input, depth));
        }
    }

    /** Sends the next probe request, one every {@link #PROBE_INTERVAL} ticks, and writes the log when none are left. */
    static void tick(Minecraft minecraft) {
        if (!probing) {
            return;
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || connection != suggestionsConnection) {
            probing = false;
            PROBES.clear();
            inFlight = null;
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("message.apeira.command_logger.probe_stopped").withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        if (inFlight != null) {
            if (++waited < PROBE_TIMEOUT) {
                return;
            }
            timeouts++;
            inFlight = null;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        boolean limit = sent >= PROBE_MAX_REQUESTS;
        if (PROBES.isEmpty() || limit) {
            finishProbe(minecraft, limit && !PROBES.isEmpty());
            return;
        }
        inFlight = PROBES.poll();
        inFlightId = nextProbeId++;
        waited = 0;
        cooldown = PROBE_INTERVAL;
        sent++;
        connection.send(new ServerboundCommandSuggestionPacket(inFlightId, inFlight.input()));
    }

    private static void finishProbe(Minecraft minecraft, boolean limit) {
        probing = false;
        PROBES.clear();
        Consumer<Component> chat = component -> {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(component, false);
            }
        };
        write(null, component -> {}, chat);
        UiCaptureClient.LOGGER.info("[command logger] probe done: {} requests, {} answered, {} timed out", sent, answeredCount, timeouts);
        chat.accept(Component.translatable("message.apeira.command_logger.probe_done", sent, answeredCount, timeouts,
                limit ? Component.translatable("message.apeira.command_logger.probe_limit") : Component.empty(), LOG)
                .withStyle(ChatFormatting.GOLD));
    }

    /** Notes the chat box's completion request, which the client sends under {@code id}. */
    public static void requested(int id, String input) {
        forgetOldConnection(Minecraft.getInstance().getConnection());
        chatRequest = id;
        chatInput = input;
    }

    /**
     * Records the server's completions for a request; true for a probe's answer, which the chat box must not get (its
     * own pending request has another id, so it would ignore it anyway).
     */
    public static boolean answered(int id, Suggestions suggestions) {
        Minecraft minecraft = Minecraft.getInstance();
        forgetOldConnection(minecraft.getConnection());
        if (id >= PROBE_ID_BASE) {
            if (inFlight != null && id == inFlightId) {
                Probe probe = inFlight;
                inFlight = null;
                answeredCount++;
                record(probe.input(), suggestions);
                follow(minecraft, probe, suggestions);
            }
            return true;
        }
        if (id == chatRequest && chatInput != null) {
            record(chatInput, suggestions);
        }
        return false;
    }

    private static void record(String input, Suggestions suggestions) {
        JsonObject answer = new JsonObject();
        answer.addProperty("input", input);
        answer.addProperty("start", suggestions.getRange().getStart());
        JsonArray list = new JsonArray();
        for (Suggestion suggestion : suggestions.getList()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("text", suggestion.getText());
            if (suggestion.getTooltip() != null) {
                entry.addProperty("tooltip", suggestion.getTooltip().getString());
            }
            list.add(entry);
        }
        answer.add("suggestions", list);
        SUGGESTIONS.put(input, answer);
    }

    /** Queues each completion of a probe's answer, followed by a space, a word deeper. */
    private static void follow(Minecraft minecraft, Probe probe, Suggestions suggestions) {
        if (probe.depth() >= PROBE_MAX_DEPTH || suggestions.isEmpty()) {
            return;
        }
        int start = Math.min(suggestions.getRange().getStart(), probe.input().length());
        String prefix = probe.input().substring(0, start);
        List<String> texts = new ArrayList<>();
        suggestions.getList().forEach(suggestion -> texts.add(suggestion.getText()));
        Set<String> players = new HashSet<>();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                players.add(info.getProfile().name());
            }
        }
        long names = texts.stream().filter(players::contains).count();
        int branches = names * 2 >= texts.size() ? 1 : PROBE_MAX_BRANCHES;
        for (String text : texts.subList(0, Math.min(branches, texts.size()))) {
            if (!text.isBlank()) {
                queue(prefix + text + " ", probe.depth() + 1);
            }
        }
    }

    /** Drops what was recorded for an earlier connection. */
    private static void forgetOldConnection(@Nullable ClientPacketListener connection) {
        if (connection != suggestionsConnection) {
            suggestionsConnection = connection;
            SUGGESTIONS.clear();
            chatRequest = -1;
            chatInput = null;
        }
    }

    /** The vanilla root commands, from a copy of vanilla's dispatcher built on the client; empty if that fails. */
    private static Set<String> vanillaRoots(ClientPacketListener connection) {
        Set<String> names = new HashSet<>();
        try {
            Commands commands = new Commands(Commands.CommandSelection.ALL, Commands.createValidationContext(connection.registryAccess()));
            commands.getDispatcher().getRoot().getChildren().forEach(node -> names.add(node.getName()));
        } catch (Throwable e) {
            UiCaptureClient.LOGGER.warn("[command logger] could not build the vanilla commands, so none are marked vanilla", e);
        }
        return names;
    }

    /** The namespace a plain root command was also sent under ({@code pay} beside {@code essentials:pay}), or its own. */
    private static @Nullable String namespace(String name, List<CommandNode<?>> roots) {
        int colon = name.indexOf(':');
        if (colon > 0) {
            return name.substring(0, colon);
        }
        for (CommandNode<?> other : roots) {
            String otherName = other.getName();
            if (otherName.endsWith(":" + name) && !otherName.startsWith("minecraft:")) {
                return otherName.substring(0, otherName.length() - name.length() - 1);
            }
        }
        return null;
    }

    private static <S> void walk(CommandNode<S> node, String path, List<String> lines, int depth) {
        if (node.getRedirect() != null) {
            String target = node.getRedirect().getName();
            lines.add(path + " -> " + (target.isEmpty() ? "/..." : target));
            return;
        }
        if (node.getCommand() != null) {
            lines.add(path);
        }
        if (depth > MAX_DEPTH) {
            return;
        }
        List<String> endings = new ArrayList<>();
        for (CommandNode<S> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode<?> && child.getChildren().isEmpty() && child.getRedirect() == null
                    && child.getCommand() != null) {
                endings.add(child.getName());
            }
        }
        if (endings.size() > 1) {
            lines.add(path + " " + String.join("|", endings));
        }
        for (CommandNode<S> child : node.getChildren()) {
            if (endings.size() > 1 && endings.contains(child.getName()) && child instanceof LiteralCommandNode) {
                continue;
            }
            String name = child instanceof ArgumentCommandNode<?, ?> ? "<" + child.getName() + ">" : child.getName();
            walk(child, path + " " + name, lines, depth + 1);
        }
    }

    private static <S> JsonObject node(CommandNode<S> node, int depth) {
        JsonObject json = new JsonObject();
        json.addProperty("type", node instanceof ArgumentCommandNode<?, ?> ? "argument" : "literal");
        json.addProperty("name", node.getName());
        if (node instanceof ArgumentCommandNode<S, ?> argument) {
            parser(json, argument.getType());
            SuggestionProvider<S> suggestions = argument.getCustomSuggestions();
            if (suggestions != null) {
                json.addProperty("suggestions", suggestionName(suggestions));
            }
        }
        if (node.getCommand() != null) {
            json.addProperty("executable", true);
        }
        if (node.getRedirect() != null) {
            String target = node.getRedirect().getName();
            json.addProperty("redirect", target.isEmpty() ? "/" : target);
        }
        if (!node.getChildren().isEmpty() && depth <= MAX_DEPTH) {
            JsonArray children = new JsonArray();
            node.getChildren().forEach(child -> children.add(node(child, depth + 1)));
            json.add("children", children);
        }
        return json;
    }

    /** The argument's parser id and properties, as vanilla's command report writes them. */
    private static <T extends ArgumentType<?>> void parser(JsonObject json, T type) {
        try {
            ArgumentTypeInfo.Template<T> template = ArgumentTypeInfos.unpack(type);
            json.addProperty("parser", String.valueOf(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(template.type())));
            JsonObject properties = new JsonObject();
            properties(template, properties);
            if (properties.size() > 0) {
                json.add("properties", properties);
            }
        } catch (RuntimeException e) {
            json.addProperty("parser", type.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void properties(T template, JsonObject json) {
        ((ArgumentTypeInfo<A, T>) template.type()).serializeToJson(template, json);
    }

    /** The id the server named a suggestion provider by ({@code minecraft:ask_server}), read from its wrapper record. */
    private static String suggestionName(SuggestionProvider<?> provider) {
        Class<?> type = provider.getClass();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (component.getName().equals("name")) {
                    try {
                        component.getAccessor().setAccessible(true);
                        return String.valueOf(component.getAccessor().invoke(provider));
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        break;
                    }
                }
            }
        }
        return "custom";
    }
}
