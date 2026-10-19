// The finish atlas (apeira:textures/misc/finish_atlas.png, a standalone 512x512 texture), as CosmoLib's
// include/finish_atlas.glsl. The fragment shader declares it as `uniform sampler2D FinishSampler`.
#ifndef APEIRA_FINISH_ATLAS_GLSL
#define APEIRA_FINISH_ATLAS_GLSL

// Atlas pixel origin within the atlas texture.
const vec2 FINISH_ATLAS = vec2(0.0);

// Use this instead of /dim for all atlas lookups: it is the finish atlas's own size, not the model texture's.
#define ATLAS_DIM vec2(textureSize(FinishSampler, 0))

#endif
