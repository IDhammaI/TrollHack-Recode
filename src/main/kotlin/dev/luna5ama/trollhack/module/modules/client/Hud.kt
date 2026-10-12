package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

internal object Hud : Module(
    name = "Hud",
    description = "Renders the HUD elements",
    category = Category.CLIENT,
    visible = false,
    enabledByDefault = true
)
