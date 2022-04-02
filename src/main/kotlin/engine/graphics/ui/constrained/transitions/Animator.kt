package engine.graphics.ui.constrained.transitions

class Animator {
    private class RunningTransition(val transition: Transition, var isDone: (() -> Unit)? = null)

    data class State(var x: Float = 0.0f, var y: Float = 0.0f, var width: Float = 1.0f, var height: Float = 1.0f, var alpha: Float = 1.0f)

    internal var x = 0.0f
    internal var y = 0.0f
    internal var width = 1.0f
    internal var height = 1.0f
    internal var alpha = 1.0f

    val isTransitioning get() = transition != null

    private var transition: RunningTransition? = null

    private val queuedTransitions = arrayListOf<RunningTransition>()

    fun getState(state: State = State()): State {
        state.x = x
        state.y = y
        state.width = width
        state.height = height
        state.alpha = alpha
        return state
    }

    fun setState(state: State) {
        x = state.x
        y = state.y
        width = state.width
        height = state.height
        alpha = state.alpha
    }

    fun hasQueuedTransition(transition: Transition): Boolean {
        queuedTransitions.forEach {
            if (it.transition == transition)
                return true
        }

        return false
    }

    fun queueTransition(transition: Transition, isDone: (() -> Unit)? = null) {
        queuedTransitions += RunningTransition(transition, isDone)
    }

    fun applyTransition(transition: Transition, isDone: (() -> Unit)? = null): Boolean {
        if (isTransitioning)
            return false

        this.transition = RunningTransition(transition, isDone)
        return true
    }

    fun update(delta: Float) {
        if (transition?.transition?.update(this, delta) == true) {
            transition?.isDone?.invoke()
            transition = queuedTransitions.removeFirstOrNull()
        }
    }
}