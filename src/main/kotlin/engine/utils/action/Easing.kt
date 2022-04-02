package engine.utils.action

import com.gratedgames.utils.extensions.clamp

enum class Easing(private val f: (Float) -> Float) {
    LINEAR({ it }),
    QUAD_IN({ it * it }),
    QUAD_OUT({ it * (2.0f - it) }),
    QUAD_IN_OUT({ if (it < 0.5f) 2.0f * it * it else -1.0f + (4.0f - 2.0f * it) * it }),
    CUBIC_IN({ it * it * it }),
    CUBIC_OUT({ (it - 1.0f) * (it - 1.0f) * (it - 1.0f) + 1.0f }),
    CUBIC_IN_OUT({ if (it < 0.5f) 4.0f * it * it * it else (it - 1.0f) * (2.0f * it - 2.0f) * (2.0f * it - 2.0f) + 1.0f }),
    QUART_IN({ it * it * it * it }),
    QUART_OUT({ 1.0f - (it - 1.0f) * (it - 1.0f) * (it - 1.0f) * (it - 1.0f) }),
    QUART_IN_OUT({ if (it < 0.5f) 8.0f * it * it * it * it else 1.0f - 8.0f * (it - 1.0f) * (it - 1.0f) * (it - 1.0f) * (it - 1.0f) });

    operator fun invoke(value: Float) = f(value).clamp(0.0f, 1.0f)
}