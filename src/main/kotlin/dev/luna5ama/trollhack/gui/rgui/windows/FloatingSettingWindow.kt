package dev.luna5ama.trollhack.gui.rgui.windows

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.gui.rgui.component.Slider
import dev.luna5ama.trollhack.setting.groups.SettingGroup
import dev.luna5ama.trollhack.util.ClipboardUtils
import dev.luna5ama.trollhack.util.KeyboardUtils
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min

/**
 * Floating window listing setting rows; auto-fits its content and spawns beside the mouse.
 */
abstract class FloatingSettingWindow(
    name: String,
    components: List<InteractiveComponent>,
    screenWidth: Float,
    screenHeight: Float,
    mouseX: Float,
    mouseY: Float
) : ListWindow(name) {

    final override val minimizable = false

    override val keyboardListening: Boolean
        get() = children.any { it is Slider && it.listeningState }

    private val heightLimit = max(60.0f, screenHeight - 12.0f)

    /**
     * The config group Ctrl+C / Ctrl+V copy and paste, as JSON. Null disables it.
     */
    protected open val copyableSettingGroup: SettingGroup? = null

    /**
     * Ctrl+C copies this panel's whole configuration to the clipboard and Ctrl+V pastes one
     * back, so a module setup can be moved between presets or shared. Skipped while a child is
     * capturing keys, so typing into a text or bind field is not swallowed.
     */
    override fun onKeyInput(key: Int, state: Boolean): Boolean {
        if (super.onKeyInput(key, state)) return true
        if (!state || keyboardListening) return false

        if (!KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)
            && !KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)
        ) {
            return false
        }

        val group = copyableSettingGroup ?: return false

        try {
            when (key) {
                GLFW.GLFW_KEY_C -> {
                    ClipboardUtils.copyToClipboard(gson.toJson(group.write()))
                    return true
                }

                GLFW.GLFW_KEY_V -> {
                    val json = ClipboardUtils.pasteFromClipboard()
                    if (json.isNotBlank()) {
                        group.read(gson.fromJson(json, JsonObject::class.java))
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            NoSpamMessage.sendError("Failed to copy/paste settings")
            TrollHackMod.logger.error("Failed to copy/paste settings", e)
            return true
        }

        return false
    }

    private companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }

    init {
        children.addAll(components)
        for (child in children) {
            child.height = child.minHeight
        }
        updateChildPosSize()

        width = optimalWidth
        height = optimalHeight.coerceAtMost(heightLimit)

        posX = if (mouseX + width <= screenWidth) mouseX else max(0.0f, mouseX - width)
        posY = min(mouseY, max(0.0f, screenHeight - height))
    }

    /**
     * Re-fit to the content every tick, matching the original's `minHeight/maxHeight by
     * ::optimalHeight`. Conditional setting rows appear and disappear as their condition flips,
     * so a height fixed at construction would clip the new rows or leave a dead gap.
     */
    override fun onTick() {
        super.onTick()

        val target = optimalHeight.coerceAtMost(heightLimit)
        if (target != height) {
            height = target
            updateChildPosSize()
        }
    }
}
