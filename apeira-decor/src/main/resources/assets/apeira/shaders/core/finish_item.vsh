#version 330
#extension GL_ARB_separate_shader_objects : require

// Vanilla's core/item.vsh (without the glint) for the apeira:pipeline/finish_item_* pipelines, plus CosmoLib's finish
// detection: a vertex colour with red and blue 1 and green below 1 is a finish's paint value (FinishType#paintValue),
// whose green is (id - 1) * 2 / 255. An odd green would draw the finish at full brightness, as in CosmoLib.

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 6) in vec3 Normal;

#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif
layout(location = 2) out vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) out vec4 lightMapColor;
layout(location = 4) out vec4 overlayColor;
#endif

layout(location = 5) out vec2 texCoord0;
layout(location = 7) flat out float finish;
layout(location = 8) out vec4 normal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    #ifndef OIT_ALPHA_ONLY
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    #endif

    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);

    if (Color.xz == vec2(1.0) && Color.y < 1.0) {
        // CosmoLib floored here; rounding keeps 8-bit green exact.
        float y = round(Color.y * 255.0);
        bool fullBright = mod(y, 2.0) == 1.0;
        vertexColor = fullBright ? vec4(1.0) : minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0));
        #ifndef OIT_ALPHA_ONLY
        lightMapColor = fullBright ? vec4(1.0) : lightMapColor;
        #endif
        finish = floor(y / 2.0) + 1.0;
    } else {
        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
        finish = 0.0;
    }
}
