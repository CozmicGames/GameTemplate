package engine.graphics.shaders

import com.gratedgames.Kore
import com.gratedgames.files
import com.gratedgames.files.Files
import com.gratedgames.files.readToString
import com.gratedgames.graphics.gpu.VertexLayout
import com.gratedgames.graphics.gpu.pipeline.PipelineDefinition
import com.gratedgames.utils.extensions.removeComments

open class Shader(source: String) {
    companion object {
        private fun loadSource(file: String, type: Files.Type): String {
            val text = Kore.files.readToString(file, type)
            return text.removeComments()
        }

        val VERTEX_LAYOUT = VertexLayout {
            vec2("position")
            vec2("texcoord")
            vec2("uBounds")
            vec2("vBounds")
            vec4("color", true, VertexLayout.AttributeType.BYTE)
        }
    }

    constructor(file: String, type: Files.Type) : this(loadSource(file, type))

    private val definition = PipelineDefinition()

    init {
        definition.parse(
            """
            $source
                
            #section state
            blend add source_alpha one_minus_source_alpha
            
            #section layout
            vec2 position
            vec2 texcoord
            vec2 uBounds
            vec2 vBounds
            normalized vec4 color byte
            
            #section uniforms
            mat4 uTransform;
            bool uFlipX;
            bool uFlipY;
            sampler2D uTexture;
            
            #section types
            struct Vertex {
                vec2 position;
                vec2 texcoord;
                vec4 color;
            }
            
            #section vertex
            #define uMin (uBounds.x)
            #define vMin (vBounds.x)
            #define uMax (uBounds.y)
            #define vMax (vBounds.y)
            #define uRange (uMax - uMin)
            #define vRange (vMax - vMin)

            out vec2 vTexcoord;
            out vec4 vColor;

            void main() {
                vec2 tc;
                
                if (uFlipX) {
                    float normalizedU = (texcoord.x - uMin) / uRange;
                    float flippedNormalizedU = 1.0 - normalizedU;
                    float flippedU = uMin + flippedNormalizedU * uRange;
                    tc.x = flippedU;
                } else
                    tc.x = texcoord.x;
                
                if (uFlipY) {
                    float normalizedV = (texcoord.y - vMin) / vRange;
                    float flippedNormalizedV = 1.0 - normalizedV;
                    float flippedV = vMin + flippedNormalizedV * vRange;
                    tc.y = flippedV;
                } else
                    tc.y = texcoord.y;
                
               
                Vertex v = vertexShader(position, tc, color);
                 
                gl_Position = uTransform * vec4(v.position, 0.0, 1.0);
                vTexcoord = v.texcoord;
                vColor = v.color;
            }
            
            #section fragment
            in vec2 vTexcoord;
            in vec4 vColor;
                                
            out vec4 outColor;
                                
            void main() {
                outColor = fragmentShader(uTexture, vTexcoord, vColor);
            }
            
        """.trimIndent()
        )
    }

    fun createPipeline() = definition.createPipeline()
}