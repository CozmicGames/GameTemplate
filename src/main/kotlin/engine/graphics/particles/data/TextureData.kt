package engine.graphics.particles.data

import engine.graphics.particles.ParticleData
import engine.Game
import engine.graphics.TextureRegion
import engine.graphics.asRegion

data class TextureData(var region: TextureRegion = Game.graphics2d.blankTexture.asRegion()) : ParticleData.DataType

