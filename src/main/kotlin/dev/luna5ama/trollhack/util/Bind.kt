package dev.luna5ama.trollhack.util

import it.unimi.dsi.fastutil.ints.*
import org.lwjgl.glfw.GLFW

class Bind(
    modifierKeysIn: IntAVLTreeSet,
    keyIn: Int
) {

    constructor() : this(KeyboardUtils.KEY_NONE)

    constructor(key: Int) : this(IntAVLTreeSet(keyComparator), key)

    constructor(
        vararg modifierKeys: Int,
        key: Int
    ) : this(IntAVLTreeSet(keyComparator).apply { modifierKeys.forEach { add(it) } }, key)

    val modifierKeys = modifierKeysIn
    var key = keyIn; private set
    private var cachedName = getName()

    val isEmpty get() = key !in VALID_KEY_RANGE && key !in VALID_MOUSE_RANGE

    fun isDown(): Boolean {
        return !isEmpty
            && KeyboardUtils.isKeyDown(key)
            && synchronized(this) { modifierKeys.all { isModifierKeyDown(key, it) } }
    }

    fun isDown(eventKey: Int): Boolean {
        return !isEmpty
            && key == eventKey
            && synchronized(this) { modifierKeys.all { isModifierKeyDown(eventKey, it) } }
    }

    private fun isModifierKeyDown(eventKey: Int, modifierKey: Int) =
        eventKey != modifierKey
            && when (modifierKey) {
            GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> {
                KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)
            }
            GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> {
                KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT)
            }
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> {
                KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)
            }
            GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> {
                KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SUPER) || KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SUPER)
            }
            in VALID_KEY_RANGE -> {
                KeyboardUtils.isKeyDown(modifierKey)
            }
            else -> {
                false
            }
        }

    fun setBind(keyIn: Int) {
        val cache = IntArrayList(0)

        for (key in KeyboardUtils.allKeys) {
            if (key == keyIn) continue
            if (!KeyboardUtils.isKeyDown(key)) continue
            cache.add(key)
        }

        setBind(cache, keyIn)
    }

    fun setBind(modifierKeysIn: IntCollection, keyIn: Int) {
        synchronized(this) {
            modifierKeys.clear()
            modifierKeys.addAll(modifierKeysIn)
            key = keyIn
            cachedName = getName()
        }
    }

    fun clear() {
        synchronized(this) {
            modifierKeys.clear()
            key = KeyboardUtils.KEY_NONE
            cachedName = getName()
        }
    }

    override fun toString(): String {
        return cachedName
    }

    private fun getName(): String {
        return if (isEmpty) {
            "None"
        } else {
            StringBuilder().run {
                for (key in modifierKeys) {
                    val name = modifierName[key] ?: KeyboardUtils.getDisplayName(key) ?: continue
                    append(name)
                    append('+')
                }
                if (key >= 0) {
                    append(KeyboardUtils.getDisplayName(key))
                } else {
                    append("Mouse")
                    append(' ')
                    append(-key - 1)
                }
                toString()
            }
        }
    }

    companion object {
        val VALID_KEY_RANGE = 32..348
        val VALID_MOUSE_RANGE = -16..-1

        private val modifierName = Int2ObjectLinkedOpenHashMap<String>().apply {
            put(GLFW.GLFW_KEY_LEFT_CONTROL, "Ctrl")
            put(GLFW.GLFW_KEY_RIGHT_CONTROL, "Ctrl")
            put(GLFW.GLFW_KEY_LEFT_ALT, "Alt")
            put(GLFW.GLFW_KEY_RIGHT_ALT, "Alt")
            put(GLFW.GLFW_KEY_LEFT_SHIFT, "Shift")
            put(GLFW.GLFW_KEY_RIGHT_SHIFT, "Shift")
            put(GLFW.GLFW_KEY_LEFT_SUPER, "Meta")
            put(GLFW.GLFW_KEY_RIGHT_SUPER, "Meta")
        }

        private val priorityMap = Int2IntLinkedOpenHashMap().apply {
            val priorityKey = arrayOf(
                GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
                GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
                GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER
            )

            for ((index, key) in priorityKey.withIndex()) {
                this[key] = index / 2
            }

            val sortedKeys = KeyboardUtils.allKeys.sortedBy { KeyboardUtils.getKeyName(it) }

            for ((index, key) in sortedKeys.withIndex()) {
                this.putIfAbsent(key, index + priorityKey.size / 2)
            }
        }

        val keyComparator = compareBy<Int> {
            priorityMap[it]
        }
    }
}
