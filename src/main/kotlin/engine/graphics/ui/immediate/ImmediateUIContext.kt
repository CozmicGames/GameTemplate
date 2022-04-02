package engine.graphics.ui.immediate

import com.gratedgames.utils.Disposable
import com.gratedgames.utils.maths.VectorPath
import engine.graphics.Renderer

class ImmediateUIContext(val renderer: Renderer, private val ownsRenderer: Boolean) : Disposable {
    private val path = VectorPath()

    fun path(block: VectorPath.() -> Unit): VectorPath {
        path.clear()
        block(path)
        return path
    }

    override fun dispose() {
        if (ownsRenderer)
            renderer.dispose()
    }
}