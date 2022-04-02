package engine.render.passes

import com.gratedgames.graphics.gpu.Texture
import engine.render.RenderPass
import engine.render.addDepthRenderTarget
import engine.render.standardResolution

class ColorDepthRenderPass(resolution: Resolution = standardResolution(), colorFormat: Texture.Format = Texture.Format.RGBA8_UNORM, depthFormat: Texture.Format = Texture.Format.DEPTH24) : RenderPass(resolution) {
    val color = addColorRenderTarget(colorFormat)
    val depth = addDepthRenderTarget(depthFormat)
}