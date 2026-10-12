package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.event.IListenerOwner
import dev.luna5ama.trollhack.event.ListenerOwner
import dev.luna5ama.trollhack.graphics.HAlign
import dev.luna5ama.trollhack.graphics.VAlign
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.setting.ConfigManager
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.translation.ITranslateSrc
import dev.luna5ama.trollhack.translation.TranslateSrc
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.interfaces.Alias
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Base of all HUD elements.
 * Position is stored dock-relative ("Pos X"/"Pos Y" offsets from the docking anchor)
 * so elements stay glued to their corner across resolution changes; setting names and
 * the "hud_gui" config group match the original for config compatibility.
 */
abstract class AbstractHudElement(
    name: String,
    alias: Array<String> = emptyArray(),
    val category: Category,
    description: String,
    val alwaysListening: Boolean = false,
    enabledByDefault: Boolean = false,
    defaultDockingH: HAlign = HAlign.LEFT,
    defaultDockingV: VAlign = VAlign.TOP,
    defaultPosX: Float = 2.0f,
    defaultPosY: Float = 2.0f
) : Component(name, UiSettingGroup.HUD_GUI),
    Alias,
    IListenerOwner by ListenerOwner(),
    ITranslateSrc by TranslateSrc("hud_${name.replace(" ", "_")}") {

    final override val alias: Array<out CharSequence> = arrayOf(internalName, *alias)

    val description = TranslateType.SPECIFIC key ("description" to description)

    protected val visibleSetting = setting("Visible", enabledByDefault, { false })
    open var visible by visibleSetting

    val bind by setting("Bind", Bind())
    val scale by setting("Scale", 1.0f, 0.1f..4.0f, 0.05f)

    private val dockingHSetting = setting("Docking H", defaultDockingH)
    private val dockingVSetting = setting("Docking V", defaultDockingV)
    private val posXSetting = setting("Pos X", defaultPosX, -69420.911f..69420.911f, 0.1f)
    private val posYSetting = setting("Pos Y", defaultPosY, -69420.911f..69420.911f, 0.1f)

    val default = setting("Default", false, isTransient = true)

    var dockingH by dockingHSetting
    var dockingV by dockingVSetting

    /** Size of the element in gui-scaled pixels before [scale] is applied. */
    abstract val hudWidth: Float
    abstract val hudHeight: Float

    protected val screenWidth get() = Wrapper.minecraft.window.guiScaledWidth.toFloat()
    protected val screenHeight get() = Wrapper.minecraft.window.guiScaledHeight.toFloat()

    /** Absolute top-left position in gui-scaled pixels. */
    var renderPosX: Float
        get() = posXSetting.value + (screenWidth - hudWidth * scale) * dockingH.multiplier
        set(value) {
            posXSetting.value = value - (screenWidth - hudWidth * scale) * dockingH.multiplier
        }

    var renderPosY: Float
        get() = posYSetting.value + (screenHeight - hudHeight * scale) * dockingV.multiplier
        set(value) {
            posYSetting.value = value - (screenHeight - hudHeight * scale) * dockingV.multiplier
        }

    val settingList
        get() = GuiConfig.getGroupOrPut(UiSettingGroup.HUD_GUI.groupName).getGroupOrPut(internalName).getSettings()

    open fun onRegister() {
        if (visible || alwaysListening) {
            subscribe()
        }
    }

    open fun getHudInfo(): String = ""

    open fun renderHud(graphics: GuiGraphicsExtractor) {}

    /** Draws the editor frame around the element; called by the HUD editor only. */
    fun renderFrame(graphics: GuiGraphicsExtractor) {
        val w = (hudWidth * scale).toInt()
        val h = (hudHeight * scale).toInt()
        val x = renderPosX.toInt()
        val y = renderPosY.toInt()
        graphics.fill(x, y, x + w, y + h, GuiColors.backGround.toArgb())
        graphics.outline(x, y, w, h, GuiColors.outline.toArgb())
    }

    fun isInside(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= renderPosX && mouseX <= renderPosX + hudWidth * scale
            && mouseY >= renderPosY && mouseY <= renderPosY + hudHeight * scale
    }

    init {
        visibleSetting.valueListeners.add { _, it ->
            if (it) {
                subscribe()
            } else if (!alwaysListening) {
                unsubscribe()
            }
        }

        // Re-encode the relative position when the user changes docking so the element doesn't jump.
        // Skipped during a config load: the file already holds a matching docking/position pair, and
        // the initial load runs from the client entrypoint before the game window exists.
        dockingHSetting.valueListeners.add { prev, new ->
            if (prev != new && !ConfigManager.loading) {
                posXSetting.value += (screenWidth - hudWidth * scale) * (prev.multiplier - new.multiplier)
            }
        }

        dockingVSetting.valueListeners.add { prev, new ->
            if (prev != new && !ConfigManager.loading) {
                posYSetting.value += (screenHeight - hudHeight * scale) * (prev.multiplier - new.multiplier)
            }
        }

        default.valueListeners.add { _, it ->
            if (it) {
                settingList.filter { it != visibleSetting && it != default }.forEach { it.resetValue() }
                default.value = false
                NoSpamMessage.sendMessage(Companion, "$name $defaultMessage!")
            }
        }
    }

    enum class Category(override val displayName: CharSequence) : DisplayEnum {
        CLIENT("Client"),
        COMBAT("Combat"),
        PLAYER("Player"),
        WORLD("World"),
        MISC("Misc")
    }

    protected companion object : ITranslateSrc by TranslateSrc("hud") {
        val defaultMessage = TranslateType.COMMON key ("setToDefault" to "Set to defaults")
    }
}
