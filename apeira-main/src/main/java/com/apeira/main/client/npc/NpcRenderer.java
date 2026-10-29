package com.apeira.main.client.npc;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
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
 * <p>Each NPC's renderer names its head locator and its {@link Marker}: the conductors use {@link #HEAD_LOCATOR} with the
 * bubble on {@link #CHAT_LOCATOR}; Jeckyl and Jacko wear {@link Marker#RUBY} instead, a static ruby icon
 * ({@code textures/custom/ui/ruby_marker.png}, the ruby item's texture with its opaque texels at alpha 254) drawn
 * exactly as the bubble is, a flat full-bright billboard always facing the camera, anchored to the head locator
 * {@link #RUBY_LIFT} block up (where the text display stood over the head), so it follows the head's animation. It does
 * not turn or bob, and is {@link #RUBY_SIZE} across, 1.5 times smaller than the bubble.
 *
 * <p>The head bone (named as the head locator) turns to the nearest player within {@link #LOOK_RANGE} blocks, on top of
 * the animation, up to {@link #MAX_LOOK_YAW} degrees sideways and {@link #MAX_LOOK_PITCH} up or down, and back to the
 * animation's pose when nobody is near; both ways it eases over about {@link #LOOK_EASE_MS} ms. Jeckyl's and Jacko's
 * head bones carry Blockbench's half-turn ({@code [-180, y, 180]}), which turns their yaw the other way, so their
 * renderers pass {@code headYawSign} -1.
 *
 * <p>An NPC with a night texture (Jacko's {@code jacko_night}) wears it from 7 PM to 5 AM ({@link #isNight}).
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
    private static final Identifier BUBBLE_TEXTURE = Apeira.REGISTRAR.id("textures/custom/ui/speech_bubble.png");
    /** The bubble texture's frames, stacked top to bottom; the first is drawn. */
    private static final int BUBBLE_FRAMES = 6;
    /** The bubble's width and height, in blocks. */
    private static final float BUBBLE_SIZE = 0.5F;
    /** Each NPC's ghost fade and last drawn locators. */
    private static final Map<NpcEntity, Track> TRACKS = new WeakHashMap<>();

    private static final DataTicket<Track> TRACK = DataTicket.create("apeira_npc_track", Track.class);
    private static final DataTicket<Integer> COLOR = DataTicket.create("apeira_npc_color", Integer.class);
    private static final Identifier RUBY_TEXTURE = Apeira.REGISTRAR.id("textures/custom/ui/ruby_marker.png");
    /** How far over the head locator the ruby stands, in blocks: the models' text display, 16 pixels over the head. */
    private static final float RUBY_LIFT = 1.0F;
    /** The ruby's width and height, in blocks: the bubble's over 1.5. */
    private static final float RUBY_SIZE = BUBBLE_SIZE / 1.5F;
    private static final DataTicket<Float> LOOK_YAW = DataTicket.create("apeira_npc_look_yaw", Float.class);
    private static final DataTicket<Float> LOOK_PITCH = DataTicket.create("apeira_npc_look_pitch", Float.class);
    private static final DataTicket<Boolean> NIGHT = DataTicket.create("apeira_npc_night", Boolean.class);
    /** How near, in blocks, a player must be for the head to turn to them. */
    private static final double LOOK_RANGE = 8.0;
    private static final float MAX_LOOK_YAW = 70.0F;
    private static final float MAX_LOOK_PITCH = 35.0F;
    /** The head's turn closes {@code 1 - e^(-dt / LOOK_EASE_MS)} of the gap each frame. */
    private static final float LOOK_EASE_MS = 150.0F;
    /** Night, for night textures: day time 13000 (7 PM) to 23000 (5 AM). */
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    /**
     * What floats over the NPC's head, a square billboard: Origin Realms' speech bubble (the first of
     * {@code frames} frames of its texture), or the ruby of the NPCs who sell for rubies.
     */
    public enum Marker {
        BUBBLE(BUBBLE_TEXTURE, BUBBLE_FRAMES, BUBBLE_SIZE, 0.0F),
        RUBY(RUBY_TEXTURE, 1, RUBY_SIZE, RUBY_LIFT);

        final Identifier texture;
        final int frames;
        /** Its width and height, in blocks. */
        final float size;
        /** How far over its locator it stands, in blocks. */
        final float lift;

        Marker(Identifier texture, int frames, float size, float lift) {
            this.texture = texture;
            this.frames = frames;
            this.size = size;
            this.lift = lift;
        }
    }

    private final String headLocator;
    private final String markerLocator;
    private final Marker marker;
    private final float headYawSign;

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
        /** The head's turn on top of the animation, in degrees, and when it was last moved. */
        float lookYaw;
        float lookPitch;
        long lookMillis = Util.getMillis();
    }

    /** The model, with the night texture swapped in at night. */
    private static final class NpcModel extends DefaultedEntityGeoModel<NpcEntity> {
        private final @Nullable Identifier night;

        NpcModel(Identifier model, Identifier texture, @Nullable Identifier night) {
            super(model);
            withAltTexture(texture);
            this.night = night == null ? null : buildFormattedTexturePath(night);
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            return night != null && renderState.getOrDefaultGeckolibData(NIGHT, false) ? night : super.getTextureResource(renderState);
        }
    }

    public NpcRenderer(EntityRendererProvider.Context context, Identifier model, Identifier texture) {
        this(context, model, texture, HEAD_LOCATOR, Marker.BUBBLE);
    }

    /**
     * A renderer whose camera follows {@code headLocator}, a locator of the model, and whose {@code marker} floats at
     * {@link #CHAT_LOCATOR} (the bubble) or over the head locator (the ruby).
     */
    public NpcRenderer(EntityRendererProvider.Context context, Identifier model, Identifier texture, String headLocator, Marker marker) {
        this(context, model, texture, null, headLocator, marker, 1.0F);
    }

    /**
     * As above, with a night texture ({@code null} for none) and the sign of the head bone's yaw (-1 for a head bone
     * with Blockbench's half-turn).
     */
    public NpcRenderer(EntityRendererProvider.Context context, Identifier model, Identifier texture, @Nullable Identifier nightTexture,
            String headLocator, Marker marker, float headYawSign) {
        super(context, new NpcModel(model, texture, nightTexture));
        this.shadowRadius = 0.5F;
        this.headLocator = headLocator;
        this.markerLocator = marker == Marker.BUBBLE ? CHAT_LOCATOR : headLocator;
        this.marker = marker;
        this.headYawSign = headYawSign;
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
        look(npc, track, partialTick);
        renderState.addGeckolibData(LOOK_YAW, track.lookYaw);
        renderState.addGeckolibData(LOOK_PITCH, track.lookPitch);
        renderState.addGeckolibData(NIGHT, isNight(npc));
    }

    /** Whether it is night where the NPC is, by the overworld clock that realms share. */
    private static boolean isNight(NpcEntity npc) {
        long time = Math.floorMod(npc.level().getOverworldClockTime(), 24000L);
        return time >= NIGHT_START && time < NIGHT_END;
    }

    /**
     * Eases the head's turn towards the nearest visible player within {@link #LOOK_RANGE} blocks (relative to the NPC's
     * facing, clamped), or back to 0 when there is none.
     */
    private static void look(NpcEntity npc, Track track, float partialTick) {
        Vec3 head = head(npc, partialTick);
        Player target = npc.level().getNearestPlayer(head.x, head.y, head.z, LOOK_RANGE, player -> !player.isSpectator());
        float yaw = 0.0F;
        float pitch = 0.0F;
        if (target != null) {
            Vec3 to = target.getEyePosition(partialTick).subtract(head);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            float bearing = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
            yaw = Mth.clamp(Mth.degreesDifference(npc.getYRot(), bearing), -MAX_LOOK_YAW, MAX_LOOK_YAW);
            pitch = Mth.clamp((float) (-Mth.atan2(to.y, flat) * Mth.RAD_TO_DEG), -MAX_LOOK_PITCH, MAX_LOOK_PITCH);
        }
        long now = Util.getMillis();
        float step = 1.0F - (float) Math.exp(-(now - track.lookMillis) / LOOK_EASE_MS);
        track.lookYaw += (yaw - track.lookYaw) * step;
        track.lookPitch += (pitch - track.lookPitch) * step;
        track.lookMillis = now;
    }

    /** Turns the head bone by the eased look, on top of the animation (GeckoLib's own head turn: pitch on x, yaw on y). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        EntityRenderState state = renderPassInfo.renderState();
        float yaw = state.getOrDefaultGeckolibData(LOOK_YAW, 0.0F) * Mth.DEG_TO_RAD * headYawSign;
        float pitch = state.getOrDefaultGeckolibData(LOOK_PITCH, 0.0F) * Mth.DEG_TO_RAD;
        if (yaw != 0.0F || pitch != 0.0F) {
            snapshots.ifPresent(headLocator, bone -> {
                bone.setRotX(bone.getRotX() + pitch);
                bone.setRotY(bone.getRotY() + yaw);
            });
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
            if (!markerLocator.equals(headLocator)) {
                renderPassInfo.addLocatorPositionListener(markerLocator, (world, model, local) -> track.chat = local);
            }
        }
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Track track = state.getGeckolibData(TRACK);
        if (track != null && track.hidden) {
            return;
        }
        super.submit(state, poseStack, collector, camera);
        @Nullable Vec3 anchor = track == null ? null : markerLocator.equals(headLocator) ? track.head : track.chat;
        if (anchor != null) {
            poseStack.pushPose();
            poseStack.translate(anchor.x, anchor.y + marker.lift, anchor.z);
            billboard(poseStack, collector, camera.orientation, marker, state.getOrDefaultGeckolibData(COLOR, 0xFFFFFFFF));
            poseStack.popPose();
        }
    }

    /**
     * The marker's first frame: a square at the pose's origin, turned to face the camera (as name tags are), its bottom
     * edge on the origin, drawn from both sides at full brightness.
     */
    private static void billboard(PoseStack poseStack, SubmitNodeCollector collector, Quaternionf facing, Marker marker, int color) {
        Vector3f anchor = poseStack.last().pose().getTranslation(new Vector3f());
        poseStack.pushPose();
        poseStack.last().pose().translation(anchor).rotate(facing);
        poseStack.last().normal().rotation(facing);
        float size = marker.size;
        float half = size / 2.0F;
        float v = 1.0F / marker.frames;
        collector.submitCustomGeometry(poseStack, EmissiveRenderTypes.entityTranslucent(marker.texture), (pose, buffer) -> {
            bubbleVertex(buffer, pose, -half, 0.0F, 0.0F, v, color);
            bubbleVertex(buffer, pose, half, 0.0F, 1.0F, v, color);
            bubbleVertex(buffer, pose, half, size, 1.0F, 0.0F, color);
            bubbleVertex(buffer, pose, -half, size, 0.0F, 0.0F, color);
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
