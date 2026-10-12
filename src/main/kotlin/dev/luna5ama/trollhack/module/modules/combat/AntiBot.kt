package dev.luna5ama.trollhack.module.modules.combat

import com.google.common.collect.MapMaker
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.player.PlayerAttackEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.UUIDManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import kotlin.math.abs

internal object AntiBot : Module(
    name = "Anti Bot",
    description = "Avoid attacking fake players",
    category = Category.COMBAT
) {
    private val tabList by setting("Tab List", true)
    private val ping by setting("Ping", true)
    private val hp by setting("HP", true)
    private val sleeping by setting("Sleeping", false)
    private val hoverOnTop by setting("Hover On Top", true)
    private val ticksExists by setting("Ticks Exists", 200, 0..500, 10)
    private val mojangApi by setting("Mojang API", true)

    private val botMap = MapMaker().weakKeys().makeMap<Player, Boolean>()
    private var count = 0

    override fun getHudInfo(): String {
        return count.toString()
    }

    init {
        onDisable {
            botMap.clear()
            count = 0
        }

        listener<ConnectionEvent.Disconnect> {
            botMap.clear()
        }

        listener<PlayerAttackEvent> {
            if (isBot(it.entity)) it.cancel()
        }

        safeListener<WorldEvent.Entity.Add> {
            if (it.entity is Player) {
                val isBot = checkBot(it.entity)
                botMap[it.entity] = isBot
            }
        }

        safeConcurrentListener<TickEvent.Pre> {
            var newCount = 0

            for (entity in EntityManager.players) {
                val isBot = checkBot(entity)
                botMap[entity] = isBot
                if (isBot) newCount++
            }

            count = newCount
        }
    }

    fun isBot(entity: Entity): Boolean {
        return isEnabled
            && entity is Player
            && !entity.isFakeOrSelf
            && botMap[entity] ?: true
    }

    private fun SafeClientEvent.checkBot(entity: Player): Boolean {
        val entityName = entity.gameProfile.name
        return (entityName == player.gameProfile.name
            || tabList && connection.getPlayerInfo(entityName) == null
            || ping && (connection.getPlayerInfo(entityName)?.latency ?: -1) <= 0
            || hp && entity.health !in 0.0f..20.0f
            || sleeping && entity.isSleeping && !entity.onGround()
            || hoverOnTop && hoverCheck(entity)
            || entity.tickCount < ticksExists
            || mojangApi && UUIDManager.getByName(entityName, true) == null)
    }

    private fun SafeClientEvent.hoverCheck(entity: Player): Boolean {
        val distXZ = Vec2d(entity.x, entity.z).minus(player.x, player.z).lengthSq()
        return distXZ < 16 && entity.y - player.y > 2.0 && abs(entity.y - entity.yOld) < 0.1
    }
}
