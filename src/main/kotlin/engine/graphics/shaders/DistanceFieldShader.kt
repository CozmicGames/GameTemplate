package engine.graphics.shaders

object DistanceFieldShader : Shader(
    """
    #section uniforms
    float uDistanceFieldSmoothing
    
    #section common
    Vertex vertexShader(vec2 position, vec2 texcoord, vec4 color) {
        Vertex v;
        v.position = position;
        v.texcoord = texcoord;
        v.color = color;
        return v;
    }
    
    vec4 fragmentShader(sampler2D sampler, vec2 uv, vec4 color) {
        float smoothing = 0.25 / uDistanceFieldSmoothing;
        float edge0 = 0.5 - smoothing;
        float edge1 = 0.5 + smoothing;
        float distance = texture(sampler, uv).a;
        float t = clamp((distance - edge0) / (edge1 - edge0), 0.0, 1.0);
        float alpha = t * t * (3.0 - 2.0 * t);
        return vec4(color.rgb, alpha * color.a);
    }
""".trimIndent()
)
