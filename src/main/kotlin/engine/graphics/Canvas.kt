package engine.graphics

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.safeHeight
import com.gratedgames.graphics.safeWidth
import com.gratedgames.input
import com.gratedgames.input.Key
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.maths.OrthographicCamera
import com.gratedgames.utils.maths.Vector2

class Canvas(width: Int = Kore.graphics.safeWidth, height: Int = Kore.graphics.safeHeight, val renderer: Renderer = Renderer()) : Disposable {
    var width = width
        private set

    var height = height
        private set

    var camera = OrthographicCamera(width, height)

    private val components = arrayListOf<Component>()

    private val keyListener = { key: Key, down: Boolean ->
        for (component in components)
            component.onKeyAction(key, down)
    }

    private val charListener = { char: Char ->
        for (component in components)
            component.onCharAction(char)
    }

    private val mouseListener = { x: Int, y: Int ->
        for (component in components)
            component.onMouseAction(x, y)
    }

    private val touchListener = { x: Int, y: Int, pointer: Int, down: Boolean ->
        for (component in components)
            component.onTouchAction(x, y, pointer, down)
    }

    private val scrollListener = { amount: Float ->
        for (component in components)
            component.onScrollAction(amount)
    }

    private val dropListener = { values: Array<String> ->
        for (component in components)
            if (Vector2(Kore.input.x.toFloat(), Kore.input.y.toFloat()) in component.rectangle)
                component.onDropAction(values)
    }

    init {
        Kore.input.addKeyListener(keyListener)
        Kore.input.addCharListener(charListener)
        Kore.input.addMouseListener(mouseListener)
        Kore.input.addTouchListener(touchListener)
        Kore.input.addScrollListener(scrollListener)
        Kore.addDropListener(dropListener)
    }

    fun resize(width: Int, height: Int, resetPosition: Boolean = true) {
        this.width = width
        this.height = height

        camera.width = width
        camera.height = height
        if (resetPosition)
            camera.resetPosition()
        camera.update()
    }

    fun render(delta: Float) {
        renderer.begin(camera)

        components.forEach {
            it.update(delta)

            if (it.rectangle in camera.rectangle)
                it.render(delta, renderer)
        }

        renderer.end()
    }

    fun addComponent(component: Component) {
        components += component
    }

    fun removeComponent(component: Component) {
        components.remove(component)
    }

    operator fun plusAssign(component: Component) = addComponent(component)

    operator fun minusAssign(component: Component) = removeComponent(component)

    override fun dispose() {
        components.forEach {
            if (it is Disposable)
                it.dispose()
        }

        Kore.input.removeKeyListener(keyListener)
        Kore.input.removeCharListener(charListener)
        Kore.input.removeMouseListener(mouseListener)
        Kore.input.removeTouchListener(touchListener)
        Kore.input.removeScrollListener(scrollListener)
        Kore.removeDropListener(dropListener)
    }
}