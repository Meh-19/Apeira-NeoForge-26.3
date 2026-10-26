#version 330
#extension GL_ARB_separate_shader_objects : require

// Vanilla's core/item.fsh (without the glint) for the apeira:pipeline/finish_item_* pipelines, plus CosmoLib's finish
// branch: a finish (see finish_item.vsh) replaces the texel's colour before it is lit, as CosmoLib's entity shaders did.
// Texels with alpha 254 are emissive (apeira:emissive.glsl): unlit, opaque, finished all the same.

#include <minecraft:globals.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;
// apeira:textures/misc/finish_atlas.png; CosmoLib bound it to Sampler3.
uniform sampler2D FinishSampler;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif
layout(location = 2) in vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec4 overlayColor;
#endif
layout(location = 5) in vec2 texCoord0;
layout(location = 7) flat in float finish;
layout(location = 8) in vec4 normal;
layout(location = 9) in vec4 vertexTint;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

#include <apeira:finish/utils.glsl>
#include <apeira:finish/atlas.glsl>
#include <apeira:finish/phantom.glsl>
#include <apeira:finish/finishes.glsl>
#include <apeira:emissive.glsl>

#ifndef OIT_ALPHA_ONLY
vec4 calculateFinalColor(vec4 color, bool emissive) {
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    if (!emissive) {
        color *= lightMapColor;
    }

    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}
#endif

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    bool emissive = apeira_isEmissive(color.a);
    if (emissive) {
        color.a = 1.0;
    }
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    if (finish > 0.0) {
        color = finishGet(color, texCoord0, finish);
    }

    color *= (emissive ? vertexTint : vertexColor) * ColorModulator;

    // Phantom and Snow lower the alpha; CosmoLib dropped what they made fully clear.
    if (color.a < 0.01) {
        discard;
    }

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color, emissive);
    #endif
}
