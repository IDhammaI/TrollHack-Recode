package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

/**
 * @see dev.luna5ama.trollhack.mixins.MixinLevelWeather
 */
internal object AntiWeather : Module(
    name = "Anti Weather",
    description = "Removes rain and thunder from your world",
    category = Category.MISC
) {
    @JvmStatic
    fun isModuleEnabled() = isEnabled
}
