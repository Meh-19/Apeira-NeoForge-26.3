// Apeira's alpha 254 rule: a texel whose alpha is exactly 254 (0xFE) is emissive. It is drawn at full brightness and
// full opacity, ignoring the lightmap and the cardinal face shading; any other alpha keeps its usual meaning (255
// opaque, lower blended). Shared by the apeira:core/emissive_* and finish_item shaders.

bool apeira_isEmissive(float alpha) {
    return abs(alpha * 255.0 - 254.0) < 0.5;
}
