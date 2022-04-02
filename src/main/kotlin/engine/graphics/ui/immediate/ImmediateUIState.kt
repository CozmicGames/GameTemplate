package engine.graphics.ui.immediate

import com.gratedgames.files.ReadStream
import com.gratedgames.files.WriteStream
import com.gratedgames.utils.maths.Vector2
import com.gratedgames.utils.maths.Vector2i
import kotlin.reflect.KProperty

abstract class ImmediateUIState {
    abstract class StateValue<T>(protected var value: T) {
        operator fun getValue(thisRef: Any, property: KProperty<*>) = value

        abstract fun write(stream: WriteStream)

        abstract fun read(stream: ReadStream)
    }

    abstract class MutableStateValue<T>(value: T) : StateValue<T>(value) {
        operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            this.value = value
        }
    }

    class BooleanValue(value: Boolean) : MutableStateValue<Boolean>(value) {
        override fun write(stream: WriteStream) {
            stream.writeByte(if (value) 1.toByte() else 0.toByte())
        }

        override fun read(stream: ReadStream) {
            value = stream.readByte() > 0
        }
    }

    class IntValue(value: Int) : MutableStateValue<Int>(value) {
        override fun write(stream: WriteStream) {
            stream.writeInt(value)
        }

        override fun read(stream: ReadStream) {
            value = stream.readInt()
        }
    }

    class FloatValue(value: Float) : MutableStateValue<Float>(value) {
        override fun write(stream: WriteStream) {
            stream.writeFloat(value)
        }

        override fun read(stream: ReadStream) {
            value = stream.readFloat()
        }
    }

    class TextDataValue(value: TextData) : StateValue<TextData>(value) {
        override fun write(stream: WriteStream) {
            stream.writeString(value.text)
        }

        override fun read(stream: ReadStream) {
            value.setText(stream.readString())
        }
    }

    class Vector2iValue(value: Vector2i) : StateValue<Vector2i>(value) {
        override fun write(stream: WriteStream) {
            stream.writeInt(value.x)
            stream.writeInt(value.y)
        }

        override fun read(stream: ReadStream) {
            value.x = stream.readInt()
            value.y = stream.readInt()
        }
    }

    class Vector2Value(value: Vector2) : StateValue<Vector2>(value) {
        override fun write(stream: WriteStream) {
            stream.writeFloat(value.x)
            stream.writeFloat(value.y)
        }

        override fun read(stream: ReadStream) {
            value.x = stream.readFloat()
            value.y = stream.readFloat()
        }
    }

    private val values = arrayListOf<StateValue<*>>()

    fun <T> addValue(value: StateValue<T>): StateValue<T> {
        values += value
        return value
    }

    fun writeValues(stream: WriteStream) {
        values.forEach {
            it.write(stream)
        }
    }

    fun readValues(stream: ReadStream) {
        values.forEach {
            it.read(stream)
        }
    }
}

fun ImmediateUIState.boolean(value: Boolean = false) = addValue(ImmediateUIState.BooleanValue(value))

fun ImmediateUIState.int(value: Int = 0) = addValue(ImmediateUIState.IntValue(value))

fun ImmediateUIState.float(value: Float = 0.0f) = addValue(ImmediateUIState.FloatValue(value))

fun ImmediateUIState.textData(value: TextData) = addValue(ImmediateUIState.TextDataValue(value))

fun ImmediateUIState.textData(text: String = "", onEnter: TextData.() -> Unit = {}) = textData(TextData(text, onEnter))

fun ImmediateUIState.vector2i(value: Vector2i = Vector2i()) = addValue(ImmediateUIState.Vector2iValue(value))

fun ImmediateUIState.vector2(value: Vector2 = Vector2()) = addValue(ImmediateUIState.Vector2Value(value))
