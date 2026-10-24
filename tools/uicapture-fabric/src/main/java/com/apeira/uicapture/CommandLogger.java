package com.apeira.uicapture;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
import net.minecraft.network.chat.Component;

/**
 * The command logger, a client command: {@code /apeiracommands} writes the command tree the server sent (every command
 * the player may run, with its subcommands, arguments and redirects) to {@code logs/apeira_command_capture.json}, and
 * {@code /apeiracommands <root>} also lists that command's usages in chat. Each root command is marked {@code vanilla}
 * (the client's own copy of the vanilla commands has it, also under {@code minecraft:}), {@code client} (a client
 * command of an installed mod, such as this one) or {@code server} (the server's plugins), and named after the plugin
 * namespace the server also sent it under ({@code essentials:pay}). Each has its usages, one per runnable path in the
 * style of {@code /apeiradev commands} (arguments as {@code <name>}, sibling literals ending a command as {@code a|b|c},
 * redirects as {@code -> target}), and its full tree: literals and arguments with their parser id and properties
 * ({@code brigadier:integer}, min and max), custom suggestions, whether the node runs and its redirect. Nothing is sent.
 */
public final class CommandLogger {
    static final String LOG = "logs/apeira_command_capture.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int MAX_DEPTH = 32;

    private CommandLogger() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("apeiracommands")
                .executes(context -> log(context, null))
                .then(ClientCommandManager.argument("root", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            ClientPacketListener connection = Minecraft.getInstance().getConnection();
                            if (connection != null) {
                                connection.getCommands().getRoot().getChildren().forEach(node -> builder.suggest(node.getName()));
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> log(context, StringArgumentType.getString(context, "root")))));
    }

    private static int log(CommandContext<FabricClientCommandSource> context, @Nullable String only) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return 0;
        }
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
        Path file = FabricLoader.getInstance().getGameDir().resolve(LOG);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("Command log failed", e);
            context.getSource().sendError(Component.translatable("message.apeira.command_logger.failed", String.valueOf(e.getMessage())));
            return 0;
        }
        UiCaptureClient.LOGGER.info("[command logger] {} commands ({} from the server, {} usages) written to {}", roots.size(), serverCount, usageCount, file);
        context.getSource().sendFeedback(Component.translatable("message.apeira.command_logger.done",
                roots.size(), serverCount, usageCount, file.getFileName().toString()).withStyle(ChatFormatting.GOLD));
        if (only != null) {
            if (shown == null) {
                context.getSource().sendError(Component.translatable("message.apeira.command_logger.unknown", only));
                return 0;
            }
            for (String usage : shown) {
                context.getSource().sendFeedback(Component.literal(usage).withStyle(ChatFormatting.GRAY));
                UiCaptureClient.LOGGER.info("[command logger] {}", usage);
            }
        }
        return roots.size();
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
