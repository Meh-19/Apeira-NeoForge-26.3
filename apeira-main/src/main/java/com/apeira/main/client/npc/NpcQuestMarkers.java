package com.apeira.main.client.npc;

import java.util.List;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.npc.NpcEntity;
import com.apeira.main.registry.MainNpcs;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Question marks on the locator bar for the NPCs within {@link #RANGE} blocks that the player has not talked to yet
 * (by type, {@code apeira:met_npc_types}, {@link MainNpcs#MET_NPCS}, so they stay met through realm resets), so they can find Jeckyl, Jacko and the conductors. Each is Ren's
 * {@code textures/gui/npc/quest_marker.png} placed as vanilla's locator bar places a waypoint's dot: within 60 degrees
 * either side of the view, across the bar's 173 pixels. While a mark shows, the locator bar's background is drawn too, as
 * vanilla draws it whenever it has a waypoint. Hidden NPCs get no mark. A GUI layer above the contextual info bar.
 */
public final class NpcQuestMarkers {
    private static final Identifier MARKER = ApeiraIds.id("textures/gui/npc/quest_marker.png");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
    private static final int MARKER_WIDTH = 8;
    private static final int MARKER_HEIGHT = 11;
    /** How far an NPC may be, in blocks, and still get a mark. */
    private static final double RANGE = 50.0;
    /** Vanilla's locator bar: the view angle it shows either side and the dots' spread across it. */
    private static final double VISIBLE_DEGREES = 60.0;
    private static final double SPREAD = 173.0;
    private static final int DOT_SIZE = 9;

    private NpcQuestMarkers() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (minecraft.player == null || minecraft.level == null || camera == null || minecraft.gui.hud.isHidden()
                || minecraft.player.isSpectator()) {
            return;
        }
        List<Identifier> met = minecraft.player.getData(MainNpcs.MET_NPCS);
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        Vec3 eye = camera.getEyePosition(partialTick);
        float yaw = camera.getViewYRot(partialTick);
        List<NpcEntity> npcs = minecraft.level.getEntitiesOfClass(NpcEntity.class, camera.getBoundingBox().inflate(RANGE),
                npc -> npc.isShown() && !met.contains(BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType())) && npc.position().distanceToSqr(eye) <= RANGE * RANGE);
        if (npcs.isEmpty()) {
            return;
        }
        int left = (graphics.guiWidth() - ContextualBar.WIDTH) / 2;
        int top = graphics.guiHeight() - ContextualBar.MARGIN_BOTTOM - ContextualBar.HEIGHT;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, ContextualBar.WIDTH, ContextualBar.HEIGHT);
        int middle = Mth.ceil((graphics.guiWidth() - DOT_SIZE) / 2.0F);
        for (NpcEntity npc : npcs) {
            Vec3 to = npc.getPosition(partialTick).subtract(eye);
            float bearing = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
            double angle = Mth.degreesDifference(yaw, bearing);
            if (angle <= -VISIBLE_DEGREES || angle > VISIBLE_DEGREES) {
                continue;
            }
            int x = middle + Mth.floor(angle * SPREAD / 2.0 / VISIBLE_DEGREES) + (DOT_SIZE - MARKER_WIDTH) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MARKER, x, top - 3, 0.0F, 0.0F, MARKER_WIDTH, MARKER_HEIGHT, MARKER_WIDTH, MARKER_HEIGHT);
        }
    }
}
