package engine.render.functions

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.Primitive
import com.gratedgames.graphics.gpu.getTexture2DUniform
import com.gratedgames.graphics.gpu.pipeline.PipelineDefinition
import com.gratedgames.graphics.gpu.update
import com.gratedgames.utils.Color
import com.gratedgames.utils.Disposable
import engine.render.RenderFunction
import engine.render.colorRenderTargetDependency

abstract class BlitRenderFunction(dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
    private val colorInput = colorRenderTargetDependency(dependencyName, dependencyIndex)

    private val pipeline = PipelineDefinition(
        """
        #section uniforms
        sampler2D uTexture;
        
        #section vertex
        out vec2 vTexcoord;

        void main() {
            float x = -1.0 + float((gl_VertexID & 1) << 2);
            float y = -1.0 + float((gl_VertexID & 2) << 1);
            vTexcoord.x = (x + 1.0) * 0.5;
            vTexcoord.y = (y + 1.0) * 0.5;
            gl_Position = vec4(x, y, 0, 1);
        }
        
        #section fragment
        in vec2 vTexcoord;
                            
        out vec4 outColor;
                
        void main() {
             outColor = texture(uTexture, vTexcoord);
        }
        
    """.trimIndent()
    ).createPipeline()

    override fun render(delta: Float) {
        Kore.graphics.clear(Color.BLACK)
        Kore.graphics.setPipeline(pipeline)
        pipeline.getTexture2DUniform("uTexture")?.update(colorInput.texture)
        Kore.graphics.draw(Primitive.TRIANGLES, 3, 0)
    }

    override fun dispose() {
        pipeline.dispose()
    }
}