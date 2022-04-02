package engine.graphics

import com.gratedgames.input.Key
import com.gratedgames.utils.maths.Rectangle

interface Component {
    val rectangle: Rectangle

    fun render(delta: Float, renderer: Renderer) {}

    fun update(delta: Float) {}

    fun onKeyAction(key: Key, down: Boolean) {}

    fun onCharAction(char: Char) {}

    fun onMouseAction(x: Int, y: Int) {}

    fun onTouchAction(x: Int, y: Int, pointer: Int, down: Boolean) {}

    fun onScrollAction(amount: Float) {}

    fun onDropAction(values: Array<String>) {}
}