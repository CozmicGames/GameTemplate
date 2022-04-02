package engine.graphics.ui.constrained

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.input.Key
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.events.EventContext
import com.gratedgames.utils.events.EventListener
import com.gratedgames.utils.maths.Rectangle
import com.gratedgames.utils.maths.Vector2
import engine.graphics.Canvas
import engine.graphics.ui.constrained.transitions.Animator
import engine.graphics.ui.constrained.transitions.Transition
import engine.graphics.Component
import engine.graphics.Renderer
import kotlin.reflect.KClass

abstract class UIComponent : Component, Disposable {
    object Events {
        data class KeyInput(val key: Key, val down: Boolean)
        data class CharInput(val char: Char)
        object Enter
        object Exit
        data class Touch(val pointer: Int, val down: Boolean)
        data class Scroll(val amount: Float)
        object Show
        object Hide
        data class Drop(val values: Array<String>) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Drop) return false

                if (!values.contentEquals(other.values)) return false

                return true
            }

            override fun hashCode(): Int {
                return values.contentHashCode()
            }
        }
    }

    companion object {
        internal val DEFAULT_CONSTRAINTS = Constraints()
        internal val DEFAULT_PARENT = Rectangle()
            get() {
                field.x = 0.0f
                field.y = 0.0f
                field.width = Kore.graphics.width.toFloat()
                field.height = Kore.graphics.height.toFloat()
                return field
            }
    }

    override val rectangle = Rectangle()
        get() {
            field.x = (internalAnimator?.x ?: 0.0f) + constraints.x.getValue(parent, field)
            field.y = (internalAnimator?.y ?: 0.0f) + constraints.y.getValue(parent, field)
            field.width = (internalAnimator?.width ?: 1.0f) * constraints.width.getValue(parent, field)
            field.height = (internalAnimator?.height ?: 1.0f) * constraints.height.getValue(parent, field)
            return field
        }

    protected open val handlesActions = true

    private var isHovered = false
    private val point = Vector2()
    private val children = arrayListOf<UIComponent>()
    private val eventContext = EventContext()

    internal var parent: Rectangle = DEFAULT_PARENT
    internal var constraints: Constraints = DEFAULT_CONSTRAINTS

    val animator: Animator
        get() {
            if (internalAnimator == null)
                internalAnimator = Animator()
            return requireNotNull(internalAnimator)
        }

    private var internalAnimator: Animator? = null

    var isActive = true
        set(value) {
            if (value == field)
                return

            if (value)
                onShowInternal()
            else
                onHideInternal()

            field = value

            for (child in children) {
                child.isActive = value
            }
        }

    open fun onKeyInput(key: Key, down: Boolean) {}
    open fun onCharInput(char: Char) {}
    open fun onEnter() {}
    open fun onExit() {}
    open fun onTouch(pointer: Int, down: Boolean) {}
    open fun onScroll(amount: Float) {}
    open fun onShow() {}
    open fun onHide() {}
    open fun onDrop(values: Array<String>) {}

    private fun onKeyInternal(key: Key, down: Boolean) {
        eventContext.dispatch { Events.KeyInput(key, down) }
        onKeyInput(key, down)
    }

    private fun onCharInternal(char: Char) {
        eventContext.dispatch { Events.CharInput(char) }
        onCharInput(char)
    }

    private fun onEnterInternal() {
        eventContext.dispatch(Events.Enter)
        onEnter()
    }

    private fun onExitInternal() {
        eventContext.dispatch(Events.Exit)
        onExit()
    }

    private fun onTouchInternal(pointer: Int, down: Boolean) {
        eventContext.dispatch { Events.Touch(pointer, down) }
        onTouch(pointer, down)
    }

    private fun onScrollInternal(amount: Float) {
        eventContext.dispatch { Events.Scroll(amount) }
        onScroll(amount)
    }

    private fun onShowInternal() {
        eventContext.dispatch(Events.Show)
        onShow()
    }

    private fun onHideInternal() {
        eventContext.dispatch(Events.Hide)
        onHide()
    }

    private fun onDropInternal(values: Array<String>) {
        eventContext.dispatch { Events.Drop(values) }
        onDrop(values)
    }

    fun hasQueuedTransition(transition: Transition) = animator.hasQueuedTransition(transition)

    fun queueTransition(transition: Transition, isDone: (() -> Unit)? = null) = animator.queueTransition(transition, isDone)

    fun transition(transition: Transition, isDone: (() -> Unit)? = null) = animator.applyTransition(transition, isDone)

    fun addChild(component: UIComponent, constraints: Constraints) {
        children += component
        component.parent = rectangle
        component.constraints = constraints
    }

    fun removeChild(component: UIComponent) {
        children -= component
        component.parent = DEFAULT_PARENT
        component.constraints = DEFAULT_CONSTRAINTS
    }

    inline fun <reified T : Any> addEventListener(listener: EventListener<T>) = addEventListener(listener, T::class)

    fun <T : Any> addEventListener(listener: EventListener<T>, type: KClass<T>) = eventContext.addListener(listener, type)

    inline fun <reified T : Any> removeEventListener(listener: EventListener<T>) = removeEventListener(listener, T::class)

    fun <T : Any> removeEventListener(listener: EventListener<T>, type: KClass<T>) = eventContext.removeListener(listener, type)

    override fun onKeyAction(key: Key, down: Boolean) {
        onKeyInternal(key, down)
    }

    override fun onCharAction(char: Char) {
        onCharInternal(char)
    }

    override fun onMouseAction(x: Int, y: Int) {
        if (!isActive)
            return

        val isInside = point.set(x.toFloat(), y.toFloat()) in rectangle

        if (!isHovered && isInside) {
            onEnterInternal()
            isHovered = true
        } else if (isHovered && !isInside) {
            onExitInternal()
            isHovered = false
        }

        for (child in children)
            if (point.set(x.toFloat(), y.toFloat()) in child.rectangle)
                child.onMouseAction(x, y)
    }

    override fun onTouchAction(x: Int, y: Int, pointer: Int, down: Boolean) {
        if (!isActive)
            return

        if (point.set(x.toFloat(), y.toFloat()) in rectangle)
            onTouchInternal(pointer, down)

        for (child in children)
            if (point.set(x.toFloat(), y.toFloat()) in child.rectangle)
                child.onMouseAction(x, y)

        for (child in children)
            if (point.set(x.toFloat(), y.toFloat()) in child.rectangle)
                child.onTouchAction(x, y, pointer, down)
    }

    override fun onScrollAction(amount: Float) {
        if (!isActive)
            return

        if (isHovered)
            onScrollInternal(amount)

        for (child in children)
            if (child.isHovered)
                child.onScroll(amount)
    }

    override fun onDropAction(values: Array<String>) {
        onDropInternal(values)
    }

    override fun update(delta: Float) {
        if (!isActive)
            return

        internalAnimator?.update(delta)

        for (child in children)
            child.update(delta)
    }

    override fun render(delta: Float, renderer: Renderer) {
        if (!isActive)
            return

        for (child in children)
            child.render(delta, renderer)
    }

    override fun dispose() {
        for (child in children)
            child.dispose()
    }
}

data class ConstrainedComponent(val component: UIComponent, val constraints: Constraints)

infix fun UIComponent.with(constraints: Constraints) = ConstrainedComponent(this, constraints)

fun Canvas.addComponent(component: UIComponent, constraints: Constraints) {
    component.parent = camera.rectangle
    component.constraints = constraints
    addComponent(component)
}

fun Canvas.removeComponent(component: UIComponent) {
    component.parent = UIComponent.DEFAULT_PARENT
    component.constraints = UIComponent.DEFAULT_CONSTRAINTS
    addComponent(component)
}

operator fun UIComponent.plusAssign(component: ConstrainedComponent) = addChild(component.component, component.constraints)