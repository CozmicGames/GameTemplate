package engine.graphics.ui.constrained

import com.gratedgames.utils.maths.Rectangle

abstract class Constraint {
    internal enum class Type {
        NONE,
        X,
        Y,
        WIDTH,
        HEIGHT
    }

    internal var type = Type.NONE

    abstract fun getValue(parent: Rectangle, child: Rectangle): Float
}

fun absolute(value: Float, mirror: Boolean = false) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle) = when (type) {
        Type.X -> if (mirror) parent.maxX - (value + child.width) else parent.minX + value
        Type.Y -> if (mirror) parent.maxY - (value + child.height) else parent.minY + value
        Type.WIDTH, Type.HEIGHT -> value
        else -> throw UnsupportedOperationException()
    }
}

fun center() = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle) = when (type) {
        Type.X -> parent.minX + (parent.width - child.width) * 0.5f
        Type.Y -> parent.minY + (parent.height - child.height) * 0.5f
        else -> throw UnsupportedOperationException()
    }
}

fun relative(value: Float) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle) = when (type) {
        Type.X -> parent.minX + parent.width * value
        Type.Y -> parent.minY + parent.height * value
        Type.WIDTH -> parent.width * value
        Type.HEIGHT -> parent.height * value
        else -> throw UnsupportedOperationException()
    }
}

fun aspect(value: Float = 1.0f) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle) = when (type) {
        Type.WIDTH -> child.height * value
        Type.HEIGHT -> child.width / value
        else -> throw UnsupportedOperationException()
    }
}

fun fill() = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle) = when (type) {
        Type.WIDTH -> parent.width
        Type.HEIGHT -> parent.height
        else -> throw UnsupportedOperationException()
    }
}

fun sum(a: Constraint, b: Constraint) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) + b.getValue(parent, child)
    }
}

fun subtraction(a: Constraint, b: Constraint) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) - b.getValue(parent, child)
    }
}

fun multiplication(a: Constraint, b: Constraint) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) * b.getValue(parent, child)
    }
}

fun division(a: Constraint, b: Constraint) = object : Constraint() {
    override fun getValue(parent: Rectangle, child: Rectangle): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) / b.getValue(parent, child)
    }
}

operator fun Constraint.plus(other: Constraint) = sum(this, other)

operator fun Constraint.minus(other: Constraint) = subtraction(this, other)

operator fun Constraint.times(other: Constraint) = multiplication(this, other)

operator fun Constraint.div(other: Constraint) = division(this, other)

class Constraints(block: Constraints.() -> Unit = {}) {
    companion object {
        private val DEFAULT_X = absolute(0.0f).apply { type = Constraint.Type.X }
        private val DEFAULT_Y = absolute(0.0f).apply { type = Constraint.Type.Y }
        private val DEFAULT_WIDTH = fill().apply { type = Constraint.Type.WIDTH }
        private val DEFAULT_HEIGHT = fill().apply { type = Constraint.Type.HEIGHT }
    }

    var x: Constraint = DEFAULT_X
        set(value) {
            value.type = Constraint.Type.X
            field = value
        }

    var y: Constraint = DEFAULT_Y
        set(value) {
            value.type = Constraint.Type.Y
            field = value
        }

    var width: Constraint = DEFAULT_WIDTH
        set(value) {
            value.type = Constraint.Type.WIDTH
            field = value
        }

    var height: Constraint = DEFAULT_HEIGHT
        set(value) {
            value.type = Constraint.Type.HEIGHT
            field = value
        }

    init {
        block(this)
    }
}