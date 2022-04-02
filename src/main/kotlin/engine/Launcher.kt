package engine

import com.gratedgames.*

private const val CONFIG_FILE = "config.txt"

fun main() {
    val configuration = Configuration()

    configuration.title = "Game"
    configuration.icons = arrayOf("icons/icon.png")

    configuration.readFromFile(CONFIG_FILE)

    Kore.start(Game, configuration) { DesktopPlatform() }

    configuration.writeToFile(CONFIG_FILE)
}
