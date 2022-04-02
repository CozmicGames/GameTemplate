package engine.graphics.animation

import com.gratedgames.utils.maths.randomInt
import engine.graphics.TextureRegion
import kotlin.math.max
import kotlin.math.min

interface Animation {
    val currentTexture: TextureRegion
    fun update(stateTime: Float)
    fun isFinished(stateTime: Float): Boolean
}

class StaticAnimation(private val texture: () -> TextureRegion) : Animation {
    override val currentTexture get() = texture()

    override fun update(stateTime: Float) {}

    override fun isFinished(stateTime: Float) = false
}

class KeyframeAnimation(private var frameDuration: Float, private val mode: Mode = Mode.NORMAL, private vararg val keyFrames: () -> TextureRegion) : Animation {
    enum class Mode {
        NORMAL,
        REVERSED,
        LOOP,
        LOOP_REVERSED,
        LOOP_PINGPONG,
        LOOP_RANDOM
    }

    val duration = frameDuration * keyFrames.size

    override val currentTexture get() = keyFrames[currentIndex]()

    private var currentIndex = 0

    private var lastFrameIndex = 0
    private var lastFrameTime = 0.0f

    override fun update(stateTime: Float) {
        val index = getKeyFrameIndex(stateTime)
        currentIndex = index
    }

    override fun isFinished(stateTime: Float): Boolean {
        val index = (stateTime / frameDuration).toInt()
        return keyFrames.size - 1 < index
    }

    private fun getKeyFrameIndex(stateTime: Float): Int {
        if (keyFrames.size == 1)
            return 0

        var index = (stateTime / frameDuration).toInt()

        when (mode) {
            Mode.NORMAL -> index = min(keyFrames.lastIndex, index)
            Mode.LOOP -> index %= keyFrames.size
            Mode.LOOP_PINGPONG -> {
                index %= (keyFrames.size * 2) - 2
                if (index >= keyFrames.size)
                    index = keyFrames.size - 2 - (index - keyFrames.size)
            }
            Mode.LOOP_RANDOM -> {
                lastFrameIndex = (lastFrameTime / frameDuration).toInt()
                index = if (lastFrameIndex != index)
                    randomInt(keyFrames.size - 1)
                else
                    lastFrameIndex
            }
            Mode.REVERSED -> index = max(keyFrames.size - index - 1, 0)
            Mode.LOOP_REVERSED -> {
                index %= keyFrames.size
                index = keyFrames.size - index - 1
            }
        }

        lastFrameIndex = index
        lastFrameTime = stateTime

        return index
    }
}