package engine.graphics

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.gpu.Texture
import com.gratedgames.graphics.safeHeight
import com.gratedgames.graphics.safeWidth
import com.gratedgames.memory.Memory
import com.gratedgames.memory.of
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.maths.OrthographicCamera
import com.gratedgames.utils.use

class Graphics2D : Disposable {
    val blankTexture = Kore.graphics.createTexture2D(Texture.Format.RGBA8_UNORM) {
        Memory.of(0xFFFFFFFF.toInt()).use {
            setImage(1, 1, it, Texture.Format.RGBA8_UNORM)
        }
    }

    val defaultCamera = OrthographicCamera(Kore.graphics.safeWidth, Kore.graphics.safeHeight)

    private val resizeListener = { width: Int, height: Int ->
        defaultCamera.width = width
        defaultCamera.height = height
        defaultCamera.resetPosition()
        defaultCamera.update()
    }

    init {
        Kore.addResizeListener(resizeListener)
    }

    override fun dispose() {
        Kore.removeResizeListener(resizeListener)
    }
}
