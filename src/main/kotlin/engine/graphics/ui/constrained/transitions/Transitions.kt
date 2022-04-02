package engine.graphics.ui.constrained.transitions

import engine.utils.action.Easing

object Transitions {
    fun slideX(distance: Float, duration: Float, delay: Float = 0.0f, easing: Easing = Easing.LINEAR): Transition {
        val transition = Transition()
        transition.add(Transition.Type.X, easing, 0.0f, distance, duration, delay)
        return transition
    }

    fun slideY(distance: Float, duration: Float, delay: Float = 0.0f, easing: Easing = Easing.LINEAR): Transition {
        val transition = Transition()
        transition.add(Transition.Type.Y, easing, 0.0f, distance, duration, delay)
        return transition
    }

    fun fadeIn(duration: Float, delay: Float = 0.0f, easing: Easing = Easing.LINEAR): Transition {
        val transition = Transition()
        transition.add(Transition.Type.ALPHA, easing, 0.0f, 1.0f, duration, delay)
        return transition
    }

    fun fadeOut(duration: Float, delay: Float = 0.0f, easing: Easing = Easing.LINEAR): Transition {
        val transition = Transition()
        transition.add(Transition.Type.ALPHA, easing, 1.0f, 0.0f, duration, delay)
        return transition
    }

    fun combine(vararg transitions: Transition): Transition {
        val transition = Transition()
        transitions.forEach {
            transition.add(it)
        }
        return transition
    }
}