package engine.graphics

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.*
import engine.graphics.font.GlyphLayout
import com.gratedgames.graphics.gpu.*
import engine.graphics.shaders.DefaultShader
import engine.graphics.shaders.Shader
import com.gratedgames.utils.Color
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.collections.Stack
import com.gratedgames.utils.maths.Camera
import com.gratedgames.utils.maths.Matrix4x4
import com.gratedgames.utils.maths.VectorPath
import engine.Game
import engine.graphics.sprite.Sprite
import engine.utils.CommandList

class Renderer : Disposable {
    val context = DrawContext2D()

    private val vertexBuffer = Kore.graphics.createBuffer(GraphicsBuffer.Usage.DYNAMIC)
    private val indexBuffer = Kore.graphics.createBuffer(GraphicsBuffer.Usage.DYNAMIC)
    private val pipelines = hashMapOf<Shader, Pipeline>()
    private val path = VectorPath()
    private val scissorStack = Stack<ScissorRect?>()
    private var forceUpdateUniforms = false
    private var forceUpdateShader = false

    var isActive = false
        private set

    var shader: Shader = DefaultShader
        set(value) {
            if (!forceUpdateShader && value == field)
                return

            val pipeline = pipelines.getOrPut(value) { value.createPipeline() }

            Kore.graphics.setPipeline(pipeline)

            forceUpdateUniforms = true
            texture = Game.graphics2d.blankTexture
            transform = transform
            flipX = flipX
            flipY = flipY
            forceUpdateUniforms = false

            field = value
        }

