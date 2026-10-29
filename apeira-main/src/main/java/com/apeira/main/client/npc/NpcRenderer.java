package com.apeira.main.client.npc;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcEntity;
import com.apeira.main.registry.MainItems;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws an {@link NpcEntity} with GeckoLib, from {@code geckolib/models/entity/<model>.geo.json} and
 * {@code geckolib/animations/entity/<model>.animation.json} with its own {@code textures/entity/<texture>.png}: the
 * model's origin at the entity's feet, its front (Blockbench's north) turned to the entity's facing, looping the
 * entity's idle animation. It is drawn with the emissive translucent entity type, so alpha 254 texels glow. A hidden NPC
 * is not drawn at all until the viewer comes within {@link #GHOST_REACH} block of it; then it fades in as a faint ghost
 * ({@link #GHOST_ALPHA}) over {@link #GHOST_FADE_MS}, and fades out again when they step away. A moved NPC (the anchor
 * tool's teleport) is drawn at its new spot at once.
 *
 * <p>Two of the model's locators are followed through GeckoLib's locator position listeners, which give where each is
 * posed as the model is drawn: {@link #HEAD_LOCATOR}, which the dialogue camera looks at ({@link #head}), and
 * {@link #CHAT_LOCATOR} (the text display Origin Realms showed over the NPC), which carries a speech bubble, always
 * shown: the first of the six 16x16 frames of {@code textures/custom/ui/speech_bubble.png}, a {@link #BUBBLE_SIZE}-block
 * billboard always facing the camera, centred across with its bottom edge on the locator. It is full bright: its texels
 * are alpha 254 (emissive, section 5) and its vertices carry full-bright light, so it stays pure white in the dark. Both
 * are where the model was drawn last frame, as GeckoLib draws after the entities are submitted.
 *
 * <p>Each NPC's renderer names its own two locators and its {@link Marker}: the conductors use {@link #HEAD_LOCATOR} and
 * {@link #CHAT_LOCATOR} with the bubble; Jeckyl and Jacko wear {@link Marker#RUBY} instead, the ruby item
 * ({@code apeira:ruby}) floating over the locator, turning and bobbing, drawn at full brightness in its ground display.
 */
public class NpcRenderer extends GeoEntityRenderer<NpcEntity, EntityRenderState> {
    /** The ghost's alpha, for a hidden NPC seen up close. */
    private static final int GHOST_ALPHA = 0x60;
    /** How near, in blocks between the viewer's and the NPC's bounding boxes, a hidden NPC shows as a ghost. */
    private static final double GHOST_REACH = 1.0;
    /** How long a hidden NPC's ghost takes to fade fully in or out. */
    private static final float GHOST_FADE_MS = 300.0F;
    /** The locator the dialogue camera looks at. */
    public static final String HEAD_LOCATOR = "balloon_conductor_head";
    /** The locator the speech bubble stands on. */
    public static final String CHAT_LOCATOR = "chat_display";
    private static final Identifier BUBBLE = Apeira.REGISTRAR.id("textures/custom/ui/speech_bubble.png");
    /** The bubble texture's frames, stacked top to bottom; the first is drawn. */
    private static final int BUBBLE_FRAMES = 6;
    /** The bubble's width and height, in blocks. */
    private static final float BUBBLE_SIZE = 0.5F;
    /** Each NPC's ghost fade and last drawn locators. */
    private static final Map<NpcEntity, Track> TRACKS = new WeakHashMap<>();

    private static final DataTicket<Track> TRACK = DataTicket.create("apeira_npc_track", Track.class);
    private static final DataTicket<Integer> COLOR = DataTicket.create("apeira_npc_color", Integer.class);
    /** The ruby marker's size (its ground display is a quarter block), lift over the locator, bob and turn speed. */
    private static final float RUBY_SCALE = 1.6F;
    private static final float RUBY_LIFT = 0.25F;
    private static final float RUBY_BOB = 0.05F;
    private static final float RUBY_DEGREES_PER_TICK = 3.0F;

    /** What floats over the NPC's head: Origin Realms' speech bubble, or the ruby of the NPCs who sell for rubies. */
    public enum Marker {
        BUBBLE,
        RUBY
    }

    private final String headLocator;
    private final String markerLocator;
    private final Marker marker;
    private final ItemModelResolver itemModelResolver;

    private static final class Track {
        /** The ghost's fade, 0 (gone) to 1 (at {@link #GHOST_ALPHA}), and when it was last moved. */
        float ghost;
        long ghostMillis = Util.getMillis();
        /** Whether the NPC is hidden, so drawn only as the ghost, and whether the ghost has faded out entirely. */
        boolean ghostMode;
        boolean hidden;
        /** The locators' last drawn positions, in blocks from the entity's position; null until first drawn. */
        @Nullable Vec3 head;
        @Nullable Vec3 chat;
        /** The ruby marker, resolved each frame for {@link Marker#RUBY}. */
        final ItemStackRenderState ruby = new ItemStackRenderState();
    }

    public NpcRenderer(EntityRendererProvider.Context context, Identifier model, Identifier texture) {
        this(context, model, texture, HEAD_LOCATOR, CHAT_LOCATOR, Marker.BUBBLE);
    }

    /**
     * A renderer whose camera follows {@code headLocator} and whose {@code marker} floats at {@code markerLocator}, both
     * locators of the model.
     */
    public NpcRenderer(EntityRendererProvider.Context context, Identifier model, Identifier texture, String headLocator, String markerLocator,
            Marker marker) {
        super(context, new DefaultedEntityGeoModel<NpcEntity>(model).withAltTexture(texture));
        this.shadowRadius = 0.5F;
        this.headLocator = headLocator;
        this.markerLocator = markerLocator;
        this.marker = marker;
        this.itemModelResolver = context.getItemModelResolver();
    }

    /**
     * Registers {@code type}'s renderer: {@code model} names its model and animation files and {@code texture} its
     * texture, both as {@code <namespace>:<path>} under GeckoLib's {@code entity} folders.
     */
    public static void register(EntityRenderersEvent.RegisterRenderers event, EntityType<? extends NpcEntity> type, Identifier model,
            Identifier texture) {
        event.registerEntityRenderer(type, context -> new NpcRenderer(context, model, texture));
    }

    /**
     * Where the NPC's head locator was drawn last frame, in world coordinates: posed by its animation and turned with the
     * NPC. Its eyes until it has been drawn.
     */
    public static Vec3 head(NpcEntity npc, float partialTick) {
        Track track = TRACKS.get(npc);
        return track == null || track.head == null ? npc.getEyePosition(partialTick) : npc.getPosition(partialTick).add(track.head);
    }

    /** Moves the ghost fade towards shown or gone, by the time since it last moved. */
    private static Track track(NpcEntity npc) {
        Track track = TRACKS.computeIfAbsent(npc, entity -> new Track());
        long now = Util.getMillis();
        boolean ghost = !npc.isShown();
        Player viewer = Minecraft.getInstance().player;
        boolean near = ghost && viewer != null && viewer.getBoundingBox().distanceToSqr(npc.getBoundingBox()) <= GHOST_REACH * GHOST_REACH;
        float step = (now - track.ghostMillis) / GHOST_FADE_MS;
        track.ghost = Mth.clamp(track.ghost + (near ? step : -step), 0.0F, 1.0F);
        track.ghostMillis = now;
        track.ghostMode = ghost;
        track.hidden = ghost && track.ghost <= 0.0F;
        return track;
    }

    private static int color(NpcEntity npc, Track track) {
        return npc.isShown() ? 0xFFFFFFFF : Math.round(GHOST_ALPHA * track.ghost) << 24 | 0xFFFFFF;
    }

    @Override
    public void addRenderData(NpcEntity npc, Void relatedObject, EntityRenderState renderState, float partialTick) {
        Track track = track(npc);
        renderState.addGeckolibData(TRACK, track);
        renderState.addGeckolibData(COLOR, color(npc, track));
        if (marker == Marker.RUBY) {
            itemModelResolver.updateForTopItem(track.ruby, new ItemStack(MainItems.RUBY.get()), ItemDisplayContext.GROUND, npc.level(), null,
                    npc.getId());
        }
    }

    @Override
    public int getRenderColor(NpcEntity npc, Void relatedObject, float partialTick) {
        return color(npc, TRACKS.computeIfAbsent(npc, entity -> new Track()));
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return EmissiveRenderTypes.entityTranslucent(texture);
    }

    /** Listens for where the head and chat locators are posed as GeckoLib draws the model. */
    @Override
    public void preRenderPass(RenderPassInfo<EntityRenderState> renderPassInfo, SubmitNodeCollector collector) {
        super.preRenderPass(renderPassInfo, collector);
        Track track = renderPassInfo.getGeckolibData(TRACK);
        if (track != null) {
            renderPassInfo.addLocatorPositionListener(headLocator, (world, model, local) -> track.head = local);
            renderPassInfo.addLocatorPositionListener(markerLocator, (world, model, local) -> track.chat = local);
        }
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Track track = state.getGeckolibData(TRACK);
        if (track != null && track.hidden) {
            return;
        }
        super.submit(state, poseStack, collector, camera);
        if (track != null && track.chat != null) {
            poseStack.pushPose();
            poseStack.translate(track.chat.x, track.chat.y, track.chat.z);
            if (marker == Marker.BUBBLE) {
                bubble(poseStack, collector, camera.orientation, state.getOrDefaultGeckolibData(COLOR, 0xFFFFFFFF));
            } else if (!track.ghostMode && !track.ruby.isEmpty()) {
                ruby(poseStack, collector, track.ruby, state.ageInTicks);
            }
            poseStack.popPose();
        }
    }

    /** The ruby marker over the pose's origin: lifted, bobbing and turning, at full brightness. */
    private static void ruby(PoseStack poseStack, SubmitNodeCollector collector, ItemStackRenderState ruby, float ticks) {
        poseStack.translate(0.0F, RUBY_LIFT + Mth.sin(ticks * 0.1F) * RUBY_BOB, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(ticks * RUBY_DEGREES_PER_TICK));
        poseStack.scale(RUBY_SCALE, RUBY_SCALE, RUBY_SCALE);
        ruby.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    /**
     * The speech bubble's first frame: a square at the pose's origin, turned to face the camera (as name tags are), its
     * bottom edge on the origin, drawn from both sides.
     */
    private static void bubble(PoseStack poseStack, SubmitNodeCollector collector, Quaternionf facing, int color) {
        Vector3f anchor = poseStack.last().pose().getTranslation(new Vector3f());
        poseStack.pushPose();
        poseStack.last().pose().translation(anchor).rotate(facing);
        poseStack.last().normal().rotation(facing);
        float half = BUBBLE_SIZE / 2.0F;
        float v = 1.0F / BUBBLE_FRAMES;
        collector.submitCustomGeometry(poseStack, EmissiveRenderTypes.entityTranslucent(BUBBLE), (pose, buffer) -> {
            bubbleVertex(buffer, pose, -half, 0.0F, 0.0F, v, color);
            bubbleVertex(buffer, pose, half, 0.0F, 1.0F, v, color);
            bubbleVertex(buffer, pose, half, BUBBLE_SIZE, 1.0F, 0.0F, color);
            bubbleVertex(buffer, pose, -half, BUBBLE_SIZE, 0.0F, 0.0F, color);
        });
        poseStack.popPose();
    }

    private static void bubbleVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    /** The animation lifts and swings the model past its collision box and the bubble floats over it, so it is culled by a larger box. */
    @Override
    protected AABB getBoundingBoxForCulling(NpcEntity entity, float partialTicks) {
        return entity.getBoundingBox().inflate(1.0, 2.5, 1.0);
    }

    @Override
    protected float getShadowRadius(EntityRenderState state) {
        Track track = state.getGeckolibData(TRACK);
        return track != null && track.ghostMode ? 0.0F : super.getShadowRadius(state);
    }
}
