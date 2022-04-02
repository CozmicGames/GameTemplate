package engine.render.present

class TonemapPresentFunction(type: Type = Type.REINHARD, dependencyName: String, dependencyIndex: Int) : PresentFunction(
    """
        vec4 effect(vec2 position) {
            ${
        when (type) {
            Type.REINHARD -> """
            vec4 color = getColor(position);
            return color / (color + vec4(1.0));
        """
            Type.ACES_FILM -> """
            const float a = 2.51;
            const float b = 0.03;
            const float c = 2.43;
            const float d = 0.59;
            const float e = 0.14;
            vec4 color = getColor(position);
            return clamp((color * (a * color + b)) / (color * (c * color + d ) + e), 0.0, 1.0);
        """
            Type.FILMIC -> """
            vec4 color = getColor(position);
            vec4 x = max(vec4(0.0), color - 0.004);
	        return (x * (6.2 * x + 0.5)) / (x * (6.2 * x + 1.7) + 0.06);
        """
            Type.UNCHARTED2 -> """
	        const float A = 0.15;
	        const float B = 0.50;
	        const float C = 0.10;
	        const float D = 0.20;
	        const float E = 0.02;
	        const float F = 0.30;
	        const float W = 11.2;
	        const float exposureBias = 2.0;

            vec4 curr = getColor(position) * exposureBias;
            curr = ((curr * (A * curr + C * B) + D * E) / (curr * (A * curr + B) + D * F)) - E / F;
	        return curr * ((W * (A * W + C * B) + D * E) / (W * (A * W + B) + D * F)) - E / F;
        """
        }
    }
    }
    """, dependencyName, dependencyIndex
) {
    enum class Type {
        REINHARD,
        ACES_FILM,
        FILMIC,
        UNCHARTED2
    }
}