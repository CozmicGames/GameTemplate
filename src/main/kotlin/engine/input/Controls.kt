package engine.input

import com.gratedgames.Kore
import com.gratedgames.input
import com.gratedgames.input.*
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.Updateable

class Controls : Updateable, Disposable {
    private class Action : InputListener, Disposable {
        val state get() = value > 0.0f
        val isTriggered get() = state && value != lastValue

        var value = 0.0f
        var lastValue = 0.0f

        val keys = hashSetOf<Key>()
        val mouseButtons = hashSetOf<MouseButton>()
        var isDeltaX = false
        var isDeltaY = false

        init {
            Kore.input.addListener(this)
        }

        fun update() {
            lastValue = value

            if (isDeltaX)
                value = Kore.input.deltaX.toFloat()

            if (isDeltaY)
                value = Kore.input.deltaY.toFloat()
        }

        override fun onKey(key: Key, down: Boolean) {
            if (key in keys)
                value = if (down) 1.0f else 0.0f
        }

        override fun onMouseButton(button: MouseButton, down: Boolean) {
            if (button in mouseButtons)
                value = if (down) 1.0f else 0.0f
        }

        override fun dispose() {
            Kore.input.removeListener(this)
        }
    }

    private val actions = hashMapOf<String, Action>()

    fun getKeys(name: String): Set<Key> = actions[name]?.keys ?: emptySet()

    fun getMouseButtons(name: String): Set<MouseButton> = actions[name]?.mouseButtons ?: emptySet()

    fun isDeltaX(name: String): Boolean = actions[name]?.isDeltaX == true

    fun isDeltaY(name: String): Boolean = actions[name]?.isDeltaY == true

    fun getState(name: String): Boolean {
        return actions[name]?.state ?: false
    }

    fun getTrigger(name: String): Boolean {
        return actions[name]?.isTriggered ?: false
    }

    fun getValue(name: String): Float {
        return actions[name]?.value ?: 0.0f
    }

    fun setActionKeyInput(name: String, key: Key) {
        val action = actions.getOrPut(name) { Action() }
        action.keys += key
    }

    fun removeActionKeyInput(name: String, key: Key) {
        val action = actions.getOrPut(name) { Action() }
        action.keys -= key
    }

    fun clearActionKeyInput(name: String) {
        val action = actions.getOrPut(name) { Action() }
        action.keys.clear()
    }

    fun setActionMouseButtonInput(name: String, button: MouseButton) {
        val action = actions.getOrPut(name) { Action() }
        action.mouseButtons += button
    }

    fun removeActionMouseButtonInput(name: String, button: MouseButton) {
        val action = actions.getOrPut(name) { Action() }
        action.mouseButtons -= button
    }

    fun clearActionMouseButtonInput(name: String) {
        val action = actions.getOrPut(name) { Action() }
        action.mouseButtons.clear()
    }

    fun setActionDeltaXInput(name: String, flag: Boolean) {
        val action = actions.getOrPut(name) { Action() }
        action.isDeltaX = flag
    }

    fun setActionDeltaYInput(name: String, flag: Boolean) {
        val action = actions.getOrPut(name) { Action() }
        action.isDeltaY = flag
    }

    override fun update(delta: Float) {
        actions.forEach { (_, action) ->
            action.update()
        }
    }

    override fun dispose() {
        actions.forEach { (_, action) ->
            action.dispose()
        }
        actions.clear()
    }
}

fun Controls.setNextKeyInput(name: String, filter: (Key) -> Boolean = { true }, cancel: () -> Boolean = { false }) {
    val listener = object : KeyListener {
        override fun invoke(key: Key, down: Boolean) {
            if (cancel()) {
                Kore.onNextFrame {
                    Kore.input.removeKeyListener(this)
                }

                return
            }

            if (down && filter(key)) {
                setActionKeyInput(name, key)

                Kore.onNextFrame {
                    Kore.input.removeKeyListener(this)
                }
            }
        }
    }

    Kore.input.addKeyListener(listener)
}
