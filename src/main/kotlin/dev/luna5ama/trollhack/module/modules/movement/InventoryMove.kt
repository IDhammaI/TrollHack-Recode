package dev.luna5ama.trollhack.module.modules.movement

import com.mojang.blaze3d.platform.InputConstants
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.KeyboardUtils
import dev.luna5ama.trollhack.util.accessor.setMoveVector
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.player.KeyboardInput
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec2

/**
 * 26.2 port notes / degradations:
 * - 1.12's `MovementInput.moveForward` / `moveStrafe` / `forwardKeyDown` / ... are gone. 26.2 keeps the
 *   pressed keys in the immutable [Input] record ([net.minecraft.client.player.ClientInput.keyPresses]) and
 *   the analog impulses in `moveVector`, so a whole new [Input] is built and the move vector is written
 *   through the existing `AccessorClientInput`. The vector is normalized exactly like
 *   [KeyboardInput.tick] does so diagonal movement matches vanilla 26.2.
 * - The `AbstractTrollGui.searching` check is dropped: this port's TrollHack GUIs keep their search string
 *   private to `WindowScreen`, so it cannot be read from here. Instead **every** TrollHack GUI is treated as
 *   an invalid GUI, i.e. this module simply does nothing while the ClickGUI / HUD editor is open, rather
 *   than only while a search is being typed.
 * - The "Key is bound to a mouse button!" error path is dropped. It only existed because LWJGL 2's
 *   `Keyboard.isKeyDown` threw on the negative key codes 1.12 used for mouse binds; GLFW reads mouse
 *   buttons fine, so mouse-bound movement keys are now supported instead of erroring out.
 */
internal object InventoryMove : Module(
    name = "Inventory Move",
    description = "Allows you to walk around with GUIs opened",
    category = Category.MOVEMENT
) {
    private val rotateSpeed by setting("Rotate Speed", 5, 0..20, 1)
    val sneak by setting("Sneak", false)

    init {
        safeListener<InputUpdateEvent>(9999) {
            val input = it.movementInput
            if (input !is KeyboardInput || isInvalidGui(mc.gui.screen())) return@safeListener

            if (KeyboardUtils.isKeyDown(InputConstants.KEY_LEFT)) {
                player.yRot = player.yRot - rotateSpeed
            }
            if (KeyboardUtils.isKeyDown(InputConstants.KEY_RIGHT)) {
                player.yRot = player.yRot + rotateSpeed
            }

            // pitch can not exceed 90 degrees nor -90 degrees, otherwise AAC servers will flag this and kick you.
            if (KeyboardUtils.isKeyDown(InputConstants.KEY_UP)) {
                player.xRot = (player.xRot - rotateSpeed).coerceAtLeast(-90.0f)
            }
            if (KeyboardUtils.isKeyDown(InputConstants.KEY_DOWN)) {
                player.xRot = (player.xRot + rotateSpeed).coerceAtMost(90.0f)
            }

            val options = mc.options
            val forward = isKeyDown(options.keyUp)
            val backward = isKeyDown(options.keyDown)
            val left = isKeyDown(options.keyLeft)
            val right = isKeyDown(options.keyRight)

            val prev = input.keyPresses
            input.keyPresses = Input(
                forward,
                backward,
                left,
                right,
                prev.jump() || isKeyDown(options.keyJump),
                prev.shift() || (sneak && isKeyDown(options.keyShift)),
                prev.sprint()
            )

            var moveStrafe = 0.0f
            var moveForward = 0.0f

            if (forward) ++moveForward
            if (backward) --moveForward
            if (left) ++moveStrafe
            if (right) --moveStrafe

            input.setMoveVector(Vec2(moveStrafe, moveForward).normalized())
        }
    }

    /**
     * [KeyMapping.isDown] is always false while a screen is open, so the raw GLFW state of the bound key
     * has to be polled instead. 26.2's [KeyMapping] exposes no getter for its current key, but
     * [KeyMapping.saveString] returns the key's registered name which [InputConstants.getKey] resolves back.
     */
    private fun isKeyDown(keyMapping: KeyMapping): Boolean {
        val key = InputConstants.getKey(keyMapping.saveString())
        if (key.value < 0) return false

        return when (key.type) {
            InputConstants.Type.KEYSYM -> KeyboardUtils.isKeyDown(key.value)
            InputConstants.Type.MOUSE -> KeyboardUtils.isKeyDown(-key.value - 1)
            else -> false
        }
    }

    private fun isInvalidGui(screen: Screen?) = screen == null
        || screen is ChatScreen
        || screen is AbstractSignEditScreen
        || screen is AnvilScreen
        || screen is AbstractTrollGui
}
