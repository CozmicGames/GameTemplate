package engine.graphics.ui.constrained

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.utils.Color
import engine.graphics.ui.UIStyle
import engine.graphics.ui.color
import engine.graphics.ui.font
import engine.graphics.Renderer
import engine.graphics.font.BitmapFont
import engine.graphics.font.GlyphLayout

open class UIText(var style: Style, var text: String) : UIComponent() {
    class Style : UIStyle() {
        val color by color { Color.WHITE.copy() }
        val font by font { BitmapFont(Kore.graphics.defaultFont) }
    }

    private val layout = GlyphLayout()

    override fun render(delta: Float, renderer: Renderer) {
        val bounds = rectangle

        layout.update(text, style.font, bounds = bounds)

        renderer.draw(layout, bounds.minX, bounds.minY, style.color)
    }
}