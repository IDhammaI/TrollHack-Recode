package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

/**
 * @see dev.luna5ama.trollhack.mixins.MixinFramerateLimitTracker
 */
internal object UnfocusedFps : Module(
    name = "Unfocused Fps",
    description = "Reduces FPS when the game is running in the background",
    category = Category.MISC
) {
    private val fpsLimit by setting("FPS Limit", 30, 1..120, 1)

    /** Returns the limit to force, or -1 when the module should not apply. */
    @JvmStatic
    fun getUnfocusedLimit(): Int {
        if (isDisabled) return -1
        if (mc.isWindowActive) return -1
        return fpsLimit
    }
}
