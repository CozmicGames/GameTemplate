package engine.render.functions

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.Primitive
import com.gratedgames.graphics.gpu.getTexture2DUniform
import com.gratedgames.graphics.gpu.pipeline.PipelineDefinition
import com.gratedgames.graphics.gpu.update
import com.gratedgames.utils.Color
import com.gratedgames.utils.Disposable
import de.cozmic.*
import engine.render.RenderFunction
import engine.render.colorRenderTargetDependency

class SharpenRenderFunction(dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
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
             float amount = 0.2;
             vec2 texSize = textureSize(uTexture, 0).xy;

             float neighbor = -amount;
             float center = amount * 4.0 + 1.0;

             vec3 color = texture(uTexture, vec2(gl_FragCoord.x + 0, gl_FragCoord.y + 1) / texSize).rgb * neighbor
                         + texture(uTexture, vec2(gl_FragCoord.x - 1, gl_FragCoord.y) / texSize).rgb * neighbor
                         + texture(uTexture, vec2(gl_FragCoord.x + 0, gl_FragCoord.y) / texSize).rgb * center
                         + texture(uTexture, vec2(gl_FragCoord.x + 1, gl_FragCoord.y) / texSize).rgb * neighbor
                         + texture(uTexture, vec2(gl_FragCoord.x + 0, gl_FragCoord.y - 1) / texSize).rgb * neighbor;

             outColor = vec4(color, texture(uTexture, vTexcoord).a);
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