package engine.graphics.sprite

import com.gratedgames.Kore
import com.gratedgames.log
import engine.graphics.Drawable
import engine.graphics.Ninepatch
import engine.utils.Transform

class TilingSprite(val ninepatch: Ninepatch, tilesX: Int, tilesY: Int, transform: Transform = Transform()) : Sprite(transform) {
    override val texture get() = ninepatch.texture

    var patchesX = tilesX
        set(value) {
            if (value < 3) {
                Kore.log.error(this::class, "Ninepatches need at least 3 patches in each dimension")
                return
            }
            field = value
            isDirty = true
        }

    var patchesY = tilesY
        set(value) {
            if (value < 3) {
                Kore.log.error(this::class, "Ninepatches need at least 3 patches in each dimension")
                return
            }
            field = value
            isDirty = true
        }

    override val vertices = Array(tilesX * tilesY * 4) { Drawable.Vertex(0.0f, 0.0f, 0.0f, 0.0f) }
    override val indices = Array(tilesX * tilesY * 6) { 0 }

    override fun updateVertices() {
        val xOffset = -patchesX * 0.5f
        val yOffset = -patchesY * 0.5f

        var verticesIndex = 0
        var indicesIndex = 0

        fun updateRegion(patchX: Int, patchY: Int) {
            val regionX = when {
                patchX == 0 -> 0
                patchX < patchesX - 1 -> 1
                else -> 2
            }

            val regionY = when {
                patchY == 0 -> 0
                patchY < patchesY - 1 -> 1
                else -> 2
            }

            val xx0 = xOffset + patchX
            val yy0 = yOffset + patchY
            val xx1 = xOffset + patchX + 1.0f
            val yy1 = yOffset + patchY + 1.0f

            transform.transform(xx0, yy0) { x, y ->
                vertices[verticesIndex].x = x
                vertices[verticesIndex].y = y
                vertices[verticesIndex].u = 0.0f
                vertices[verticesIndex].v = 0.0f
            }

            transform.transform(xx1, yy0) { x, y ->
                vertices[verticesIndex + 1].x = x
                vertices[verticesIndex + 1].y = y
                vertices[verticesIndex + 1].u = 1.0f
                vertices[verticesIndex + 1].v = 0.0f
            }

            transform.transform(xx1, yy1) { x, y ->
                vertices[verticesIndex + 2].x = x
                vertices[verticesIndex + 2].y = y
                vertices[verticesIndex + 2].u = 1.0f
                vertices[verticesIndex + 2].v = 1.0f
            }

            transform.transform(xx0, yy1) { x, y ->
                vertices[verticesIndex + 3].x = x
                vertices[verticesIndex + 3].y = y
                vertices[verticesIndex + 3].u = 0.0f
                vertices[verticesIndex + 3].v = 1.0f
            }

            indices[indicesIndex] = verticesIndex
            indices[indicesIndex + 1] = verticesIndex + 1
            indices[indicesIndex + 2] = verticesIndex + 2
            indices[indicesIndex + 3] = verticesIndex
            indices[indicesIndex + 4] = verticesIndex + 2
            indices[indicesIndex + 5] = verticesIndex + 3
        }

        repeat(patchesX) { patchX ->
            repeat(patchesY) { patchY ->
                updateRegion(patchX, patchY)

                verticesIndex += 4
                indicesIndex += 6
            }
        }

        var x0 = -patchesX * 0.5f
        var y0 = -patchesY * 0.5f
        var x1 = patchesX * 0.5f
        var y1 = -patchesY * 0.5f
        var x2 = patchesX * 0.5f
        var y2 = patchesY * 0.5f
        var x3 = -patchesX * 0.5f
        var y3 = patchesY * 0.5f

        transform.transform(x0, y0) { x, y ->
            x0 = x
            y0 = y
        }

        transform.transform(x1, y1) { x, y ->
            x1 = x
            y1 = y
        }

        transform.transform(x2, y2) { x, y ->
            x2 = x
            y2 = y
        }

        transform.transform(x3, y3) { x, y ->
            x3 = x
            y3 = y
        }

        bounds.update(x0, y0, x1, y1, x2, y2, x3, y3)
    }
}