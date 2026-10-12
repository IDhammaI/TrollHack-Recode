package dev.luna5ama.trollhack.core

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.*
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.events.player.InteractEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.events.player.PlayerTravelEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.events.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.graphics.ProjectionUtils
import dev.luna5ama.trollhack.graphics.Renderer3D
import dev.luna5ama.trollhack.manager.managers.MessageManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.module.modules.chat.ExtraChatHistory
import dev.luna5ama.trollhack.module.modules.client.CommandSetting
import dev.luna5ama.trollhack.module.modules.render.ExtraTab
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.ClientInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4fc

/**
 * Static entry points invoked from the Java mixins.
 * Keeps mixin classes thin; all client logic stays in Kotlin.
 */
object Hooks {
    private val renderer3D = Renderer3D()

    // ---- Game loop ----

    @JvmStatic
    fun overrideTickLength(defaultMs: Float): Float {
        return dev.luna5ama.trollhack.manager.managers.TimerManager.getModifiedTickLength(defaultMs)
    }

    @JvmStatic
    fun onRunTickStart() {
        // Modules restored as enabled by the config load have their setup held until here,
        // where Minecraft actually exists. Cheap no-op once the queue has drained.
        ModuleManager.runDeferredEnables()
        RunGameLoopEvent.Start.post()
    }

    @JvmStatic
    fun onRunTickEnd() {
        RunGameLoopEvent.End.post()
    }

    @JvmStatic
    fun onRunTickRender() {
        RunGameLoopEvent.Render.post()
    }

    @JvmStatic
    fun onTickPre() {
        RunGameLoopEvent.Tick.post()
        SafeClientEvent.update()
        TickEvent.Pre.post()
    }

    @JvmStatic
    fun onTickPost() {
        TickEvent.Post.post()
    }

    @JvmStatic
    fun onProcessKeyBindsPre() {
        ProcessKeyBindEvent.Pre.post()
    }

    @JvmStatic
    fun onProcessKeyBindsPost() {
        ProcessKeyBindEvent.Post.post()
    }

    // ---- Networking ----

    @JvmStatic
    fun onPacketSend(packet: Packet<*>): Boolean {
        val event = PacketEvent.Send(packet)
        event.post()
        return event.cancelled
    }

    @JvmStatic
    fun onPacketPostSend(packet: Packet<*>) {
        PacketEvent.PostSend(packet).post()
    }

    @JvmStatic
    fun onPacketReceive(packet: Packet<*>): Boolean {
        val event = PacketEvent.Receive(packet)
        event.post()
        return event.cancelled
    }

    @JvmStatic
    fun onPacketPostReceive(packet: Packet<*>) {
        PacketEvent.PostReceive(packet).post()
    }

    @JvmStatic
    fun onConnect() {
        ConnectionEvent.Connect.post()
    }

    @JvmStatic
    fun onDisconnect() {
        ConnectionEvent.Disconnect.post()
        SafeClientEvent.reset()
    }

    /**
     * @return true if the message was queued and the vanilla send should be cancelled
     */
    @JvmStatic
    fun onSendChat(content: String): Boolean {
        if (!TrollHackMod.ready) return false
        return MessageManager.onPlayerSendChat(content)
    }

    /**
     * @return how many chat lines the client should retain
     */
    @JvmStatic
    fun chatHistoryLimit(vanilla: Int): Int {
        if (!TrollHackMod.ready) return vanilla
        return ExtraChatHistory.limitOr(vanilla)
    }

    /**
     * @return how many players the tab list should show
     */
    @JvmStatic
    fun tabListLimit(vanilla: Long): Long {
        if (!TrollHackMod.ready) return vanilla
        return ExtraTab.limitOr(vanilla)
    }

    /**
     * @return the component to display, or null to hide the message
     */
    @JvmStatic
    fun onChatMessage(component: Component): Component? {
        if (!TrollHackMod.ready) return component
        val event = ChatEvent(component)
        event.post()
        return if (event.cancelled) null else event.component
    }

    // ---- World ----

    @JvmStatic
    fun onWorldLoad() {
        WorldEvent.Load.post()
    }

    @JvmStatic
    fun onWorldUnload() {
        WorldEvent.Unload.post()
        SafeClientEvent.reset()
    }

    @JvmStatic
    fun onEntityAdd(entity: net.minecraft.world.entity.Entity) {
        WorldEvent.Entity.Add(entity).post()
    }

    @JvmStatic
    fun onEntityRemove(entity: net.minecraft.world.entity.Entity) {
        WorldEvent.Entity.Remove(entity).post()
    }

    /** Client-side block change; 26.2 replacement for the old IWorldEventListener hook. */
    @JvmStatic
    fun onClientBlockUpdate(
        pos: net.minecraft.core.BlockPos,
        oldState: net.minecraft.world.level.block.state.BlockState,
        newState: net.minecraft.world.level.block.state.BlockState
    ) {
        if (!TrollHackMod.ready) return
        WorldEvent.ClientBlockUpdate(pos.immutable(), oldState, newState).post()
    }

