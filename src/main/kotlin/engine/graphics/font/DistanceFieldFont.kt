package engine.graphics.font

import com.gratedgames.graphics.Font
import com.gratedgames.graphics.Image
import engine.graphics.font.DrawableFont.Companion.defaultChars
import com.gratedgames.graphics.gpu.Texture
import com.gratedgames.graphics.gpu.Texture2D
import engine.graphics.shaders.DistanceFieldShader
import com.gratedgames.graphics.toTexture2D
import com.gratedgames.utils.Color
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.rectpack.RectPacker

class DistanceFieldFont(val font: Font, override val drawableCharacters: String = defaultChars(), padding: Int = 4, spread: Float = 1.0f, downscale: Int = 1, color: Color = Color.WHITE) : DrawableFont, Disposable {
    override val size = font.size.toFloat()

    override val texture: Texture2D

    override val requiredShader get() = DistanceFieldShader

    private val glyphs = hashMapOf<Char, Glyph>()

    init {
        val charImages = hashMapOf<Int, Image>()

        drawableCharacters.forEach {
            charImages[it.code] = DistanceFieldGenerator.generate(font.getCharImage(it) ?: requireNotNull(font.getCharImage(' ')), spread, downscale, color)
        }

        var image = Image(128, 128)

        val rects = charImages.map { (char, image) ->
            RectPacker.Rectangle(char, image.width + padding, image.height + padding)
        }.toTypedArray()

        while (true) {
            val packer = RectPacker(image.width, image.height)
            packer.pack(rects)

            if (rects.any { !it.isPacked }) {
                image = Image(image.width * 2, image.height * 2)
                continue
            }

            for (rect in rects) {
                val x = rect.x + padding / 2
                val y = rect.y + padding / 2

                val charImage = charImages[rect.id] ?: continue

                image.drawImage(charImage, x, y)

                val u0 = x.toFloat() / image.width
                val v0 = y.toFloat() / image.height
                val u1 = (x + charImage.width).toFloat() / image.width
                val v1 = (y + charImage.height).toFloat() / image.height

                glyphs[rect.id.toChar()] = Glyph(u0, v0, u1, v1, charImage.width, charImage.height)
            }

            break
        }

        texture = image.toTexture2D {
            setFilter(Texture.Filter.LINEAR, Texture.Filter.LINEAR)
        }
    }

    override operator fun get(char: Char) = glyphs.getOrElse(char) { requireNotNull(glyphs[' ']) }

    override fun dispose() {
        texture.dispose()
    }
}
