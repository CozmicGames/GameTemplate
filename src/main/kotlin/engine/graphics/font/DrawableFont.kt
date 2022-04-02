package engine.graphics.font

import com.gratedgames.graphics.gpu.Texture2D
import engine.graphics.shaders.DefaultShader
import engine.graphics.shaders.Shader

interface DrawableFont {
    companion object {
        fun defaultChars() = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890 \"!`?'.,;:()[]{}<>|/@\\^\$-%+=#_&~*"
    }

    val drawableCharacters: String
    val size: Float

    val requiredShader: Shader get() = DefaultShader
    val texture: Texture2D

    operator fun get(char: Char): Glyph
}