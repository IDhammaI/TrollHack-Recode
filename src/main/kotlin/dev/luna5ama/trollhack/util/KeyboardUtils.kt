package dev.luna5ama.trollhack.util

import com.mojang.blaze3d.platform.InputConstants
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import java.lang.reflect.Modifier
import java.util.*

/**
 * GLFW based keyboard utilities.
 *
 * Key codes are GLFW key codes (32..348). Mouse buttons are encoded
 * as negative values (-button - 1) like the original TrollHack.
 */
object KeyboardUtils {
    const val KEY_NONE = -1

    private val keyToName = TreeMap<Int, String>()
    private val nameToKey = HashMap<String, Int>()

    val allKeys: Set<Int> get() = keyToName.keys

    init {
        // Build the key name tables from the GLFW_KEY_* constants
        for (field in GLFW::class.java.declaredFields) {
            if (!Modifier.isStatic(field.modifiers)) continue
            if (!field.name.startsWith("GLFW_KEY_")) continue

            val key = field.getInt(null)
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue

            val name = field.name.removePrefix("GLFW_KEY_")
            keyToName.putIfAbsent(key, name)
            nameToKey[name.lowercase(Locale.ROOT)] = key
        }

        // Aliases matching LWJGL 2 style names used by the original TrollHack configs
        alias("LCONTROL", GLFW.GLFW_KEY_LEFT_CONTROL)
        alias("RCONTROL", GLFW.GLFW_KEY_RIGHT_CONTROL)
        alias("LSHIFT", GLFW.GLFW_KEY_LEFT_SHIFT)
        alias("RSHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT)
        alias("LMENU", GLFW.GLFW_KEY_LEFT_ALT)
        alias("RMENU", GLFW.GLFW_KEY_RIGHT_ALT)
        alias("LMETA", GLFW.GLFW_KEY_LEFT_SUPER)
        alias("RMETA", GLFW.GLFW_KEY_RIGHT_SUPER)
        alias("RETURN", GLFW.GLFW_KEY_ENTER)
        alias("ESCAPE", GLFW.GLFW_KEY_ESCAPE)
        alias("BACK", GLFW.GLFW_KEY_BACKSPACE)
        alias("CAPITAL", GLFW.GLFW_KEY_CAPS_LOCK)
        alias("GRAVE", GLFW.GLFW_KEY_GRAVE_ACCENT)
        alias("PRIOR", GLFW.GLFW_KEY_PAGE_UP)
        alias("NEXT", GLFW.GLFW_KEY_PAGE_DOWN)
    }

    private fun alias(name: String, key: Int) {
        nameToKey[name.lowercase(Locale.ROOT)] = key
    }

    fun getKey(name: String): Int {
        return nameToKey[name.trim().lowercase(Locale.ROOT)] ?: KEY_NONE
    }

    fun getDisplayName(key: Int): String? {
        return keyToName[key]
    }

    fun getKeyName(key: Int): String {
        return keyToName[key] ?: "None"
    }

    @JvmStatic
    fun isKeyDown(key: Int): Boolean {
        val mc = Minecraft.getInstance()
        val window = mc.window ?: return false

        return if (key >= 0) {
            InputConstants.isKeyDown(window, key)
        } else {
            GLFW.glfwGetMouseButton(window.handle(), -key - 1) == GLFW.GLFW_PRESS
        }
    }

    fun sendUnknownKeyError(name: String) {
        NoSpamMessage.sendError("Unknown key name ${formatValue(name)}!")
    }
}
