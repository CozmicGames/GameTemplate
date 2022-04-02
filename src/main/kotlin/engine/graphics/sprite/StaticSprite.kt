package engine.graphics.sprite

import engine.graphics.Drawable
import engine.graphics.TextureRegion
import engine.utils.Transform

class StaticSprite(override val texture: TextureRegion, transform: Transform = Transform()) : Sprite(transform) {
    override val vertices = arrayOf(
        Drawable.Vertex(-0.5f, -0.5f, 0.0f, 0.0f),
        Drawable.Vertex(0.5f, -0.5f, 1.0f, 0.0f),
        Drawable.Vertex(0.5f, 0.5f, 1.0f, 1.0f),
        Drawable.Vertex(-0.5f, 0.5f, 0.0f, 1.0f)
    )

    override val indices = arrayOf(0, 1, 2, 0, 2, 3)

    override fun updateVertices() {
        transform.transform(-0.5f, -0.5f) { x, y ->
            vertices[0].x = x
            vertices[0].y = y
        }

        transform.transform(0.5f, -0.5f) { x, y ->
            vertices[1].x = x
            vertices[1].y = y
        }

        transform.transform(0.5f, 0.5f) { x, y ->
            vertices[2].x = x
            vertices[2].y = y
        }

        transform.transform(-0.5f, 0.5f) { x, y ->
            vertices[3].x = x
            vertices[3].y = y
        }

        bounds.update(vertices[0].x, vertices[0].y, vertices[1].x, vertices[1].y, vertices[2].x, vertices[2].y, vertices[3].x, vertices[3].y)
    }
}