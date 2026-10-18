package com.apeira.core.network;

import java.util.Objects;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Pairs a payload's {@link CustomPacketPayload.Type} with the {@link StreamCodec} that writes it, so a payload is
 * declared once and registered with {@link ApeiraNetwork} without repeating either.
 *
 * <pre>{@code
 * public record SitPayload(BlockPos pos) implements ApeiraPayload<SitPayload> {
 *     public static final ApeiraPayloadType<SitPayload> TYPE = ApeiraPayloadType.of("sit",
 *             StreamCodec.composite(BlockPos.STREAM_CODEC, SitPayload::pos, SitPayload::new));
 *
 *     @Override
 *     public ApeiraPayloadType<SitPayload> payloadType() {
 *         return TYPE;
 *     }
 * }
 * }</pre>
 *
 * @param type  the payload's network id, normally {@code apeira:<name>}
 * @param codec encodes and decodes the payload; play-phase payloads may use registry-aware codecs
 */
public record ApeiraPayloadType<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    public ApeiraPayloadType {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(codec, "codec");
    }

    /** Declares a payload with id {@code apeira:<path>}. */
    public static <T extends CustomPacketPayload> ApeiraPayloadType<T> of(String path,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return new ApeiraPayloadType<>(new CustomPacketPayload.Type<>(ApeiraIds.id(path)), codec);
    }

    /** Declares a payload with id {@code <namespace>:<path>}, for modules that use their own namespace. */
    public static <T extends CustomPacketPayload> ApeiraPayloadType<T> of(String namespace, String path,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return new ApeiraPayloadType<>(new CustomPacketPayload.Type<>(ApeiraIds.id(namespace, path)), codec);
    }

    /** Declares a payload that carries no data, such as a request or a notification; it always decodes to {@code instance}. */
    public static <T extends CustomPacketPayload> ApeiraPayloadType<T> unit(String path, T instance) {
        return of(path, StreamCodec.unit(instance));
    }
}
