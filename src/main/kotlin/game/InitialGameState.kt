package game

import com.gratedgames.Kore
import com.gratedgames.graphics
import com.gratedgames.utils.Color
import engine.GameState

class InitialGameState : GameState {
    override fun onCreate() {

    }

    override fun onFrame(delta: Float): GameState {
        Kore.graphics.clear(Color.LIME)
        return this
    }
}
