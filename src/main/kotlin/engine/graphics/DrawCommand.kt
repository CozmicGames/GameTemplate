package engine.graphics

import com.gratedgames.graphics.gpu.Texture2D
import engine.graphics.shaders.Shader
import com.gratedgames.utils.Disposable
import engine.utils.CommandList

class DrawCommand(contextSize: Int = 4) : Disposable {
    var texture: Texture2D? = null
    var shader: Shader? = null

    val content = DrawContext2D(contextSize)

    var flipX = false
    var flipY = false

    fun reset() {
        texture = null
        content.reset()
        flipX = false
        flipY = false
    }

    override fun dispose() {
        content.dispose()
    }
}

class DrawCommandList() : CommandList<DrawCommand>({ DrawCommand() }, { it.reset() })