    // ---- Chat / commands ----

    /**
     * @return true if the message was a client command and should not reach the server
     */
    @JvmStatic
    fun onChatInput(message: String): Boolean {
        if (message.startsWith(CommandSetting.prefix)) {
            CommandManager.runCommand(message.removePrefix(CommandSetting.prefix))
            return true
        }
        return false
    }

    // ---- Block interaction ----

    /**
     * @return true if the interaction was cancelled by a listener
     */
    @JvmStatic
    fun onBlockLeftClick(pos: net.minecraft.core.BlockPos, side: net.minecraft.core.Direction): Boolean {
        if (!TrollHackMod.ready) return false
        val event = InteractEvent.Block.LeftClick(pos, side)
        event.post()
        return event.cancelled
    }

    @JvmStatic
    fun onBlockDamage(pos: net.minecraft.core.BlockPos, side: net.minecraft.core.Direction): Boolean {
        if (!TrollHackMod.ready) return false
        val event = InteractEvent.Block.Damage(pos, side)
        event.post()
        return event.cancelled
    }

    @JvmStatic
    fun onBlockRightClick(
        pos: net.minecraft.core.BlockPos,
        side: net.minecraft.core.Direction,
        hand: net.minecraft.world.InteractionHand
    ): Boolean {
        if (!TrollHackMod.ready) return false
        val event = InteractEvent.Block.RightClick(pos, side, hand)
        event.post()
        return event.cancelled
    }

    // ---- Input ----

    @JvmStatic
    fun onKeyboardInput(key: Int, pressed: Boolean) {
        InputEvent.Keyboard(key, pressed).post()
    }

    @JvmStatic
    fun onMouseInput(button: Int, pressed: Boolean) {
        InputEvent.Mouse(button, pressed).post()
    }

    // ---- Screens ----

    @JvmStatic
    fun onScreenDisplayed(screen: Screen?): Screen? {
        val event = GuiEvent.Displayed(screen)
        event.post()
        return event.screen
    }

    @JvmStatic
    fun onScreenClosed(screen: Screen) {
        GuiEvent.Closed(screen).post()
    }

    // ---- Rendering ----

    @JvmStatic
    fun onRender2D(guiRenderState: GuiRenderState, partialTicks: Float) {
        val mc = Minecraft.getInstance()
        if (mc.level == null || mc.player == null) return

        val graphics = GuiGraphicsExtractor(mc, guiRenderState, 0, 0)
        graphics.nextStratum()
        Render2DEvent.Troll(graphics, partialTicks).post()
    }

    @JvmStatic
    fun onRender3D(cameraPos: Vec3, viewMatrix: Matrix4fc, projectionMatrix: Matrix4fc, partialTicks: Float) {
        val mc = Minecraft.getInstance()
        if (mc.level == null || mc.player == null) return

        ProjectionUtils.update(cameraPos, viewMatrix, projectionMatrix)
        renderer3D.begin(cameraPos, viewMatrix)
        Render3DEvent(renderer3D, partialTicks).post()
        renderer3D.flush()
    }

    @JvmStatic
    fun onResolutionUpdate(width: Int, height: Int) {
        ResolutionUpdateEvent(width, height).post()
    }

    // ---- Player ----

    @JvmStatic
    fun onPlayerTravel(): Boolean {
        val event = PlayerTravelEvent()
        event.post()
        return event.cancelled
    }

    @JvmStatic
    fun onInputUpdate(input: ClientInput) {
        InputUpdateEvent(input).post()
    }

    @JvmStatic
    fun onPlayerMovePre(player: LocalPlayer, movement: Vec3): Vec3 {
        val event = PlayerMoveEvent.Pre(player)
        event.post()

        return if (event.cancelled || event.isModified) {
            Vec3(event.x, event.y, event.z)
        } else {
            movement
        }
    }

    @JvmStatic
    fun onPlayerMovePost() {
        PlayerMoveEvent.Post.post()
    }

    @JvmStatic
    fun onUpdateWalkingPlayerPre(player: LocalPlayer): OnUpdateWalkingPlayerEvent.Pre {
        val event = OnUpdateWalkingPlayerEvent.Pre(
            player.position(),
            player.yRot,
            player.xRot,
            player.onGround()
        )
        PlayerPacketManager.applyPacket(event)
        event.post()
        return event
    }

    @JvmStatic
    fun onUpdateWalkingPlayerPost(event: OnUpdateWalkingPlayerEvent.Pre) {
        OnUpdateWalkingPlayerEvent.Post(event.position, event.rotation, event.onGround).post()
    }

    @JvmStatic
    fun ignorePlayerPacket(packet: net.minecraft.network.protocol.game.ServerboundMovePlayerPacket) {
        PlayerPacketManager.ignoreUpdate(packet)
    }
}
