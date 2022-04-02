package engine.utils

import com.gratedgames.utils.Properties

interface Savable {
    fun read(properties: Properties)
    fun write(properties: Properties)
}