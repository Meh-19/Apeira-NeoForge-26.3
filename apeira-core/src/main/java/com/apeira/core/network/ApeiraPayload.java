package com.apeira.core.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Base interface for Apeira payloads. A payload implements {@link #payloadType()} with its static
 * {@link ApeiraPayloadType} and gets {@link #type()} for free; see {@link ApeiraPayloadType} for an example.
 *
 * @param <T> the implementing payload type
 */
public interface ApeiraPayload<T extends ApeiraPayload<T>> extends CustomPacketPayload {
    /** The type and codec this payload is registered with. */
    ApeiraPayloadType<T> payloadType();

    @Override
    default Type<T> type() {
        return payloadType().type();
    }
}
