package engine.graphics.particles

import com.gratedgames.utils.Color
import com.gratedgames.utils.maths.toRadians
import engine.Game
import engine.graphics.Renderer
import engine.graphics.asRegion
import engine.graphics.particles.data.*
import kotlin.math.floor
import kotlin.math.min

class ParticleEffect(maxParticles: Int, var emitRate: Float) {
    private val generators = arrayListOf<ParticleGenerator>()
    private val spawners = arrayListOf<ParticleSpawner>()
    private val updaters = arrayListOf<ParticleUpdater>()

    private val data = ParticleData(maxParticles)
    private var time = 0.0f

    fun addGenerator(generator: ParticleGenerator) {
        generators += generator
    }

    fun removeGenerator(generator: ParticleGenerator) {
        generators -= generator
    }

    fun addSpawner(spawner: ParticleSpawner) {
        spawners += spawner
    }

    fun removeSpawner(spawner: ParticleSpawner) {
        spawners -= spawner
    }

    fun addUpdater(updater: ParticleUpdater) {
        updaters += updater
        updater.init(data)
    }

    fun removeUpdater(updater: ParticleUpdater) {
        updaters -= updater
    }

    fun clearGenerators() = generators.clear()

    fun clearSpawners() = spawners.clear()

    fun clearUpdaters() = updaters.clear()

    fun reset() {
        data.numberOfAlive = 0
    }

    fun emit(count: Int) {
        if (spawners.isEmpty())
            return

        val start = data.numberOfAlive
        val end = min(start + count, data.maxParticles - 1)
        val numberOfNewParticles = end - start

        val spawnerCount = numberOfNewParticles / spawners.size
        val remainder = numberOfNewParticles - spawnerCount * spawners.size

        var spawnerStart = start

        spawners.forEachIndexed { index, spawner ->
            val numberToSpawn = if (index < remainder) spawnerCount + 1 else spawnerCount
            spawners[index].spawn(data, spawnerStart, spawnerStart + numberToSpawn)
            spawnerStart += numberToSpawn
        }

        generators.forEach {
            it.generate(data, start, end)
        }

        data.numberOfAlive += numberOfNewParticles
    }

    fun update(delta: Float) {
        if (emitRate > 0.0f) {
            time += delta

            if (time * emitRate > 1.0f) {
                val count = floor(time * emitRate).toInt()
                time -= (count / emitRate)

                emit(count)
            }
        }

        updaters.forEach {
            it.update(data, delta)
        }
    }

    fun render(renderer: Renderer) {
        val positions = data.getArrayOrNull<PositionData>() ?: return
        val sizes = data.getArrayOrNull<SizeData>() ?: return

        val angles = data.getArrayOrNull<AngleData>()
        val textures = data.getArrayOrNull<TextureData>()
        val colors = data.getArrayOrNull<ColorData>()

        repeat(data.numberOfAlive) {
            val angle = angles?.get(it)?.angle ?: 0.0f
            val color = colors?.get(it)?.color ?: Color.WHITE
            val texture = textures?.get(it)?.region ?: Game.graphics2d.blankTexture.asRegion()
            renderer.draw(texture, positions[it].x, positions[it].y, sizes[it].size, sizes[it].size, color, toRadians(angle))
        }
    }
}