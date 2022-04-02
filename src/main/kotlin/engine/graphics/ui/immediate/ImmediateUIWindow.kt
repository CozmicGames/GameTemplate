package engine.graphics.ui.immediate

import com.gratedgames.Kore
import com.gratedgames.input
import com.gratedgames.utils.Color
import com.gratedgames.utils.maths.Corners
import com.gratedgames.utils.maths.Rectangle
import engine.graphics.font.GlyphLayout
import kotlin.math.max

data class ImmediateUIWindow(val title: String) {
    var scrollX = 0.0f
    var scrollY = 0.0f

    var isMinimized = false

    var x = 0.0f
    var y = 0.0f
    var width = 0.0f
    var height = 0.0f
}

fun ImmediateUI.window(name: String, defaultX: Float = getLastElement().nextX, defaultY: Float = getLastElement().nextY, defaultWidth: Float = 100.0f, defaultHeight: Float = 200.0f, defaultMinimized: Boolean = false, block: () -> Unit) = window(getWindow(name, defaultX, defaultY, defaultWidth, defaultHeight, defaultMinimized), block)

fun ImmediateUI.window(window: ImmediateUIWindow, block: () -> Unit) {
    val layout = GlyphLayout()
    val rectangle = Rectangle()

    if (window.isMinimized) {
        layout.update(window.title, style.font)

        rectangle.x = window.x + layout.width + style.elementPadding * 2.0f
        rectangle.y = window.y + style.elementPadding
        rectangle.width = layout.height
        rectangle.height = layout.height

        val triangleState = getState(rectangle, ImmediateUI.ButtonBehaviour.DEFAULT)
        val triangleColor = if (ImmediateUI.State.ACTIVE in triangleState) {
            window.isMinimized = false
            style.highlightColor
        } else if (ImmediateUI.State.HOVERED in triangleState)
            style.fontColor
        else
            style.hoverColor

        val triangleX0 = rectangle.minX
        val triangleY0 = rectangle.minY + rectangle.height * 0.125f
        val triangleX1 = rectangle.maxX
        val triangleY1 = rectangle.minY + rectangle.height * 0.125f
        val triangleX2 = rectangle.centerX
        val triangleY2 = rectangle.maxY - rectangle.height * 0.125f

        rectangle.x = window.x
        rectangle.y = window.y
        rectangle.width = layout.width + layout.height + style.elementPadding * 3.0f
        rectangle.height = layout.height + style.elementPadding * 2.0f

        val titleState = getState(rectangle, ImmediateUI.ButtonBehaviour.REPEATED)

        val titleColor = if (ImmediateUI.State.ACTIVE in titleState && ImmediateUI.State.HOVERED in titleState) {
            window.x += Kore.input.deltaX
            window.y -= Kore.input.deltaY
            style.highlightColor
        } else if (ImmediateUI.State.HOVERED in titleState)
            style.hoverColor
        else
            style.backgroundColor

        currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, Corners.NONE, 0.0f, titleColor)
        currentCommandList.drawText(window.x + style.elementPadding, window.y + style.elementPadding, layout, style.fontColor, null)
        currentCommandList.drawTriangleFilled(triangleX0, triangleY0, triangleX1, triangleY1, triangleX2, triangleY2, triangleColor)
    } else
        transient {
            layout.update(window.title, style.font)
            window.width = max(window.width, layout.width + layout.height + style.elementPadding * 3.0f)

            val minimizeAreaX = window.x + window.width - (layout.height + style.elementPadding)
            val minimizeAreaY = window.y + style.elementPadding
            val minimizeAreaWidth = layout.height
            val minimizeAreaHeight = layout.height

            rectangle.x = minimizeAreaX
            rectangle.y = minimizeAreaY
            rectangle.width = minimizeAreaWidth
            rectangle.height = minimizeAreaHeight

            val minimizeState = getState(rectangle, ImmediateUI.ButtonBehaviour.DEFAULT)
            val minimizeColor = if (ImmediateUI.State.ACTIVE in minimizeState) {
                window.isMinimized = true
                style.highlightColor
            } else if (ImmediateUI.State.HOVERED in minimizeState)
                style.fontColor
            else
                style.hoverColor

            val minimizeTriangleX0 = rectangle.minX
            val minimizeTriangleY0 = rectangle.maxY - rectangle.height * 0.125f
            val minimizeTriangleX1 = rectangle.maxX
            val minimizeTriangleY1 = rectangle.maxY - rectangle.height * 0.125f
            val minimizeTriangleX2 = rectangle.centerX
            val minimizeTriangleY2 = rectangle.minY + rectangle.height * 0.125f

            rectangle.x = window.x
            rectangle.y = window.y
            rectangle.width = window.width
            rectangle.height = layout.height + style.elementPadding * 2.0f

            val titleState = getState(rectangle, ImmediateUI.ButtonBehaviour.REPEATED)

            val titleColor = if (ImmediateUI.State.ACTIVE in titleState && ImmediateUI.State.HOVERED in titleState) {
                window.x += Kore.input.deltaX
                window.y -= Kore.input.deltaY
                style.highlightColor
            } else if (ImmediateUI.State.HOVERED in titleState)
                style.hoverColor
            else
                style.backgroundColor

            currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, Corners.NONE, 0.0f, titleColor)
            currentCommandList.drawText(window.x + style.elementPadding, window.y + style.elementPadding, layout, style.fontColor, null)
            currentCommandList.drawTriangleFilled(minimizeTriangleX0, minimizeTriangleY0, minimizeTriangleX1, minimizeTriangleY1, minimizeTriangleX2, minimizeTriangleY2, minimizeColor)

            val contentX = window.x
            val contentY = window.y + layout.height + style.elementPadding * 2.0f
            val contentWidth = window.width
            val contentHeight = window.height

            lateinit var content: ImmediateUI.Element

            val commands = recordCommands {
                content = group(absolute(contentX - window.scrollX, contentY - window.scrollY), block)
            }

            currentCommandList.drawRectFilled(contentX, contentY, contentWidth, contentHeight, Corners.NONE, 0.0f, style.backgroundColor)

            currentCommandList.pushScissor(contentX, contentY, contentWidth, contentHeight)
            currentCommandList.addCommandList(commands)
            currentCommandList.popScissor()

            val resizeAreaX = contentX + window.width - (style.elementPadding + style.elementSize)
            val resizeAreaY = contentY + window.height - (style.elementPadding + style.elementSize)
            val resizeAreaWidth = style.elementSize
            val resizeAreaHeight = style.elementSize

            rectangle.x = resizeAreaX
            rectangle.y = resizeAreaY
            rectangle.width = resizeAreaWidth
            rectangle.height = resizeAreaHeight

            val resizeState = getState(rectangle, ImmediateUI.ButtonBehaviour.REPEATED)
            val resizeColor = if (ImmediateUI.State.ACTIVE in resizeState) {
                window.width += Kore.input.deltaX
                window.height -= Kore.input.deltaY
                style.highlightColor
            } else if (ImmediateUI.State.HOVERED in resizeState)
                style.fontColor
            else
                style.hoverColor

            val resizeTriangleX0 = resizeAreaX
            val resizeTriangleY0 = resizeAreaY + resizeAreaHeight
            val resizeTriangleX1 = resizeAreaX + resizeAreaWidth
            val resizeTriangleY1 = resizeAreaY + resizeAreaHeight
            val resizeTriangleX2 = resizeAreaX + resizeAreaWidth
            val resizeTriangleY2 = resizeAreaY

            currentCommandList.drawTriangleFilled(resizeTriangleX0, resizeTriangleY0, resizeTriangleX1, resizeTriangleY1, resizeTriangleX2, resizeTriangleY2, resizeColor)

            if (window.width < content.width) {
                val scrollbarX = contentX + style.elementPadding
                val scrollbarY = contentY + window.height - (style.elementPadding + style.elementSize * 0.5f)
                val scrollbarWidth = window.width - (resizeAreaWidth + style.elementPadding * 2.0f)
                val scrollbarHeight = style.elementSize * 0.5f

                currentCommandList.drawRectFilled(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, style.roundedCorners, style.cornerRounding, Color.BLUE)

                val scrollBarGripX = scrollbarX + window.scrollX * window.width / content.width
                val scrollbarGripWidth = window.width / content.width * scrollbarWidth

                rectangle.x = scrollBarGripX
                rectangle.y = scrollbarY
                rectangle.width = scrollbarGripWidth
                rectangle.height = scrollbarHeight

                val scrollbarGripState = getState(rectangle, ImmediateUI.ButtonBehaviour.REPEATED)

                val scrollbarGripColor = if (ImmediateUI.State.ACTIVE in scrollbarGripState && ImmediateUI.State.HOVERED in scrollbarGripState) {
                    window.scrollX += Kore.input.deltaX * content.width / window.width

                    if (window.scrollX < 0.0f)
                        window.scrollX = 0.0f

                    if (window.width + window.scrollX >= content.width)
                        window.scrollX = content.width - window.width

                    style.highlightColor
                } else if (ImmediateUI.State.HOVERED in scrollbarGripState)
                    style.hoverColor
                else
                    style.backgroundColor

                currentCommandList.drawRectFilled(scrollBarGripX, scrollbarY, scrollbarGripWidth, scrollbarHeight, style.roundedCorners, style.cornerRounding, scrollbarGripColor)
            } else
                window.scrollX = 0.0f

            if (window.height < content.height) {
                val scrollbarX = contentX + window.width - (style.elementPadding + style.elementSize * 0.5f)
                val scrollbarY = contentY + style.elementPadding
                val scrollbarWidth = style.elementSize * 0.5f
                val scrollbarHeight = window.height - (resizeAreaWidth + style.elementPadding * 2.0f)

                currentCommandList.drawRectFilled(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, style.roundedCorners, style.cornerRounding, style.hoverColor)

                val scrollBarGripY = scrollbarY + window.scrollY
                val scrollbarGripHeight = scrollbarHeight - (content.height - window.height)

                rectangle.x = scrollbarX
                rectangle.y = scrollBarGripY
                rectangle.width = scrollbarWidth
                rectangle.height = scrollbarGripHeight

                val scrollbarGripState = getState(rectangle, ImmediateUI.ButtonBehaviour.REPEATED)

                val scrollbarGripColor = if (ImmediateUI.State.ACTIVE in scrollbarGripState && ImmediateUI.State.HOVERED in scrollbarGripState) {
                    window.scrollY -= Kore.input.deltaY
                    if (window.scrollY < 0.0f)
                        window.scrollY = 0.0f

                    if (window.height + window.scrollY >= content.height)
                        window.scrollY = content.height - window.height

                    style.highlightColor
                } else if (ImmediateUI.State.HOVERED in scrollbarGripState)
                    style.hoverColor
                else
                    style.backgroundColor

                currentCommandList.drawRectFilled(scrollbarX, scrollBarGripY, scrollbarWidth, scrollbarGripHeight, style.roundedCorners, style.cornerRounding, scrollbarGripColor)
            } else
                window.scrollX = 0.0f
        }
}
