package engine.utils

import com.gratedgames.utils.Properties
import com.gratedgames.utils.maths.Matrix3x2
import com.gratedgames.utils.maths.Vector2

class Transform(parent: Transform? = null) : Savable {
    private var isDirty = true
    private val children = arrayListOf<Transform>()

    private val internalLocal = Matrix3x2()
    private val internalGlobal = Matrix3x2()
    private val listeners = arrayListOf<() -> Unit>()

    val local: Matrix3x2
        get() {
            if (isDirty) {
                update()
                isDirty = false
            }
            return internalLocal
        }

    val global: Matrix3x2
        get() {
            if (isDirty) {
                update()
                isDirty = false
            }
            return internalGlobal
        }

    var x = 0.0f
        set(value) {
            field = value
            setDirty()
        }

    var y = 0.0f
        set(value) {
            field = value
            setDirty()
        }

    var rotation = 0.0f
        set(value) {
            field = value
            setDirty()
        }

    var scaleX = 1.0f
        set(value) {
            field = value
            setDirty()
        }

    var scaleY = 1.0f
        set(value) {
            field = value
            setDirty()
        }

    var parent: Transform? = parent
        set(value) {
            parent?.children?.remove(this)
            value?.children?.add(this)
            field = value
            setDirty()
        }

    private fun setDirty() {
        isDirty = true
        listeners.forEach {
            it()
        }
    }

    private fun update() {
        internalLocal.setToTranslationRotationScaling(x, y, rotation, scaleX, scaleY)

        val p = parent
        if (p == null)
            internalGlobal.set(internalLocal)
        else
            internalGlobal.set(p.global).mul(internalLocal)

        children.forEach {
            it.update()
        }
    }

    fun addChangeListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun transform(point: Vector2) = transform(point.x, point.y) { x, y -> point.set(x, y) }

    fun <R> transform(x: Float, y: Float, block: (Float, Float) -> R): R = global.transform(x, y, block)

    override fun read(properties: Properties) {
        properties.getFloatArray("position")?.let {
            x = it.getOrElse(0) { 0.0f }
            y = it.getOrElse(1) { 0.0f }
        }

        properties.getFloat("rotation")?.let {
            rotation = it
        }

        properties.getFloatArray("scale")?.let {
            scaleX = it.getOrElse(0) { 0.0f }
            scaleY = it.getOrElse(1) { 0.0f }
        }
    }

    override fun write(properties: Properties) {
        properties.setFloatArray("position", arrayOf(x, y))
        properties.setFloat("rotation", rotation)
        properties.setFloatArray("scale", arrayOf(scaleX, scaleY))
    }
}
