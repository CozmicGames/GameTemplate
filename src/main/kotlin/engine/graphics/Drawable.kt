package engine.graphics

interface Drawable {
    data class Vertex(var x: Float, var y: Float, var u: Float, var v: Float)

    val vertices: Array<Vertex>
    val indices: Array<Int>
}