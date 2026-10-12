package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.modules.combat.AntiBot
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.combat.CombatUtils.totalHealth
import dev.luna5ama.trollhack.util.math.vector.distanceTo

internal object TextRadar : LabelHud(
    name = "Text Radar",
    category = Category.WORLD,
    description = "Displays nearby players"
) {
    private val health by setting("Health", true)
    private val distance by setting("Distance", true)
    private val friend by setting("Friend", true)
    private val maxEntries by setting("Max Entries", 8, 4..32, 1)
    private val range by setting("Range", 64, 16..512, 16)

    private val colorGood = ColorRGB(80, 255, 80)
    private val colorMid = ColorRGB(255, 220, 60)
    private val colorBad = ColorRGB(255, 80, 80)

    override fun SafeClientEvent.updateText() {
        var shown = 0

        val players = EntityManager.players.asSequence()
            .filter { !it.isFakeOrSelf }
            .filter { friend || !FriendManager.isFriend(it.gameProfile.name) }
            .filter { AntiBot.isDisabled || !AntiBot.isBot(it) }
            .map { it to player.distanceTo(it) }
            .filter { it.second <= range }
            .sortedBy { it.second }
            .toList()

        for ((target, dist) in players) {
            if (shown >= maxEntries) {
                displayText.addLine("...and ${players.size - shown} more", GuiColors.primary)
                break
            }

            if (health) {
                val hp = target.totalHealth
                val color = when {
                    hp > 15.0f -> colorGood
                    hp > 7.5f -> colorMid
                    else -> colorBad
                }
                displayText.add("%.0f".format(hp), color)
            }

            val nameColor = if (FriendManager.isFriend(target.gameProfile.name)) colorGood else GuiColors.text
            displayText.add(target.gameProfile.name, nameColor)

            if (distance) {
                displayText.add("${dist.toInt()}m", GuiColors.primary)
            }

            displayText.newLine()
            shown++
        }
    }
}
