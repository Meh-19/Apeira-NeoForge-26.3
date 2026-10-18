package com.apeira.core.network.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.core.network.PayloadHandlers;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * Client-side counterpart of {@link com.apeira.core.network.ApeiraNetwork}: collects one module's handlers for
 * payloads the client receives and registers them when {@link RegisterClientPayloadHandlersEvent} fires.
 *
 * <p>Only use this class from client-only code, such as a module's {@code @Mod(value = ..., dist = Dist.CLIENT)}
 * entrypoint:
 *
 * <pre>{@code
 * ApeiraClientNetwork.create()
 *         .handle(SeatSyncPayload.TYPE, ClientSeatHandler::onSeatSync)
 *         .register(modEventBus);
 * }</pre>
 */
public final class ApeiraClientNetwork {
    private final List<Consumer<RegisterClientPayloadHandlersEvent>> registrations = new ArrayList<>();
    private @Nullable IEventBus modEventBus;

    private ApeiraClientNetwork() {}

    public static ApeiraClientNetwork create() {
        return new ApeiraClientNetwork();
    }

    /** Sends payloads from the client to the server. */
    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ClientPacketDistributor.sendToServer(payload, payloads);
    }

    /** Handles a client-bound or bidirectional payload on the main client thread. */
    public <T extends CustomPacketPayload> ApeiraClientNetwork handle(ApeiraPayloadType<T> type, IPayloadHandler<T> handler) {
        return handle(type, HandlerThread.MAIN, handler);
    }

    /** Handles a client-bound or bidirectional payload on the given thread. */
    public <T extends CustomPacketPayload> ApeiraClientNetwork handle(ApeiraPayloadType<T> type, HandlerThread thread,
            IPayloadHandler<T> handler) {
        if (modEventBus != null) {
            throw new IllegalStateException("Handlers must be added before ApeiraClientNetwork#register is called");
        }
        IPayloadHandler<T> wrapped = PayloadHandlers.forThread(thread, handler);
        registrations.add(event -> event.register(type.type(), HandlerThread.NETWORK, wrapped));
        return this;
    }

    /** Registers the handlers when NeoForge fires {@link RegisterClientPayloadHandlersEvent}. Call once. */
    public void register(IEventBus modEventBus) {
        if (this.modEventBus != null) {
            throw new IllegalStateException("Client network is already registered");
        }
        this.modEventBus = modEventBus;
        modEventBus.addListener(this::onRegisterClientPayloadHandlers);
    }

    private void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        registrations.forEach(registration -> registration.accept(event));
    }
}
