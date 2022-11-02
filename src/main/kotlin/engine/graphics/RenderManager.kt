package engine.graphics

import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.DynamicArray
import com.cozmicgames.utils.collections.Pool
import com.cozmicgames.utils.collections.getOrPut
import com.cozmicgames.utils.maths.OrthographicCamera
import engine.Game
import engine.graphics.shaders.DefaultShader
import engine.graphics.shaders.Shader
import engine.materials.Material

class RenderManager : Disposable {
    private val renderLists = DynamicArray<MutableList<Renderable>>()

    private val drawableRenderablePool = Pool(supplier = { DrawableRenderable() })
    private val directRenderablePool = Pool(supplier = { DirectRenderable() })
    private val renderables = arrayListOf<Renderable>()
    private val batchBuilder = RenderBatchBuilder()

    fun submit(layer: Int, drawable: Drawable, material: Material, flipX: Boolean, flipY: Boolean) {
        val renderable = drawableRenderablePool.obtain()
        renderable.drawable = drawable
        renderable.material = material
        renderable.flipX = flipX
        renderable.flipY = flipY
        renderable.layer = layer
        renderable.updateBounds()
        renderables += renderable
    }

    fun submit(layer: Int, texture: Texture2D, shader: String, flipX: Boolean, flipY: Boolean, draw: (DrawContext) -> Unit) {
        val renderable = directRenderablePool.obtain()
        draw(renderable.context)
        renderable.flipX = flipX
        renderable.flipY = flipY
        renderable.texture = texture
        renderable.shader = shader
        renderable.layer = layer
        renderable.updateBounds()
        renderables += renderable
    }

    fun render(camera: OrthographicCamera, layerFilter: (Int) -> Boolean = { true }) {
        for (renderable in renderables) {
            if (!(renderable.bounds intersects camera.rectangle))
                continue

            if (!layerFilter(renderable.layer))
                continue

            val renderList = renderLists.getOrPut(renderable.layer) { arrayListOf() }
            renderList.add(renderable)
        }

        Game.graphics2d.render(camera) { renderer ->
            renderLists.forEach { renderList ->
                for (renderable in renderList)
                    when (renderable) {
                        is DrawableRenderable -> batchBuilder.submit(renderable.material, renderable.drawable, renderable.flipX, renderable.flipY)
                        is DirectRenderable -> {
                            renderer.withTransientState {
                                shader = Game.shaders[renderable.shader] ?: DefaultShader
                                texture = renderable.texture
                                flipX = renderable.flipX
                                flipY = renderable.flipY
                                context.draw(renderable.context)
                            }
                        }
                    }

                renderList.clear()

                batchBuilder.flush { batch ->
                    renderer.drawBatch(batch)
                }
            }
        }
    }

    fun clear() {
        renderables.forEach {
            when (it) {
                is DrawableRenderable -> drawableRenderablePool.free(it)
                is DirectRenderable -> directRenderablePool.free(it)
            }
        }
        renderables.clear()
    }

    override fun dispose() {
        batchBuilder.dispose()
        drawableRenderablePool.dispose()
        directRenderablePool.dispose()
    }
}