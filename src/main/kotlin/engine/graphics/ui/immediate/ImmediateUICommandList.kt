package engine.graphics.ui.immediate

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.gpu.ScissorRect
import com.gratedgames.graphics.gpu.Texture2D
import com.gratedgames.utils.Color
import com.gratedgames.utils.concurrency.Lock
import com.gratedgames.utils.maths.Corners
import com.gratedgames.utils.maths.Vector2
import com.gratedgames.utils.maths.VectorPath
import engine.graphics.TextureRegion
import engine.graphics.drawRect
import engine.graphics.font.GlyphLayout

class ImmediateUICommandList {
    private val commands = arrayListOf<ImmediateUIContext.() -> Unit>()
    private val lock = Lock()

    fun addCommand(command: ImmediateUIContext.() -> Unit) = lock.write {
        commands += command
    }

    fun addCommandList(list: ImmediateUICommandList) = lock.write {
        commands.addAll(list.commands)
    }

    fun process(context: ImmediateUIContext) = lock.write {
        commands.forEach {
            it(context)
        }
        commands.clear()
    }
}

fun ImmediateUICommandList.pushScissor(x: Float, y: Float, width: Float, height: Float) = addCommand {
    renderer.pushScissor(ScissorRect(x.toInt(), Kore.graphics.height - (y.toInt() + height.toInt()), width.toInt(), height.toInt()))
}

fun ImmediateUICommandList.popScissor() = addCommand {
    renderer.popScissor()
}

fun ImmediateUICommandList.drawLine(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        line(x0, y0, x1, y1)
    }, thickness, false, color)
}

fun ImmediateUICommandList.drawCurve(x0: Float, y0: Float, x1: Float, y1: Float, controlX0: Float, contronY0: Float, controlX1: Float, contronY1: Float, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        bezier(controlX0, contronY0, controlX1, contronY1, x1, y1)
    }, thickness, false, color)
}

fun ImmediateUICommandList.drawRect(x: Float, y: Float, width: Float, height: Float, roundedCorners: Int, cornerRounding: Float, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        if (roundedCorners != Corners.NONE)
            roundedRect(x, y, width, height, cornerRounding, roundedCorners)
        else
            rect(x, y, width, height)
    }, thickness, true, color)
}

fun ImmediateUICommandList.drawRectFilled(x: Float, y: Float, width: Float, height: Float, roundedCorners: Int, cornerRounding: Float, color: Color) = addCommand {
    renderer.drawPathFilled(path {
        if (roundedCorners != Corners.NONE)
            roundedRect(x, y, width, height, cornerRounding, roundedCorners)
        else
            rect(x, y, width, height)
    }, color)
}

fun ImmediateUICommandList.drawRectMultiColor(x: Float, y: Float, width: Float, height: Float, color00: Color, color01: Color, color11: Color, color10: Color) = addCommand {
    renderer.drawRect(x, y, width, height, color00, color01, color11, color10)
}

fun ImmediateUICommandList.drawTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        line(x0, y0, x1, y1)
        line(x1, y1, x2, y2)
        line(x2, y2, x0, y0)
    }, thickness, true, color)
}

fun ImmediateUICommandList.drawTriangleFilled(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Color) = addCommand {
    renderer.drawPathFilled(path {
        line(x0, y0, x1, y1)
        line(x1, y1, x2, y2)
        line(x2, y2, x0, y0)
    }, color)
}

fun ImmediateUICommandList.drawCircle(x: Float, y: Float, radius: Float, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        circle(x, y, radius)
    }, thickness, true, color)
}

fun ImmediateUICommandList.drawCircleFilled(x: Float, y: Float, radius: Float, color: Color) = addCommand {
    renderer.drawPathFilled(path {
        circle(x, y, radius)
    }, color)
}

fun ImmediateUICommandList.drawArc(x: Float, y: Float, radius: Float, angleMin: Float, angleMax: Float, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        arc(x, y, radius, angleMin, angleMax)
    }, thickness, false, color)
}

fun ImmediateUICommandList.drawArcFilled(x: Float, y: Float, radius: Float, angleMin: Float, angleMax: Float, color: Color) = addCommand {
    renderer.drawPathFilled(path {
        add(x, y)
        arc(x, y, radius, angleMin, angleMax)
    }, color)
}

fun ImmediateUICommandList.drawPolygon(points: Iterable<Vector2>, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        points.forEach {
            add(it.x, it.y)
        }
    }, thickness, true, color)
}

fun ImmediateUICommandList.drawPolygonFilled(points: Iterable<Vector2>, color: Color) = addCommand {
    renderer.drawPathFilled(path {
        points.forEach {
            add(it.x, it.y)
        }
    }, color)
}

fun ImmediateUICommandList.drawPolyline(points: Iterable<Vector2>, thickness: Float, color: Color) = addCommand {
    renderer.drawPathStroke(path {
        points.forEach {
            add(it.x, it.y)
        }
    }, thickness, false, color)
}

fun ImmediateUICommandList.drawImage(x: Float, y: Float, width: Float, height: Float, texture: Texture2D, u0: Float, v0: Float, u1: Float, v1: Float, color: Color) = drawImage(x, y, width, height, TextureRegion(texture, u0, v0, u1, v1), color)

fun ImmediateUICommandList.drawImage(x: Float, y: Float, width: Float, height: Float, region: TextureRegion, color: Color) = addCommand {
    renderer.draw(region, x, y, width, height, color)
}

fun ImmediateUICommandList.drawText(x: Float, y: Float, layout: GlyphLayout, foregroundColor: Color, backgroundColor: Color?) = addCommand {
    if (backgroundColor != null)
        renderer.drawRect(x + layout.width * 0.5f, y + layout.height * 0.5f, layout.width, layout.height, backgroundColor)
    renderer.draw(layout, x, y, foregroundColor)
}

fun ImmediateUICommandList.drawPath(path: VectorPath, thickness: Float, closed: Boolean, color: Color) = addCommand {
    renderer.drawPathStroke(path, thickness, closed, color)
}

fun ImmediateUICommandList.drawPathFilled(path: VectorPath, color: Color) = addCommand {
    renderer.drawPathFilled(path, color)
}