    var transform = Game.graphics2d.defaultCamera.projectionView
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            pipelines[shader]?.getMatrixUniform("uTransform")?.update(value)
            field = value
        }

    var texture: Texture2D = Game.graphics2d.blankTexture
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            pipelines[shader]?.getTexture2DUniform("uTexture")?.update(value)
            field = value
        }

    var flipX = false
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            pipelines[shader]?.getBooleanUniform("uFlipX")?.update(value)
            field = value
        }

    var flipY = false
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            pipelines[shader]?.getBooleanUniform("uFlipY")?.update(value)
            field = value
        }

    fun path(block: VectorPath.(Renderer) -> Unit): VectorPath {
        path.clear()
        block(path, this)
        return path
    }

    fun <R> withTransform(transform: Matrix4x4, block: (Renderer) -> R): R {
        val previous = this.transform
        this.transform = transform
        val result = block(this)
        this.transform = previous
        return result
    }

    fun <R> withFlippedX(flip: Boolean = !flipX, block: (Renderer) -> R): R {
        val previous = this.flipX
        this.flipX = flip
        val result = block(this)
        this.flipX = previous
        return result
    }

    fun <R> withFlippedY(flip: Boolean = !flipY, block: (Renderer) -> R): R {
        val previous = this.flipY
        this.flipY = flip
        val result = block(this)
        this.flipY = previous
        return result
    }

    fun <R> withShader(shader: Shader, block: (Renderer) -> R): R {
        val previous = this.shader
        this.shader = shader
        val result = block(this)
        this.shader = previous
        return result
    }

    fun pushScissor(rect: ScissorRect?) {
        flush()
        Kore.graphics.setScissor(rect)
        scissorStack.push(rect)
    }

    fun popScissor() {
        flush()
        scissorStack.pop()
        Kore.graphics.setScissor(scissorStack.current)
    }

    fun <R> withScissor(rect: ScissorRect?, block: (Renderer) -> R): R {
        pushScissor(rect)
        val result = block(this)
        popScissor()
        return result
    }

    fun withTransientState(block: Renderer.() -> Unit) {
        val shader = this.shader
        val texture = this.texture
        val transform = this.transform
        val flipX = this.flipX
        val flipY = this.flipY
        block(this)
        forceUpdateUniforms = true
        this.shader = shader
        this.texture = texture
        this.transform = transform
        this.flipX = flipX
        this.flipY = flipY
        forceUpdateUniforms = false
    }

    fun draw(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE, rotation: Float = 0.0f) = draw(texture.texture, x, y, width, height, color, rotation, texture.u0, texture.v0, texture.u1, texture.v1)

    fun draw(texture: Texture2D, x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE, rotation: Float = 0.0f, u0: Float = 0.0f, v0: Float = 0.0f, u1: Float = 1.0f, v1: Float = 1.0f) {
        require(isActive)

        this.texture = texture

        context.drawRect(x, y, width, height, color, rotation, u0, v0, u1, v1)
    }

    fun drawPathFilled(path: VectorPath, color: Color = Color.WHITE, texture: Texture2D = Game.graphics2d.blankTexture, vMin: Float = 0.0f, uMin: Float = 0.0f, uMax: Float = 1.0f, vMax: Float = 1.0f) {
        require(isActive)

        this.texture = texture
        context.drawPathFilled(path, color, uMin, vMin, uMax, vMax)
    }

    fun drawPathStroke(path: VectorPath, thickness: Float, closed: Boolean, color: Color = Color.WHITE, extrusionOffset: Float = 0.5f) {
        require(isActive)

        this.texture = Game.graphics2d.blankTexture
        context.drawPathStroke(path, thickness, closed, color, extrusionOffset)
    }

    fun draw(sprite: Sprite) {
        require(isActive)

        this.texture = sprite.texture.texture
        context.drawSprite(sprite)
    }

    fun draw(glyphLayout: GlyphLayout, x: Float, y: Float, color: Color = Color.WHITE) {
        require(isActive)

        withShader(glyphLayout.font.requiredShader) {
            this.texture = glyphLayout.font.texture
            context.drawGlyphs(glyphLayout, x, y, color)
        }
    }

    fun draw(command: DrawCommand) {
        require(isActive)

        this.texture = command.texture ?: Game.graphics2d.blankTexture
        this.shader = command.shader ?: DefaultShader

        context.draw(command.content)
    }

    fun draw(commands: CommandList<DrawCommand>, reset: Boolean = true) {
        commands.process {
            draw(it)

            if (reset)
                it.reset()
        }
    }

    fun begin() {
        if (isActive)
            return

        isActive = true

        forceUpdateShader = true
        shader = DefaultShader
        forceUpdateShader = false

        Kore.graphics.setVertexBuffer(vertexBuffer, Shader.VERTEX_LAYOUT.indices)
        Kore.graphics.setIndexBuffer(indexBuffer)
    }

    fun end() {
        if (!isActive)
            return

        flush()

        context.reset()

        Kore.graphics.setPipeline(null)
        Kore.graphics.setVertexBuffer(null)
        Kore.graphics.setIndexBuffer(null)

        isActive = false
    }

    fun flush() {
        if (context.numVertices == 0 || context.numIndices == 0)
            return

        context.updateBuffers(vertexBuffer, indexBuffer)

        Kore.graphics.drawIndexed(Primitive.TRIANGLES, context.numIndices, 0, IndexDataType.INT)

        context.reset()
    }

    override fun dispose() {
        context.dispose()
        vertexBuffer.dispose()
        indexBuffer.dispose()
    }
}

inline fun <R> Renderer.render(camera: Camera, block: (Renderer) -> R) = render(camera.projectionView, block)

inline fun <R> Renderer.render(transform: Matrix4x4 = Game.graphics2d.defaultCamera.projectionView, block: (Renderer) -> R): R {
    begin(transform)
    val result = block(this)
    end()
    return result
}

inline fun Renderer.setCamera(camera: Camera) {
    this.transform = camera.projectionView
}

inline fun <R> Renderer.withCamera(camera: Camera, noinline block: (Renderer) -> R) = withTransform(camera.projectionView, block)

inline fun Renderer.begin(transform: Matrix4x4) {
    begin()
    this.transform = transform
}

inline fun Renderer.begin(camera: Camera) = begin(camera.projectionView)

fun Renderer.drawTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color0: Color, color1: Color = color0, color2: Color = color0) {
    require(isActive)

    context.drawTriangle(x0, y0, x1, y1, x2, y2, color0, color1, color2)
}

fun Renderer.drawRect(x: Float, y: Float, width: Float, height: Float, color00: Color, color01: Color = color00, color11: Color = color00, color10: Color = color00) {
    require(isActive)

    context.drawRect(x, y, width, height, color00, color01, color11, color10)
}
