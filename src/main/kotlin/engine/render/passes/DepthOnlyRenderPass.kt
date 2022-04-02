package engine.render.passes

import com.gratedgames.graphics.gpu.Texture
import engine.render.RenderPass
import engine.render.addDepthRenderTarget
import engine.render.standardResolution

class DepthOnlyRenderPass(resolution: Resolution = standardResolution(), depthFormat: Texture.Format = Texture.Format.DEPTH24) : RenderPass(resolution) {
    val depth = addDepthRenderTarget(depthFormat)
}