package engine.graphics.animation

import com.gratedgames.Kore
import com.gratedgames.log

class AnimationController(private val animations: Map<String, Animation>, currentAnimation: String) {
    var currentAnimation = currentAnimation
        set(value) {
            if (value !in animations) {
                Kore.log.error(this::class, "'$value' is not a valid animation")
                return
            }

            val previousCallback = onFinish
            val previousAnimation = field
            onFinish = {
                field = previousAnimation
                onFinish = previousCallback
            }
            field = value
        }

    val currentTexture get() = requireNotNull(animations[currentAnimation]).currentTexture

    private var animationStateTime = 0.0f

    private var onFinish: () -> Unit = {}

    fun reset() {
        animationStateTime = 0.0f
    }

    fun update(delta: Float) {
        animationStateTime += delta


        if (requireNotNull(animations[currentAnimation]).isFinished(animationStateTime)) {
            animationStateTime = 0.0f
            onFinish()
        }

        val animation = requireNotNull(animations[currentAnimation])

        animation.update(animationStateTime)
    }
}