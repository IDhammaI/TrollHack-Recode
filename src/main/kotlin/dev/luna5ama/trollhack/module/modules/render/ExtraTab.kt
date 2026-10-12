package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

/**
 * 26.2 truncates the tab list with a stream limit inside PlayerTabOverlay.getPlayerInfos
 * instead of the sublist the 1.12 version swapped out, so the cap is raised there.
 */
internal object ExtraTab : Module(
    name = "Extra Tab",
    description = "Expands the player tab menu",
    category = Category.RENDER
) {
    private val tabSize by setting("Max Players", 265, 80..400, 5)

    override fun getHudInfo(): String {
        return tabSize.toString()
    }

    @JvmStatic
    fun limitOr(vanilla: Long): Long {
        return if (isEnabled) tabSize.toLong() else vanilla
    }
}
