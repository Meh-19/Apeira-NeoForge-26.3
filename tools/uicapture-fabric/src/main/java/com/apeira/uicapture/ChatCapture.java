package com.apeira.uicapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.apeira.uicapture.mixin.ChatComponentAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The chat logger (F7 toggles it): while on, every message the chat receives is appended to
 * {@code logs/apeira_chat_capture.jsonl}, one JSON object per line: its plain text, its full component JSON (click and
 * hover events, fonts) and its runs of one font and style with exact colours ({@code #RRGGBB}) and formatting, whether
 * it was signed, and its tag (the indicator colour and label of system, modified or unsigned messages). Turning it on
 * first writes the chat history, oldest first, marked {@code "history": true}; turning it off describes every font the
 * logged messages used, glyph by glyph, in {@code logs/apeira_chat_capture_fonts.json}, with the font bitmaps copied
 * as for UI captures.
 */
public final class ChatCapture {
    static final String LOG = "logs/apeira_chat_capture.jsonl";
    static final String FONTS = "logs/apeira_chat_capture_fonts.json";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static boolean logging;
    private static int logged;
    private static Map<Identifier, TreeSet<Integer>> glyphs = new LinkedHashMap<>();

    private ChatCapture() {}

    /** Turns the logger on or off and says so in chat (after the history is written, so the line itself is not logged). */
    static void toggle(Minecraft minecraft) {
        Path game = FabricLoader.getInstance().getGameDir();
        Component message;
        try {
            if (!logging) {
                logging = true;
                logged = 0;
                glyphs = new LinkedHashMap<>();
                JsonObject start = new JsonObject();
                start.addProperty("session_start", LocalDateTime.now().toString());
                if (minecraft.getCurrentServer() != null) {
                    start.addProperty("server", minecraft.getCurrentServer().ip);
                }
                append(start);
                List<GuiMessage> history = ((ChatComponentAccessor) minecraft.gui.getChat()).apeira$allMessages();
                for (GuiMessage line : history.reversed()) {
                    JsonObject entry = entry(minecraft, line.content(), line.signature() != null, line.tag());
                    entry.addProperty("history", true);
                    append(entry);
                }
                message = Component.translatable("message.apeira.ui_capture.chat_on", LOG);
            } else {
                logging = false;
                writeFonts(minecraft, game);
                message = Component.translatable("message.apeira.ui_capture.chat_off", logged, FONTS);
            }
        } catch (IOException e) {
            logging = false;
            UiCaptureClient.LOGGER.error("Chat logger failed", e);
            message = Component.translatable("message.apeira.ui_capture.failed", String.valueOf(e.getMessage()));
        }
        boolean wasLogging = logging;
        logging = false;
        minecraft.gui.getChat().addMessage(message);
        logging = wasLogging;
    }

    /** Called for every message added to the chat. */
    public static void onMessage(Component message, boolean signed, @Nullable GuiMessageTag tag) {
        if (!logging) {
            return;
        }
        try {
            append(entry(Minecraft.getInstance(), message, signed, tag));
        } catch (IOException e) {
            logging = false;
            UiCaptureClient.LOGGER.error("Chat logger failed, turned off", e);
        }
    }

    private static JsonObject entry(Minecraft minecraft, Component message, boolean signed, @Nullable GuiMessageTag tag) {
        JsonObject entry = UiCaptureWriter.component(message, UiCaptureWriter.ops(minecraft));
        entry.addProperty("time", LocalDateTime.now().toString());
        entry.addProperty("signed", signed);
        if (tag != null) {
            JsonObject tagJson = new JsonObject();
            tagJson.addProperty("indicator_color", String.format("#%06X", tag.indicatorColor() & 0xFFFFFF));
            if (tag.text() != null) {
                tagJson.addProperty("text", tag.text().getString());
            }
            if (tag.logTag() != null) {
                tagJson.addProperty("log_tag", tag.logTag());
            }
            entry.add("tag", tagJson);
        }
        entry.addProperty("width", minecraft.font.width(message));
        message.getVisualOrderText().accept((index, style, codepoint) -> {
            if (style.getFont() instanceof FontDescription.Resource resource) {
                glyphs.computeIfAbsent(resource.id(), id -> new TreeSet<>()).add(codepoint);
            }
            return true;
        });
        logged++;
        return entry;
    }

    private static void append(JsonObject entry) throws IOException {
        Path file = FabricLoader.getInstance().getGameDir().resolve(LOG);
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(entry) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void writeFonts(Minecraft minecraft, Path game) throws IOException {
        ResourceManager resources = minecraft.getResourceManager();
        CapturedAssets assets = new CapturedAssets(resources, game.resolve(UiCaptureWriter.FOLDER));
        JsonObject fonts = new JsonObject();
        for (Map.Entry<Identifier, TreeSet<Integer>> font : glyphs.entrySet()) {
            fonts.add(font.getKey().toString(), FontGlyphs.describe(resources, font.getKey(), font.getValue(), assets));
        }
        JsonObject root = new JsonObject();
        root.addProperty("written_at", LocalDateTime.now().toString());
        root.addProperty("messages", logged);
        root.add("fonts", fonts);
        root.add("assets", assets.index());
        Files.writeString(game.resolve(FONTS), PRETTY.toJson(root), StandardCharsets.UTF_8);
    }
}
