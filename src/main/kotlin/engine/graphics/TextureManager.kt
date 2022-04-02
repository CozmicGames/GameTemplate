package engine.graphics

import com.gratedgames.Kore
import com.gratedgames.files
import com.gratedgames.files.Files
import com.gratedgames.graphics
import com.gratedgames.graphics.Image
import com.gratedgames.graphics.gpu.Texture
import com.gratedgames.log
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.extensions.extension
import com.gratedgames.utils.use
import kotlin.reflect.KProperty

class TextureManager : Disposable {
    inner class Getter(val file: String, val filter: Texture.Filter) {
        operator fun getValue(thisRef: Any, property: KProperty<*>) = getOrAdd(file, filter)
    }

    private data class TextureKey(val filter: Texture.Filter)

    private val textures = hashMapOf<TextureKey, TextureAtlas>()
    private val keys = hashMapOf<String, TextureKey>()

    fun add(file: String, filter: Texture.Filter = Texture.Filter.NEAREST) {
        if (!Kore.files.exists(file, Files.Type.ASSET)) {
            Kore.log.error(this::class, "Texture file not found: $file")
            return
        }

        val image = Kore.files.readAsset(file).use {
            Kore.graphics.readImage(it, file.extension)
        }

        if (image == null) {
            Kore.log.error(this::class, "Failed to load texture file: $file")
            return
        }

        add(file, image, filter)
    }

    operator fun contains(name: String) = name in keys

    fun add(file: String, image: Image, filter: Texture.Filter = Texture.Filter.NEAREST) {
        val key = TextureKey(filter)
        val atlas = getAtlas(key)
        atlas.add(file to image)
        keys[file] = key
    }

    fun remove(file: String) {
        val key = keys[file] ?: return
        textures[key]?.remove(file)
    }

    operator fun get(file: String): TextureRegion? {
        val key = keys[file] ?: return null
        val texture = textures[key] ?: return null
        return texture[file]
    }

    fun getOrAdd(file: String, filter: Texture.Filter = Texture.Filter.NEAREST): TextureRegion {
        if (file !in this)
            add(file, filter)

        return requireNotNull(this[file])
    }

    private fun getAtlas(key: TextureKey): TextureAtlas {
        return textures.getOrPut(key) {
            TextureAtlas().also {
                it.texture.setFilter(key.filter, key.filter)
            }
        }
    }

    override fun dispose() {
        textures.forEach { (_, texture) ->
            texture.dispose()
        }
    }

    operator fun invoke(file: String, filter: Texture.Filter = Texture.Filter.NEAREST) = Getter(file, filter)
}
