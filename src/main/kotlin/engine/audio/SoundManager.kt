package engine.audio

import com.gratedgames.*
import com.gratedgames.audio.Sound
import com.gratedgames.files.Files
import com.gratedgames.utils.Disposable
import com.gratedgames.utils.extensions.extension
import com.gratedgames.utils.use
import kotlin.reflect.KProperty

class SoundManager : Disposable {
    inner class Getter(val file: String) {
        operator fun getValue(thisRef: Any, property: KProperty<*>) = getOrAdd(file)
    }

    private val sounds = hashMapOf<String, Sound>()

    fun add(file: String) {
        if (!Kore.files.exists(file, Files.Type.ASSET)) {
            Kore.log.error(this::class, "Sound file not found: $file")
            return
        }

        val sound = Kore.files.readAsset(file).use {
            Kore.audio.readSound(it, file.extension)
        }

        if (sound == null) {
            Kore.log.error(this::class, "Failed to load sound file: $file")
            return
        }

        add(file, sound)
    }

    operator fun contains(file: String) = file in sounds

    fun add(file: String, sound: Sound) {
        sounds[file] = sound
    }

    fun remove(file: String) {
        sounds.remove(file)
    }

    operator fun get(file: String): Sound? {
        return sounds[file]
    }

    fun getOrAdd(file: String): Sound {
        if (file !in this)
            add(file)

        return requireNotNull(this[file])
    }

    override fun dispose() {
        sounds.forEach { (_, sound) ->
            if (sound is Disposable)
                sound.dispose()
        }
    }

    operator fun invoke(file: String) = Getter(file)
}