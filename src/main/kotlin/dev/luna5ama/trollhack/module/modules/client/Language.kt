package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.GuiEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.setting.GenericConfig
import dev.luna5ama.trollhack.translation.TranslationManager
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.gui.screens.TitleScreen

internal object Language : AbstractModule(
    name = "Language",
    description = "Change language",
    category = Category.CLIENT,
    alwaysEnabled = true,
    visible = false,
    config = GenericConfig
) {
    private val overrideLanguage = setting("Override Language", false)
    private val language = setting("Language", "en_us", { overrideLanguage.value })

    init {
        listener<GuiEvent.Displayed>(114514) {
            if (it.screen is TitleScreen) {
                TranslationManager.reload()
            }
        }
    }

    val settingLanguage: String
        get() = if (overrideLanguage.value) {
            language.value
        } else {
            // options is still null while the client entrypoint runs inside Minecraft's constructor
            @Suppress("UNNECESSARY_SAFE_CALL")
            Wrapper.minecraft.options?.languageCode ?: "en_us"
        }

    init {
        overrideLanguage.listeners.add {
            TranslationManager.reload()
        }

        language.listeners.add {
            TranslationManager.reload()
        }
    }
}
