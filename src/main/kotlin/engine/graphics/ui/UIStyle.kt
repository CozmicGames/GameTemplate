package engine.graphics.ui

import com.gratedgames.utils.Color
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.maths.Vector2
import com.gratedgames.utils.maths.Vector3
import com.gratedgames.utils.maths.Vector4
import engine.graphics.font.BitmapFont
import engine.graphics.font.DrawableFont
import kotlin.reflect.KProperty

open class UIStyle : Disposable {
    private val values = hashMapOf<String, Any>()

    operator fun <T : Any> set(name: String, value: T) {
        (values.put(name, value) as? Disposable?)?.dispose()
    }

    operator fun <T : Any> get(name: String, default: () -> T) = values.getOrPut(name, default)

    override fun dispose() {
        for ((_, value) in values)
            if (value is Disposable)
                value.dispose()
    }
}

class UIStyleAccessor<T : Any>(private val style: UIStyle, private val supplier: () -> T) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): T = style[property.name, supplier] as T

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        style[property.name] = value
    }
}

fun UIStyle.font(supplier: () -> DrawableFont) = UIStyleAccessor(this, supplier)

fun UIStyle.color(supplier: () -> Color) = UIStyleAccessor(this, supplier)

fun UIStyle.string(supplier: () -> String) = UIStyleAccessor(this, supplier)

fun UIStyle.boolean(supplier: () -> Boolean) = UIStyleAccessor(this, supplier)

fun UIStyle.int(supplier: () -> Int) = UIStyleAccessor(this, supplier)

fun UIStyle.float(supplier: () -> Float) = UIStyleAccessor(this, supplier)

fun UIStyle.vector2(supplier: () -> Vector2) = UIStyleAccessor(this, supplier)

fun UIStyle.vector3(supplier: () -> Vector3) = UIStyleAccessor(this, supplier)

fun UIStyle.vector4(supplier: () -> Vector4) = UIStyleAccessor(this, supplier)
