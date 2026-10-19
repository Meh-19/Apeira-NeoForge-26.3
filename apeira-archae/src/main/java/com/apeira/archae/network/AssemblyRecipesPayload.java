package com.apeira.archae.network;

import java.util.List;

import com.apeira.archae.assembly.AssemblyRecipe;
import com.apeira.archae.assembly.AssemblyRecipes;
import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: every assembly table recipe, in the server's order, on login and after {@code /reload}. */
public record AssemblyRecipesPayload(List<AssemblyRecipe> recipes) implements ApeiraPayload<AssemblyRecipesPayload> {
    public static final ApeiraPayloadType<AssemblyRecipesPayload> TYPE = ApeiraPayloadType.of("assembly_recipes",
            StreamCodec.composite(AssemblyRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblyRecipesPayload::recipes,
                    AssemblyRecipesPayload::new));

    @Override
    public ApeiraPayloadType<AssemblyRecipesPayload> payloadType() {
        return TYPE;
    }

    public static void handle(AssemblyRecipesPayload payload, IPayloadContext context) {
        AssemblyRecipes.setClientRecipes(payload.recipes());
    }
}
