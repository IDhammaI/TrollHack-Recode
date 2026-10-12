package dev.luna5ama.trollhack.gui.clickgui.component

import dev.luna5ama.trollhack.gui.rgui.Component.UiSettingGroup
import dev.luna5ama.trollhack.gui.rgui.windows.ListWindow
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.ModuleManager

class CategoryWindow(
    val category: Category,
    onOpenSettings: (ModuleButton) -> Unit
) : ListWindow(category.displayName.toString(), UiSettingGroup.CLICK_GUI) {

    init {
        // Every module in the category is listed, including visible = false ones.
        // AbstractModule.isVisible only controls the ActiveModules HUD list; filtering on it here
        // would make GuiSetting, ChatSetting, CombatSetting and 19 others unreachable.
        for (module in ModuleManager.modules) {
            if (module.category != category) continue
            children.add(ModuleButton(module, onOpenSettings))
        }
    }

    /** Sizes to fit the module list; called once the font metrics are available. */
    fun updateSize(maxHeight: Float) {
        for (child in children) {
            child.height = child.minHeight
        }
        updateChildPosSize()
        width = optimalWidth
        height = optimalHeight.coerceAtMost(maxHeight)
    }

    /** Filters module buttons by the search string (empty string shows all). */
    fun applySearch(search: String) {
        val query = search.replace(" ", "")
        for (child in children) {
            val button = child as? ModuleButton ?: continue
            button.visible = query.isEmpty()
                || button.module.name.toString().replace(" ", "").contains(query, ignoreCase = true)
                || button.module.internalName.contains(query, ignoreCase = true)
        }
    }
}
