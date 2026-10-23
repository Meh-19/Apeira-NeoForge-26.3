package com.apeira.uicapture;

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.uicapture.mixin.BossHealthOverlayAccessor;
import com.apeira.uicapture.mixin.GuiAccessor;
import com.apeira.uicapture.mixin.PlayerTabOverlayAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * The HUD log's {@code hud} section (F10): what the HUD shows as text rather than as drawn sprites, each as a component
 * with its runs (colours, formatting, fonts): the boss bars (name, colour, overlay, progress), the action bar message,
 * the title and subtitle, the scoreboard sidebar as it is drawn, the player list's header and footer, and the selected
 * hotbar slot. It is collected while the frame records, so the fonts it uses are described with the drawn ones.
 */
final class HudLog {
    private HudLog() {}

    static JsonObject collect(Minecraft minecraft, DynamicOps<JsonElement> ops) {
        JsonObject hud = new JsonObject();
        JsonArray bosses = new JsonArray();
        for (LerpingBossEvent event : ((BossHealthOverlayAccessor) minecraft.gui.getBossOverlay()).apeira$events().values()) {
            JsonObject boss = new JsonObject();
            boss.addProperty("id", event.getId().toString());
            boss.add("name", UiCaptureWriter.component(event.getName(), ops));
            boss.addProperty("color", event.getColor().getName());
            boss.addProperty("overlay", event.getOverlay().getName());
            boss.addProperty("progress", event.getProgress());
            boss.addProperty("darken_screen", event.shouldDarkenScreen());
            boss.addProperty("create_world_fog", event.shouldCreateWorldFog());
            bosses.add(boss);
        }
        hud.add("boss_bars", bosses);

        GuiAccessor gui = (GuiAccessor) minecraft.gui;
        if (gui.apeira$overlayMessageTime() > 0) {
            add(hud, "action_bar", gui.apeira$overlayMessage(), ops);
        }
        if (gui.apeira$titleTime() > 0) {
            add(hud, "title", gui.apeira$title(), ops);
            add(hud, "subtitle", gui.apeira$subtitle(), ops);
        }
        PlayerTabOverlayAccessor tab = (PlayerTabOverlayAccessor) minecraft.gui.getTabList();
        add(hud, "tab_header", tab.apeira$header(), ops);
        add(hud, "tab_footer", tab.apeira$footer(), ops);

        if (minecraft.level != null) {
            Scoreboard scoreboard = minecraft.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective != null) {
                hud.add("sidebar", sidebar(scoreboard, objective, ops));
            }
        }
        if (minecraft.player != null) {
            hud.addProperty("selected_slot", minecraft.player.getInventory().getSelectedSlot());
        }
        return hud;
    }

    /** The sidebar as vanilla draws it: the title, then up to 15 scores, highest first. */
    private static JsonObject sidebar(Scoreboard scoreboard, Objective objective, DynamicOps<JsonElement> ops) {
        JsonObject sidebar = new JsonObject();
        sidebar.addProperty("objective", objective.getName());
        sidebar.add("title", UiCaptureWriter.component(objective.getDisplayName(), ops));
        List<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .limit(15)
                .toList();
        JsonArray lines = new JsonArray();
        for (PlayerScoreEntry entry : entries) {
            JsonObject line = new JsonObject();
            line.addProperty("owner", entry.owner());
            line.add("name", UiCaptureWriter.component(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()), ops));
            line.addProperty("value", entry.value());
            line.add("score", UiCaptureWriter.component(entry.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)), ops));
            lines.add(line);
        }
        sidebar.add("lines", lines);
        return sidebar;
    }

    private static void add(JsonObject hud, String key, @Nullable Component component, DynamicOps<JsonElement> ops) {
        if (component != null) {
            hud.add(key, UiCaptureWriter.component(component, ops));
        }
    }
}
