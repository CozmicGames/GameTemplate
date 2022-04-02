package engine.graphics.font

import com.gratedgames.graphics.Image
import com.gratedgames.utils.Color
import com.gratedgames.utils.collections.Array2D
import com.gratedgames.utils.maths.distanceSquared
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object DistanceFieldGenerator {
    fun generate(image: Image, spread: Float = 1.0f, downscale: Int = 1, color: Color = Color.WHITE): Image {
        val width = image.width / downscale
        val height = image.height / downscale

        val bitmap = Array2D(image.width, image.height) { x, y ->
            val bits = image[x, y].bits ushr 8
            bits and 0x808080 != 0 && bits and -0x80000000 != 0
        }

        val result = Image(width, height)

        repeat(width) { x ->
            repeat(width) { y ->
                val centerX = x * downscale + downscale / 2
                val centerY = y * downscale + downscale / 2
                val base = bitmap[centerY, centerX] == true
                val delta = ceil(spread).toInt()
                val startX = max(0, centerX - delta)
                val endX = min(width - 1, centerX + delta)
                val startY = max(0, centerY - delta)
                val endY = min(height - 1, centerY + delta)
                var closestSquareDist = delta * delta

                for (yy in startY..endY) {
                    for (xx in startX..endX) {
                        if (base != bitmap[xx, yy]) {
                            val squareDist = distanceSquared(centerX, centerY, xx, yy)
                            if (squareDist < closestSquareDist) {
                                closestSquareDist = squareDist
                            }
                        }
                    }
                }

                val distance = (if (base) 1 else -1) * min(sqrt(closestSquareDist.toFloat()), spread)
                val alpha = 0.5f + 0.5f * (distance / spread)

                result[x, y] = Color(color.r, color.g, color.b, alpha)
            }
        }

        return result
    }
}