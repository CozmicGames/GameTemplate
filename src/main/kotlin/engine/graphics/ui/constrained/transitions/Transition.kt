package engine.graphics.ui.constrained.transitions

import engine.utils.action.Easing

class Transition {
    enum class Type {
        X {
            override fun applyValue(animator: Animator, value: Float) {
                animator.x = value
            }
        },
        Y {
            override fun applyValue(animator: Animator, value: Float) {
                animator.y = value
            }
        },
        WIDTH {
            override fun applyValue(animator: Animator, value: Float) {
                animator.width = value
            }
        },
        HEIGHT {
            override fun applyValue(animator: Animator, value: Float) {
                animator.height = value
            }
        },
        ALPHA {
            override fun applyValue(animator: Animator, value: Float) {
                animator.alpha = value
            }
        };

        abstract fun applyValue(animator: Animator, value: Float)
    }

    data class ValueTransition(val type: Type, val easing: Easing, val start: Float, val target: Float, val duration: Float, val delay: Float) {
        private var time = 0.0f
        private var isFirstUpdate = true

        fun update(animator: Animator, delta: Float): Boolean {
            if (!isFirstUpdate)
                time += delta
            else
                isFirstUpdate = false

            if (time >= delay) {
                val value = start + (easing((time - delay) / duration) * (target - start))
                type.applyValue(animator, value)
            }

            return time >= duration
        }
    }

    private var transitions = emptyArray<ValueTransition>()

    fun add(type: Type, easing: Easing, start: Float, target: Float, duration: Float, delay: Float = 0.0f) = add(ValueTransition(type, easing, start, target, duration, delay))

    fun add(valueTransition: ValueTransition) {
        transitions = Array(transitions.size + 1) {
            if (it < transitions.size)
                transitions[it]
            else
                valueTransition
        }
    }

    fun add(transition: Transition) {
        transition.transitions.forEach {
            add(it)
        }
    }

    fun update(animator: Animator, delta: Float): Boolean {
        var isDone = true

        transitions.forEach {
            if (!it.update(animator, delta))
                isDone = false
        }

        return isDone
    }

    override fun equals(other: Any?): Boolean {
        if (other is Transition)
            return other.transitions.contentEquals(transitions)
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return transitions.contentHashCode()
    }
}
