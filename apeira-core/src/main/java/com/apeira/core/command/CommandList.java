package com.apeira.core.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Lists the commands mods added to the server, for {@code /apeiradev commands}. {@link #snapshot} runs first on
 * {@link RegisterCommandsEvent} (register it with the highest priority), when the dispatcher holds only vanilla's
 * commands; {@link #usages} then gives every other root command's usages, one per runnable path, with arguments as
 * {@code <name>} (never their suggestions, such as player names), sibling literals that end a command joined as
 * {@code a|b|c}, and redirects as {@code -> target}.
 */
public final class CommandList {
    private static final Set<String> VANILLA = new HashSet<>();

    private CommandList() {}

    public static void snapshot(RegisterCommandsEvent event) {
        VANILLA.clear();
        event.getDispatcher().getRoot().getChildren().forEach(node -> VANILLA.add(node.getName()));
    }

    /** Each non-vanilla root command, alphabetically, with its usages ({@code /realm invite <player>}). */
    public static Map<String, List<String>> usages(CommandDispatcher<CommandSourceStack> dispatcher) {
        Map<String, List<String>> usages = new LinkedHashMap<>();
        dispatcher.getRoot().getChildren().stream()
                .filter(node -> !VANILLA.contains(node.getName()))
                .sorted(Comparator.comparing(CommandNode::getName))
                .forEach(node -> {
                    List<String> lines = new ArrayList<>();
                    walk(node, "/" + node.getName(), lines, 0);
                    usages.put(node.getName(), lines);
                });
        return usages;
    }

    private static void walk(CommandNode<CommandSourceStack> node, String path, List<String> lines, int depth) {
        if (node.getRedirect() != null) {
            String target = node.getRedirect().getName();
            lines.add(path + " -> " + (target.isEmpty() ? "/..." : target));
            return;
        }
        if (node.getCommand() != null) {
            lines.add(path);
        }
        if (depth > 16) {
            return;
        }
        List<String> endings = new ArrayList<>();
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode<?> && child.getChildren().isEmpty() && child.getRedirect() == null
                    && child.getCommand() != null) {
                endings.add(child.getName());
            }
        }
        if (endings.size() > 1) {
            lines.add(path + " " + String.join("|", endings));
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (endings.size() > 1 && endings.contains(child.getName()) && child instanceof LiteralCommandNode) {
                continue;
            }
            String name = child instanceof ArgumentCommandNode<?, ?> ? "<" + child.getName() + ">" : child.getName();
            walk(child, path + " " + name, lines, depth + 1);
        }
    }
}
