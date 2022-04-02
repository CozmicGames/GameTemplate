package engine.graphics.ui.immediate

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.safeHeight
import com.gratedgames.graphics.safeWidth
import com.gratedgames.input
import com.gratedgames.input.CharListener
import com.gratedgames.input.KeyListener
import com.gratedgames.utils.Color
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.extensions.clamp
import com.gratedgames.utils.maths.*
import engine.graphics.Renderer
import engine.graphics.TextureRegion
import engine.graphics.font.BitmapFont
import engine.graphics.font.GlyphLayout
import engine.graphics.render
import engine.graphics.ui.*
import engine.utils.Gradient
import engine.utils.GradientColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ImmediateUI private constructor(private val context: ImmediateUIContext, val style: Style = Style()) : Disposable {
    constructor(style: Style = Style()) : this(ImmediateUIContext(Renderer(), true), style)

    constructor(renderer: Renderer, style: Style = Style()) : this(ImmediateUIContext(renderer, false), style)

    class Style : UIStyle() {
        var backgroundColor by color { Color(0x171A23FF) }
        var normalColor by color { Color(0x2A2F3FFF) }
        var highlightColor by color { Color(0x4FB742FF) }
        var hoverColor by color { Color(0x43485BFF) }
        var fontColor by color { Color.WHITE.copy() }
        var cursorColor by color { Color(0xFFFFFF99.toInt()) }
        var font by font { BitmapFont(Kore.graphics.defaultFont) }
        var elementSize by float { font.size }
        var strokeThickness by float { elementSize * 0.1f }
        var elementPadding by float { elementSize * 0.15f }
        var cornerRounding by float { 1.5f }
        var roundedCorners by int { Corners.ALL }
        var offsetToNextX by float { 4.0f }
        var offsetToNextY by float { 4.0f }
    }

    enum class State {
        HOVERED,
        ACTIVE,
        ENTERED,
        LEFT;

        companion object {
            val NONE = 0

            fun combine(vararg states: State, base: Int = NONE): Int {
                var flags = base
                states.forEach {
                    flags = flags or (1 shl it.ordinal)
                }
                return flags
            }

            fun isSet(flags: Int, state: State) = (flags and (1 shl state.ordinal)) != 0
        }
    }

    operator fun Int.plus(state: State) = State.combine(state, base = this)

    operator fun Int.contains(state: State) = State.isSet(this, state)

    enum class ButtonBehaviour {
        NONE,
        DEFAULT,
        REPEATED
    }

    enum class PlotType {
        POINTS,
        BARS,
        LINES
    }

    abstract class Element(val x: Float, val y: Float, val width: Float, val height: Float) {
        abstract val nextX: Float
        abstract val nextY: Float

        operator fun component1() = nextX
        operator fun component2() = nextY
    }

    private val commandList = ImmediateUICommandList()
    private val transform = Matrix4x4()
    private val textDatas = hashMapOf<Any, TextData>()
    private val windows = hashMapOf<String, ImmediateUIWindow>()
    private var isSameLine = false
    private var lineHeight = 0.0f
    private var textDataSetThisFrame = false

    inline val inputX get() = Kore.input.x.toFloat()
    inline val inputY get() = Kore.graphics.height - Kore.input.y.toFloat()

    var activeTextData: TextData? = null

    var time = 0.0f
        private set

    val touchPosition = Vector2()
        get() = field.set(inputX, inputY)

    val lastTouchPosition = Vector2()
        get() = field.set(Kore.input.lastX.toFloat(), Kore.graphics.height - Kore.input.lastY.toFloat())

    var lastElement: Element? = null
        private set

    var currentCommandList = commandList
        private set

    var currentGroup: ImmediateUIGroup? = null
        private set

    private val keyListener: KeyListener = { key, down ->
        if (down)
            activeTextData?.onKeyAction(key)
    }

    private val charListener: CharListener = {
        activeTextData?.onCharAction(it)
    }

    init {
        Kore.input.addKeyListener(keyListener)
        Kore.input.addCharListener(charListener)
    }

    fun getTextData(obj: Any, text: String = "", onEnter: TextData.() -> Unit) = textDatas.getOrPut(obj) { TextData(text, onEnter = onEnter) }

    fun getWindow(name: String, defaultX: Float, defaultY: Float, defaultWidth: Float, defaultHeight: Float, defaultMinimized: Boolean) = windows.getOrPut(name) {
        ImmediateUIWindow(name).also {
            it.x = defaultX
            it.y = defaultY
            it.width = defaultWidth
            it.height = defaultHeight
            it.isMinimized = defaultMinimized
        }
    }

    fun recordCommands(block: () -> Unit): ImmediateUICommandList {
        val list = ImmediateUICommandList()
        val previousList = currentCommandList
        currentCommandList = list
        block()
        currentCommandList = previousList
        return list
    }

    fun setLastElement(x: Float, y: Float, width: Float, height: Float): Element {
        return setLastElement(if (isSameLine)
            object : Element(x, y, width, height) {
                override val nextX get() = x + width + style.offsetToNextX
                override val nextY get() = y
            }
        else
            object : Element(x, y, width, height) {
                override val nextX get() = x
                override val nextY get() = y + height + style.offsetToNextY
            })
    }

    fun setLastElement(element: Element): Element {
        lineHeight = if (isSameLine)
            max(lineHeight, element.height + style.offsetToNextY)
        else
            element.height + style.offsetToNextY

        lastElement = element

        currentGroup?.let {
            it.width = max(it.width, element.x + element.width - it.x + style.elementPadding)

            if (!isSameLine)
                it.height += lineHeight
        }

        return element
    }

    fun getLastElement(defaultX: Float = 0.0f, defaultY: Float = 0.0f): Element {
        return lastElement ?: absolute(defaultX, defaultY)
    }

    fun absolute(point: Vector2) = absolute(point.x, point.y)

    fun absolute(x: Float, y: Float) = object : Element(x, y, 0.0f, 0.0f) {
        override val nextX get() = x
        override val nextY get() = y
    }

    fun relative(factorX: Float, factorY: Float, srcX: Float = Kore.graphics.width.toFloat(), srcY: Float = Kore.graphics.height.toFloat()) = object : Element(factorX * srcX, factorY * srcY, 0.0f, 0.0f) {
        override val nextX get() = x
        override val nextY get() = y
    }

    fun offset(offset: Element, src: Element = getLastElement(), resetX: Boolean = false, resetY: Boolean = false, block: () -> Unit) = offset(offset.nextX, offset.nextY, src, resetX, resetY, block)

    fun offset(offsetX: Float, offsetY: Float, src: Element = getLastElement(), resetX: Boolean = false, resetY: Boolean = false, block: () -> Unit) = offset(offsetX, offsetY, src.nextX, src.nextY, resetX, resetY, block)

    fun offset(offsetX: Float, offsetY: Float, srcX: Float, srcY: Float, resetX: Boolean = false, resetY: Boolean = false, block: () -> Unit): Element {
        val lastElement = getLastElement()
        setLastElement(srcX + offsetX, srcY + offsetY, 0.0f, 0.0f)
        block()
        return setLastElement(if (resetX) lastElement.nextX else getLastElement().nextX, if (resetY) lastElement.nextY else getLastElement().nextY, 0.0f, 0.0f)
    }

    fun transient(block: () -> Unit): Element {
        val lastElement = getLastElement()
        block()
        return setLastElement(lastElement)
    }

    fun blankLine(element: Element = getLastElement()): Element {
        val (x, y) = element
        return setLastElement(x, y, 0.0f, style.elementSize)
    }

    fun sameLine(block: () -> Unit): Element {
        if (isSameLine) {
            block()
            return getLastElement()
        }

        val lastElement = getLastElement()
        isSameLine = true
        lineHeight = 0.0f

        block()

        isSameLine = false

        return setLastElement(lastElement.nextX, lastElement.nextY, 0.0f, lineHeight)
    }

    fun getState(rectangle: Rectangle, buttonBehaviour: ButtonBehaviour = ButtonBehaviour.NONE): Int {
        var state = State.NONE

        if (touchPosition in rectangle) {
            state += State.HOVERED

            when (buttonBehaviour) {
                ButtonBehaviour.DEFAULT -> if (Kore.input.justTouched) state += State.ACTIVE
                ButtonBehaviour.REPEATED -> if (Kore.input.isTouched) state += State.ACTIVE
            }

            if (lastTouchPosition !in rectangle)
                state += State.ENTERED
        } else if (lastTouchPosition in rectangle)
            state += State.LEFT

        return state
    }

    fun group(element: Element = getLastElement(), block: () -> Unit): Element {
        val previousLastElement = lastElement
        lastElement = element

        val previousGroup = currentGroup
        val group = ImmediateUIGroup(element.x, element.y, 0.0f, 0.0f)
        currentGroup = group

        val commands = recordCommands {
            block()
        }

        currentCommandList.drawRectFilled(group.x, group.y, group.width, group.height, style.roundedCorners, style.cornerRounding, style.backgroundColor)
        currentCommandList.addCommandList(commands)

        lastElement = previousLastElement
        currentGroup = previousGroup
        return setLastElement(group.x, group.y, group.width, group.height)
    }

    fun separator(element: Element = getLastElement()) = separator(element.width, element)

    fun separator(width: Float, element: Element = getLastElement()): Element {
        val (x, y) = element
        val separatorX = x + style.elementPadding
        val separatorY = y + style.elementSize * 0.4f
        val separatorWidth = width - style.elementPadding * 2.0f
        val separatorHeight = style.elementSize * 0.2f
        currentCommandList.drawRectFilled(separatorX, separatorY, separatorWidth, separatorHeight, Corners.NONE, 0.0f, style.normalColor)
        return setLastElement(x, y, width, style.elementSize)
    }

    fun spacing(width: Float = style.elementSize, element: Element = getLastElement()): Element {
        val (x, y) = element
        return setLastElement(x, y, width, 0.0f)
    }

    fun colorSquare(color: Color, element: Element = getLastElement()): Element {
        val (x, y) = element
        currentCommandList.drawRectFilled(x, y, style.elementSize, style.elementSize, style.roundedCorners, style.cornerRounding, color)
        return setLastElement(x, y, style.elementSize, style.elementSize)
    }

    fun label(text: String, element: Element = getLastElement()): Element {
        val (x, y) = element
        val layout = GlyphLayout(text, style.font)
        currentCommandList.drawText(x, y, layout, style.fontColor, style.backgroundColor)
        return setLastElement(x, y, layout.width, layout.height)
    }

    fun button(text: String, element: Element = getLastElement(), action: () -> Unit): Element {
        val (x, y) = element

        val rectangle = Rectangle()
        rectangle.x = x
        rectangle.y = y

        val layout = GlyphLayout(text, style.font)
        val textX = x + style.elementPadding
        val textY = y + style.elementPadding

        rectangle.width = layout.width + 2.0f * style.elementPadding
        rectangle.height = layout.height + 2.0f * style.elementPadding

        val state = getState(rectangle, ButtonBehaviour.DEFAULT)

        if (State.ACTIVE in state) {
            action()
            currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, style.roundedCorners, style.cornerRounding, style.highlightColor)
            currentCommandList.drawText(textX, textY, layout, style.fontColor, null)
        } else if (State.HOVERED in state) {
            currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, style.roundedCorners, style.cornerRounding, style.hoverColor)
            currentCommandList.drawText(textX, textY, layout, style.fontColor, null)
        } else {
            currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, style.roundedCorners, style.cornerRounding, style.normalColor)
            currentCommandList.drawText(textX, textY, layout, style.fontColor, null)
        }

        return setLastElement(x, y, rectangle.width, rectangle.height)
    }

    fun image(texture: TextureRegion, width: Float = style.elementSize, height: Float = style.elementSize, color: Color = Color.WHITE, borderThickness: Float = 4.0f, element: Element = getLastElement()): Element {
        val (x, y) = element

        currentCommandList.drawImage(x, y, width, height, texture, color)
        if (borderThickness > 0.0f)
            currentCommandList.drawRect(x, y, width, height, style.roundedCorners, style.cornerRounding, borderThickness, style.normalColor)

        return setLastElement(x, y, width, height)
    }

    fun checkBox(checked: Boolean, element: Element = getLastElement(), action: (Boolean) -> Unit): Element {
        val (x, y) = element
        val size = style.elementSize

        val rectangle = Rectangle(x, y, size, size)
        val state = getState(rectangle, ButtonBehaviour.DEFAULT)

        currentCommandList.drawRectFilled(x, y, size, size, style.roundedCorners, style.cornerRounding, if (State.HOVERED in state) style.hoverColor else style.normalColor)
        val isClicked = State.ACTIVE in state
        var newChecked = checked

        if (isClicked) {
            newChecked = !newChecked
            action(newChecked)
        }

        if (newChecked) {
            val checkMarkX = x + style.elementPadding
            val checkMarkY = y + style.elementPadding
            val checkMarkSize = size - style.elementPadding * 2.0f

            currentCommandList.drawRectFilled(checkMarkX, checkMarkY, checkMarkSize, checkMarkSize, style.roundedCorners, style.cornerRounding, style.highlightColor)
        }

        return setLastElement(x, y, size, size)
    }

    fun option(option: Int, selectedOption: Int, element: Element = getLastElement(), action: (Int) -> Unit): Element {
        val (x, y) = element
        val size = style.elementSize

        val rectangle = Rectangle(x, y, size, size)
        val state = getState(rectangle, ButtonBehaviour.DEFAULT)

        currentCommandList.drawCircleFilled(x + size * 0.5f, y + size * 0.5f, size * 0.5f, if (State.HOVERED in state) style.hoverColor else style.normalColor)
        val isClicked = State.ACTIVE in state
        var newChecked = option == selectedOption

        if (isClicked) {
            newChecked = !newChecked
            action(option)
        }

        if (newChecked) {
            val middleX = x + style.elementPadding
            val middleY = y + style.elementPadding
            val middleSize = size - style.elementPadding * 2.0f

            currentCommandList.drawCircleFilled(middleX + middleSize * 0.5f, middleY + middleSize * 0.5f, middleSize * 0.5f, style.highlightColor)
        }

        return setLastElement(x, y, size, size)
    }

    fun progress(progress: Float, width: Float = 100.0f, element: Element = getLastElement()): Element {
        val (x, y) = element
        val height = style.elementSize

        currentCommandList.drawRectFilled(x, y, width, height, style.roundedCorners, style.cornerRounding, style.normalColor)

        if (progress > 0.0f)
            currentCommandList.drawRectFilled(x, y, width * progress.clamp(0.0f, 1.0f), height, style.roundedCorners, style.cornerRounding, style.highlightColor)

        return setLastElement(x, y, width, height)
    }

    fun slider(amount: Float, width: Float = 100.0f, element: Element = getLastElement(), action: (Float) -> Unit): Element {
        val (x, y) = element

        val handleRadius = style.elementSize * 0.5f

        val sliderHeight = style.elementSize / 3.0f
        val sliderWidth = width - handleRadius * 2.0f
        val sliderX = x + handleRadius
        val sliderY = y + sliderHeight

        var handleX = x + handleRadius + sliderWidth * amount
        val handleY = y + handleRadius

        currentCommandList.drawRectFilled(sliderX, sliderY, sliderWidth, sliderHeight, style.roundedCorners, style.cornerRounding, style.normalColor)

        val rectangle = Rectangle()
        rectangle.x = x
        rectangle.y = y
        rectangle.width = width
        rectangle.height = style.elementSize

        val state = getState(rectangle, ButtonBehaviour.REPEATED)

        if (State.HOVERED in state) {
            val color = if (State.ACTIVE in state) {
                val newAmount = (touchPosition.x - x) / width
                handleX = x + handleRadius + sliderWidth * newAmount

                action(newAmount)
                style.highlightColor
            } else
                style.hoverColor

            currentCommandList.drawCircleFilled(handleX, handleY, handleRadius, color)
        } else
            currentCommandList.drawCircleFilled(handleX, handleY, handleRadius, style.normalColor)

        return setLastElement(x, y, width, style.elementSize)
    }

    fun colorEdit(color: Color, element: Element = getLastElement()): Element {
        val (x, y) = element

        val hsv = color.toHSV()
        var newAlpha = color.a
        val rectangle = Rectangle()
        val size = 100.0f
        var totalWidth = 0.0f
        var totalHeight = 0.0f

        val commands = recordCommands {
            rectangle.x = x + style.elementPadding
            rectangle.y = y + style.elementPadding + 15.0f
            rectangle.width = size
            rectangle.height = size

            currentCommandList.drawRectMultiColor(rectangle.x + rectangle.width * 0.5f, rectangle.y + rectangle.height * 0.5f, rectangle.width, rectangle.height, Color.WHITE, color, color, Color.WHITE)
            currentCommandList.drawRectMultiColor(rectangle.x + rectangle.width * 0.5f, rectangle.y + rectangle.height * 0.5f, rectangle.width, rectangle.height, Color.CLEAR, Color.CLEAR, Color.BLACK, Color.BLACK)

            var state = getState(rectangle, ButtonBehaviour.REPEATED)

            var crossHairColor = style.normalColor
            if (State.HOVERED in state) {
                if (State.ACTIVE in state) {
                    hsv[1] = (touchPosition.x - rectangle.x) / rectangle.width
                    hsv[2] = 1.0f - (touchPosition.y - rectangle.y) / rectangle.height
                    crossHairColor = style.highlightColor
                } else
                    crossHairColor = style.hoverColor
            }

            val crossHairX = rectangle.x + hsv[1] * rectangle.width
            val crossHairY = rectangle.y + (1.0f - hsv[2]) * rectangle.height

            currentCommandList.drawRectFilled(crossHairX - 5.0f, crossHairY - 1.0f, 10.0f, 2.0f, Corners.NONE, 0.0f, crossHairColor)
            currentCommandList.drawRectFilled(crossHairX - 1.0f, crossHairY - 5.0f, 2.0f, 10.0f, Corners.NONE, 0.0f, crossHairColor)

            rectangle.x += 105.0f
            rectangle.width = 20.0f

            val hueBarX = rectangle.x + style.elementPadding
            val hueBarY = rectangle.y
            val hueBarWidth = rectangle.width - style.elementPadding * 2.0f
            val hueBarSubHeight = rectangle.height / Color.HUE_COLORS.size

            Color.HUE_COLORS.forEachIndexed { index, c0 ->
                val c1 = Color.HUE_COLORS[if (index < Color.HUE_COLORS.lastIndex) index + 1 else 0]
                currentCommandList.drawRectMultiColor(hueBarX + hueBarWidth * 0.5f, hueBarY + index * hueBarSubHeight + hueBarSubHeight * 0.5f, hueBarWidth, hueBarSubHeight, c0, c0, c1, c1)
            }

            state = getState(rectangle, ButtonBehaviour.REPEATED)

            var hueLineColor = style.normalColor
            if (State.HOVERED in state) {
                if (State.ACTIVE in state) {
                    hsv[0] = (touchPosition.y - rectangle.y) / rectangle.height * 360.0f
                    hueLineColor = style.highlightColor
                } else
                    hueLineColor = style.hoverColor
            }

            val hueLineY = rectangle.y + (hsv[0] / 360.0f) * rectangle.height - 0.5f - 2.0f
            currentCommandList.drawRectFilled(rectangle.x, hueLineY, rectangle.width, 4.0f, Corners.NONE, 0.0f, hueLineColor)

            rectangle.x += 25.0f
            rectangle.width = 20.0f

            val alphaBarX = rectangle.x + style.elementPadding
            val alphaBarY = rectangle.y
            val alphaBarWidth = rectangle.width - style.elementPadding * 2.0f
            val alphaBarHeight = rectangle.height

            currentCommandList.drawRectMultiColor(alphaBarX + alphaBarWidth * 0.5f, alphaBarY + alphaBarHeight * 0.5f, alphaBarWidth, alphaBarHeight, Color.WHITE, Color.WHITE, Color.BLACK, Color.BLACK)

            state = getState(rectangle, ButtonBehaviour.REPEATED)

            var alphaLineColor = style.normalColor
            if (State.HOVERED in state) {
                if (State.ACTIVE in state) {
                    newAlpha = 1.0f - (touchPosition.y - rectangle.y) / rectangle.height
                    alphaLineColor = style.highlightColor
                } else
                    alphaLineColor = style.hoverColor
            }

            val alphaLineY = rectangle.y + (1.0f - color.a) * rectangle.height - 0.5f - 2.0f
            currentCommandList.drawRectFilled(rectangle.x, alphaLineY, rectangle.width, 4.0f, Corners.NONE, 0.0f, alphaLineColor)

            totalWidth = rectangle.x + rectangle.width - (x + style.elementPadding)
            totalHeight = rectangle.y + rectangle.height - (y + style.elementPadding)
            currentCommandList.drawRectFilled(x + style.elementPadding, y + style.elementPadding, totalWidth, 12.0f, style.roundedCorners, style.cornerRounding, color)
        }

        color.fromHSV(hsv)
        color.a = newAlpha

        currentCommandList.drawRectFilled(x, y, totalWidth + style.elementPadding * 2.0f, totalHeight + style.elementPadding * 2.0f, style.roundedCorners, style.cornerRounding, style.backgroundColor)
        currentCommandList.addCommandList(commands)

        return setLastElement(x, y, totalWidth + style.elementPadding * 2.0f, totalHeight + style.elementPadding * 2.0f)
    }

    fun plot(values: Iterable<Float>, type: PlotType, width: Float = 120.0f, height: Float = 80.0f, min: Float? = null, max: Float? = null, element: Element = getLastElement()): Element {
        val (x, y) = element

        var count = 0
        var minValue = Float.MAX_VALUE
        var maxValue = -Float.MAX_VALUE

        values.forEach {
            minValue = min(minValue, it)
            maxValue = max(maxValue, it)
            count++
        }

        val usedMin = min ?: minValue
        val usedMax = max ?: maxValue

        currentCommandList.drawRectFilled(x, y, width, height, style.roundedCorners, style.cornerRounding, style.normalColor)

        val slotWidth = (width - style.elementPadding * 2.0f) / count
        val slotMaxHeight = height - style.elementPadding * 2.0f
        var slotX = x + style.elementPadding

        fun getSlotHeight(value: Float) = value.convertRange(usedMin, usedMax, 0.0f, 1.0f) * slotMaxHeight

        when (type) {
            PlotType.POINTS -> {
                slotX += slotWidth * 0.5f
                values.forEach {
                    val slotY = y + style.elementPadding + (slotMaxHeight - getSlotHeight(it))
                    currentCommandList.drawCircleFilled(slotX, slotY, 3.0f, style.highlightColor)
                    slotX += slotWidth
                }
            }
            PlotType.BARS -> {
                values.forEach {
                    val slotHeight = getSlotHeight(it)
                    currentCommandList.drawRectFilled(slotX + 1.0f, y + style.elementPadding + slotMaxHeight - slotHeight, slotWidth - 2.0f, slotHeight, Corners.NONE, 0.0f, style.highlightColor)
                    slotX += slotWidth
                }
            }
            PlotType.LINES -> {
                slotX += slotWidth * 0.5f
                var lastValue = 0.0f
                var isFirst = true
                values.forEach {
                    val slotY = y + style.elementPadding + (slotMaxHeight - getSlotHeight(it))

                    if (isFirst)
                        isFirst = false
                    else {
                        val lastSlotX = slotX - slotWidth
                        val lastSlotY = y + style.elementPadding + (slotMaxHeight - getSlotHeight(lastValue))
                        currentCommandList.drawLine(lastSlotX, lastSlotY, slotX, slotY, 2.5f, style.highlightColor)
                    }

                    currentCommandList.drawCircleFilled(slotX, slotY, 3.0f, style.highlightColor)
                    slotX += slotWidth
                    lastValue = it
                }
            }
        }

        return setLastElement(x, y, width, height)
    }

    fun gradient(gradient: Gradient, width: Float = 100.0f, height: Float = 50.0f, element: Element = getLastElement()): Element {
        val (x, y) = element

        lateinit var last: GradientColor
        var isFirst = true

        for (color in gradient) {
            if (isFirst)
                isFirst = false
            else {
                val sliceX = color.stop * width
                val sliceWidth = (color.stop - last.stop) * width
                currentCommandList.drawRectMultiColor(sliceX, y, sliceWidth, height, last.color, last.color, color.color, color.color)
            }

            last = color
        }

        return setLastElement(x, y, width, height)
    }

    fun textField(textData: TextData, minWidth: Float = style.elementSize, element: Element = getLastElement(), action: () -> Unit): Element {
        val (x, y) = element

        val layout = GlyphLayout(textData.text, style.font)
        val rectangle = Rectangle()

        rectangle.x = x
        rectangle.y = y
        rectangle.width = max(layout.width, minWidth)
        rectangle.height = layout.height

        val state = getState(rectangle, ButtonBehaviour.REPEATED)

        if (State.HOVERED in state && State.ACTIVE in state) {
            activeTextData = textData
            textDataSetThisFrame = true
            textData.setCursor(max(0, layout.findCursorIndex(touchPosition.x - x, touchPosition.y - y)))
        } else if (Kore.input.justTouched)
            activeTextData = null

        currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, style.roundedCorners, style.cornerRounding, style.backgroundColor)
        currentCommandList.drawText(x, y, layout, style.fontColor, null)

        if (textData == activeTextData) {
            if (textData.isSelectionActive) {
                val selectionX: Float
                val selectionY: Float
                val selectionWidth: Float

                val cursor0 = textData.getFrontSelectionPosition()
                val cursor1 = textData.getEndSelectionPosition()

                if (cursor0 < layout.count) {
                    val quad = layout[cursor0]
                    selectionX = quad.x
                    selectionY = quad.y
                } else {
                    val quad = layout[layout.count - 1]
                    selectionX = quad.x + quad.width
                    selectionY = quad.y
                }

                if (cursor1 < layout.count) {
                    val quad = layout[cursor1]
                    selectionWidth = quad.x - selectionX
                } else {
                    val quad = layout[layout.count - 1]
                    selectionWidth = quad.x + quad.width - selectionX
                }

                currentCommandList.drawRectFilled(x + selectionX, y + selectionY, selectionWidth, style.elementSize, Corners.NONE, 0.0f, style.cursorColor)
            } else if (sin(time * 5.0f) > 0.0f) {
                val cursorX: Float
                val cursorY: Float

                if (layout.count > 0) {
                    if (textData.cursor < layout.count) {
                        val quad = layout[textData.cursor]
                        cursorX = quad.x
                        cursorY = quad.y
                    } else {
                        val quad = layout[layout.count - 1]
                        cursorX = quad.x + quad.width
                        cursorY = quad.y
                    }
                } else {
                    cursorX = 0.0f
                    cursorY = 0.0f
                }

                currentCommandList.drawRectFilled(x + cursorX, y + cursorY, 1.0f, style.elementSize, Corners.NONE, 0.0f, style.cursorColor)
            }
        }

        if (textData.hasChanged) {
            action()
            textData.hasChanged = false
        }

        return setLastElement(x, y, rectangle.width, rectangle.height)
    }

    fun begin() {
        lastElement = null
        textDataSetThisFrame = false
    }

    fun end() {
        transform.setToOrtho2D(Kore.graphics.safeInsetLeft.toFloat(), Kore.graphics.safeWidth.toFloat(), Kore.graphics.safeHeight.toFloat(), Kore.graphics.safeInsetTop.toFloat())
        context.renderer.render(transform) {
            it.flipY = true
            commandList.process(context)
            it.flipY = false
        }
    }

    override fun dispose() {
        Kore.input.removeKeyListener(keyListener)
        Kore.input.removeCharListener(charListener)
        context.dispose()
    }
}
