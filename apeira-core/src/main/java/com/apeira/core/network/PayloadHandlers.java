package com.apeira.core.network;

import java.util.Objects;

import com.apeira.core.ApeiraCore;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * Helpers for running payload handlers on the right thread.
 *
 * <p>Payloads arrive on the network thread. Anything that touches the level, players or block entities must run on the
 * main game thread through {@link IPayloadContext#enqueueWork}. {@link ApeiraNetwork} wraps handlers with
 * {@link #onMainThread} by default, so handlers only need these helpers when registered for the network thread.
 */
public final class PayloadHandlers {
    private PayloadHandlers() {}

    /**
     * Wraps a handler so it runs on the main game thread. An exception thrown by the handler is logged with the
     * payload id instead of being lost in the returned future.
     */
    public static <T extends CustomPacketPayload> IPayloadHandler<T> onMainThread(IPayloadHandler<T> handler) {
        return (payload, context) -> enqueue(context, payload, () -> handler.handle(payload, context));
    }

    /** Wraps {@code handler} with {@link #onMainThread} for {@link HandlerThread#MAIN}, and returns it as is otherwise. */
    public static <T extends CustomPacketPayload> IPayloadHandler<T> forThread(HandlerThread thread, IPayloadHandler<T> handler) {
        Objects.requireNonNull(handler, "handler");
        return thread == HandlerThread.MAIN ? onMainThread(handler) : handler;
    }

    /**
     * Runs {@code task} on the main game thread, logging any exception it throws. Use from a network-thread handler
     * once the cheap, thread-safe work (validation, decoding) is done.
     */
    public static void enqueue(IPayloadContext context, CustomPacketPayload payload, Runnable task) {
        context.enqueueWork(task).exceptionally(error -> {
            ApeiraCore.LOGGER.error("Failed to handle {} payload {}", context.flow(), payload.type().id(), error);
            return null;
        });
    }
}
