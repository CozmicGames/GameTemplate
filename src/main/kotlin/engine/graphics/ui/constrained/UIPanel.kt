package engine.graphics.ui.constrained

import com.gratedgames.utils.Color
import com.gratedgames.utils.maths.Corners
import com.gratedgames.utils.maths.VectorPath
import engine.graphics.Renderer
import engine.graphics.ui.*

open class UIPanel(var style: Style) : UIComponent() {
    class Style : UIStyle() {
        val hasCornerRounding by boolean { true }
        val cornerRounding by float { 4.0f }
        val roundedCorners by int { Corners.ALL }
        val backgroundColor by color { Color.GRAY.copy() }
        val hasBorder by boolean { true }
        val borderThickness by float { 1.0f }
        val borderColor by color { Color.DARK_GRAY.copy() }
    }

    private val path = VectorPath()

    override fun render(delta: Float, renderer: Renderer) {
        path.clear()

        if (style.hasCornerRounding)
            path.roundedRect(rectangle, style.cornerRounding, style.roundedCorners)
        else
            path.rect(rectangle)

        renderer.drawPathFilled(path, style.backgroundColor)

        if (style.hasBorder)
            renderer.drawPathStroke(path, style.borderThickness, true, style.borderColor)

        super.render(delta, renderer)
    }
}