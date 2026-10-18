package com.apeira.core.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Collects one module's play-phase payloads and registers them with NeoForge's {@link PayloadRegistrar} when
 * {@link RegisterPayloadHandlersEvent} fires.
 *
 * <p>Each module creates one network in its {@code @Mod} class, declares payloads in holder classes and passes them to
 * {@link #register(IEventBus, PayloadModule...)} from the mod constructor:
 *
 * <pre>{@code
 * public static final ApeiraNetwork NETWORK = ApeiraNetwork.create("1");
 *
 * public final class DecorPayloads {
 *     public static void init(ApeiraNetwork network) {
 *         network.toServer(SitPayload.TYPE, SitPayload::handle);
 *         network.toClient(SeatSyncPayload.TYPE);
 *     }
 * }
 *
 * // In the module's @Mod constructor:
 * NETWORK.register(modEventBus, DecorPayloads::init);
 * }</pre>
 *
 * <p>Since NeoForge 26.x, handlers for payloads the client receives are registered separately on the client, through
 * {@link com.apeira.core.network.client.ApeiraClientNetwork}, so common code never references client-only classes.
 *
 * <p>Handlers run on the main game thread unless registered with {@link HandlerThread#NETWORK}; see
 * {@link PayloadHandlers}.
 */
public final class ApeiraNetwork {
    private final String version;
    private final List<Consumer<PayloadRegistrar>> registrations = new ArrayList<>();
    private @Nullable IEventBus modEventBus;

    private ApeiraNetwork(String version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    /**
     * Creates a network whose payloads share {@code version}. Bump it whenever a payload's codec changes, so clients
     * and servers with incompatible payloads refuse to connect instead of failing to decode.
     */
    public static ApeiraNetwork create(String version) {
        return new ApeiraNetwork(version);
    }

    /**
     * Runs each module so its payloads are queued, then registers them when NeoForge fires
     * {@link RegisterPayloadHandlersEvent}. Call once, from the module's {@code @Mod} constructor.
     */
    public void register(IEventBus modEventBus, PayloadModule... modules) {
        if (this.modEventBus != null) {
            throw new IllegalStateException("Network is already registered");
        }
        for (PayloadModule module : modules) {
            module.register(this);
        }
        this.modEventBus = modEventBus;
        modEventBus.addListener(this::onRegisterPayloadHandlers);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        // Handlers are wrapped with PayloadHandlers.onMainThread where needed, so register them all for the network
        // thread and let the wrapper decide.
        PayloadRegistrar registrar = event.registrar(version).executesOn(HandlerThread.NETWORK);
        registrations.forEach(registration -> registration.accept(registrar));
    }

    /**
     * Registers a payload the server sends to clients. Its handler is registered on the client with
     * {@link com.apeira.core.network.client.ApeiraClientNetwork#handle}.
     */
    public <T extends CustomPacketPayload> ApeiraNetwork toClient(ApeiraPayloadType<T> type) {
        return queue(registrar -> registrar.playToClient(type.type(), type.codec()));
    }

    /** Registers a payload clients send to the server, handled on the main game thread. */
    public <T extends CustomPacketPayload> ApeiraNetwork toServer(ApeiraPayloadType<T> type, IPayloadHandler<T> handler) {
        return toServer(type, HandlerThread.MAIN, handler);
    }

    /** Registers a payload clients send to the server, handled on the given thread. */
    public <T extends CustomPacketPayload> ApeiraNetwork toServer(ApeiraPayloadType<T> type, HandlerThread thread,
            IPayloadHandler<T> handler) {
        IPayloadHandler<T> wrapped = PayloadHandlers.forThread(thread, handler);
        return queue(registrar -> registrar.playToServer(type.type(), type.codec(), wrapped));
    }

    /**
     * Registers a payload sent in both directions. {@code serverHandler} handles it on the server, on the main game
     * thread; the client handler is registered with
     * {@link com.apeira.core.network.client.ApeiraClientNetwork#handle}.
     */
    public <T extends CustomPacketPayload> ApeiraNetwork bidirectional(ApeiraPayloadType<T> type, IPayloadHandler<T> serverHandler) {
        return bidirectional(type, HandlerThread.MAIN, serverHandler);
    }

    /** Registers a payload sent in both directions, with its server handler on the given thread. */
    public <T extends CustomPacketPayload> ApeiraNetwork bidirectional(ApeiraPayloadType<T> type, HandlerThread thread,
            IPayloadHandler<T> serverHandler) {
        IPayloadHandler<T> wrapped = PayloadHandlers.forThread(thread, serverHandler);
        return queue(registrar -> registrar.playBidirectional(type.type(), type.codec(), wrapped));
    }

    public String version() {
        return version;
    }

    private ApeiraNetwork queue(Consumer<PayloadRegistrar> registration) {
        if (modEventBus != null) {
            throw new IllegalStateException("Payloads must be added before ApeiraNetwork#register is called");
        }
        registrations.add(registration);
        return this;
    }
}
