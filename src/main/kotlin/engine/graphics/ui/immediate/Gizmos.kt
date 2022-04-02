package engine.graphics.ui.immediate

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.graphics.safeHeight
import com.gratedgames.graphics.safeWidth
import com.gratedgames.input
import com.gratedgames.utils.Color
import com.gratedgames.utils.maths.Corners
import com.gratedgames.utils.maths.Vector2
import com.gratedgames.utils.maths.VectorPath
import de.cozmic.*
import kotlin.math.ceil

object Gizmos {
    var isEnabled = true

    var outlineColorActive = Color(1.0f, 0.0f, 0.0f, 1.0f)
    var outlineColorInactive = Color(0.66f, 0.33f, 0.33f, 1.0f)

    fun translate(ui: ImmediateUI, position: Vector2, bounds: VectorPath) {
        if (ui.touchPosition in bounds) {
            if (Kore.input.isTouched) {
                position.x += Kore.input.deltaX
                position.y += Kore.input.deltaY

                ui.currentCommandList.drawPath(bounds, 1.5f, true, outlineColorActive)
            } else
                ui.currentCommandList.drawPath(bounds, 1.5f, true, outlineColorInactive)
        }
    }

    fun grid(ui: ImmediateUI, cellSize: Float, x: Float = 0.0f, y: Float = 0.0f, color: Color = Color.WHITE) {
        if (!isEnabled)
            return

        val cellsX = ceil(Kore.graphics.safeWidth / cellSize).toInt()
        val cellsY = ceil(Kore.graphics.safeHeight / cellSize).toInt()

        repeat(cellsX + 2) { cellX ->
            val lineX = (cellX - 1) * cellSize + x % cellSize
            ui.currentCommandList.drawLine(lineX, 0.0f, lineX, Kore.graphics.safeHeight.toFloat(), 1.5f, color)
        }

        repeat(cellsY + 2) { cellY ->
            val lineY = (cellY - 1) * cellSize + y % cellSize
            ui.currentCommandList.drawLine(0.0f, lineY, Kore.graphics.safeWidth.toFloat(), lineY, 1.5f, color)
        }
    }

    fun background(ui: ImmediateUI, color: Color) {
        if (!isEnabled)
            return

        ui.currentCommandList.drawRectFilled(0.0f, 0.0f, Kore.graphics.width.toFloat(), Kore.graphics.height.toFloat(), Corners.NONE, 0.0f, color)
    }
}