package dev.luna5ama.trollhack.module

import dev.luna5ama.trollhack.module.modules.chat.AntiSpam
import dev.luna5ama.trollhack.module.modules.chat.AutoCope
import dev.luna5ama.trollhack.module.modules.chat.AutoQMain
import dev.luna5ama.trollhack.module.modules.chat.AutoReply
import dev.luna5ama.trollhack.module.modules.chat.ChatSuffix
import dev.luna5ama.trollhack.module.modules.chat.ChatTimestamp
import dev.luna5ama.trollhack.module.modules.chat.ExtraChatHistory
import dev.luna5ama.trollhack.module.modules.chat.FancyChat
import dev.luna5ama.trollhack.module.modules.chat.LoginMessage
import dev.luna5ama.trollhack.module.modules.chat.PortalChat
import dev.luna5ama.trollhack.module.modules.chat.Spammer
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.module.modules.client.ChatSetting
import dev.luna5ama.trollhack.module.modules.client.CommandSetting
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.module.modules.client.Hud
import dev.luna5ama.trollhack.module.modules.client.HudEditor
import dev.luna5ama.trollhack.module.modules.client.Configurations
import dev.luna5ama.trollhack.module.modules.client.Language
import dev.luna5ama.trollhack.module.modules.combat.AntiBot
import dev.luna5ama.trollhack.module.modules.combat.AutoCity
import dev.luna5ama.trollhack.module.modules.combat.AutoEatPlaceholder
import dev.luna5ama.trollhack.module.modules.combat.BedAura
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.module.modules.combat.CrystalESP
import dev.luna5ama.trollhack.module.modules.combat.HoleESP
import dev.luna5ama.trollhack.module.modules.combat.KillAura
import dev.luna5ama.trollhack.module.modules.combat.MidClickPearl
import dev.luna5ama.trollhack.module.modules.combat.Surround
import dev.luna5ama.trollhack.module.modules.combat.TotemPopCounter
import dev.luna5ama.trollhack.module.modules.combat.TrollAura
import dev.luna5ama.trollhack.module.modules.combat.VisualRange
import dev.luna5ama.trollhack.module.modules.exploit.AntiAntiBurrow
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.exploit.MountBypass
import dev.luna5ama.trollhack.module.modules.exploit.SandPop
import dev.luna5ama.trollhack.module.modules.exploit.WallClip
import dev.luna5ama.trollhack.module.modules.movement.AntiHunger
import dev.luna5ama.trollhack.module.modules.movement.AntiLevitation
import dev.luna5ama.trollhack.module.modules.movement.AutoCenter
import dev.luna5ama.trollhack.module.modules.movement.AutoJump
import dev.luna5ama.trollhack.module.modules.movement.AutoRemount
import dev.luna5ama.trollhack.module.modules.movement.Flight
import dev.luna5ama.trollhack.module.modules.movement.InventoryMove
import dev.luna5ama.trollhack.module.modules.movement.ReverseStep
import dev.luna5ama.trollhack.module.modules.movement.SafeWalk
import dev.luna5ama.trollhack.module.modules.movement.Sprint
import dev.luna5ama.trollhack.module.modules.player.AutoEject
import dev.luna5ama.trollhack.module.modules.player.FastBreak
import dev.luna5ama.trollhack.module.modules.player.FastUse
import dev.luna5ama.trollhack.module.modules.player.GhostHand
import dev.luna5ama.trollhack.module.modules.player.HandSwing
import dev.luna5ama.trollhack.module.modules.player.HotbarRefill
import dev.luna5ama.trollhack.module.modules.player.ItemSaver
import dev.luna5ama.trollhack.module.modules.player.MotionLogger
import dev.luna5ama.trollhack.module.modules.player.NoFall
import dev.luna5ama.trollhack.module.modules.player.NoPacketKick
import dev.luna5ama.trollhack.module.modules.player.NoRotate
import dev.luna5ama.trollhack.module.modules.player.PacketCancel
import dev.luna5ama.trollhack.module.modules.player.PacketLimiter
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.module.modules.player.SwingLimiter
import dev.luna5ama.trollhack.module.modules.player.Timer
import dev.luna5ama.trollhack.module.modules.player.XCarry
import dev.luna5ama.trollhack.module.modules.render.AntiFog
import dev.luna5ama.trollhack.module.modules.render.ExtraTab
import dev.luna5ama.trollhack.module.modules.render.FullBright
import dev.luna5ama.trollhack.module.modules.render.SelectionHighlight

/**
 * Explicit registry of all built in modules.
 * Fabric has no cheap classpath scanning, entries are added here as modules are ported.
 */
internal object BuiltInModules {
    val list: List<AbstractModule> = listOf(
        // Chat
        AntiSpam,
        AutoCope,
        AutoQMain,
        AutoReply,
        ChatSuffix,
        ChatTimestamp,
        ExtraChatHistory,
        FancyChat,
        LoginMessage,
        PortalChat,
        Spammer,

        // Client
        ChatSetting,
        ClickGUI,
        CommandSetting,
        GuiSetting,
        Hud,
        HudEditor,
        Configurations,
        Language,

        // Combat
        AntiBot,
        AutoCity,
        AutoEatPlaceholder,
        BedAura,
        CombatSetting,
        CrystalESP,
        HoleESP,
        KillAura,
        MidClickPearl,
        Surround,
        TotemPopCounter,
        TrollAura,
        VisualRange,

        // Exploit
        AntiAntiBurrow,
        Bypass,
        MountBypass,
        SandPop,
        WallClip,

        // Misc
        dev.luna5ama.trollhack.module.modules.misc.AntiAFK,
        dev.luna5ama.trollhack.module.modules.misc.AntiBookKick,
        dev.luna5ama.trollhack.module.modules.misc.AntiWeather,
        dev.luna5ama.trollhack.module.modules.misc.AutoFish,
        dev.luna5ama.trollhack.module.modules.misc.AutoMine,
        dev.luna5ama.trollhack.module.modules.misc.AutoReconnect,
        dev.luna5ama.trollhack.module.modules.misc.AutoRespawn,
        dev.luna5ama.trollhack.module.modules.misc.AutoTool,
        dev.luna5ama.trollhack.module.modules.misc.BreakWarning,
        dev.luna5ama.trollhack.module.modules.misc.CoordsLog,
        dev.luna5ama.trollhack.module.modules.misc.FakeGameMode,
        dev.luna5ama.trollhack.module.modules.misc.LavaFiller,
        dev.luna5ama.trollhack.module.modules.misc.LogoutLogger,
        dev.luna5ama.trollhack.module.modules.misc.MidClickFriends,
        dev.luna5ama.trollhack.module.modules.misc.NoSoundLag,
        dev.luna5ama.trollhack.module.modules.misc.PingSpoof,
        dev.luna5ama.trollhack.module.modules.misc.SkinBlinker,
        dev.luna5ama.trollhack.module.modules.misc.TeleportLogger,
        dev.luna5ama.trollhack.module.modules.misc.UnfocusedFps,

        // Movement
        AntiHunger,
        AntiLevitation,
        AutoCenter,
        AutoJump,
        AutoRemount,
        Flight,
        InventoryMove,
        ReverseStep,
        SafeWalk,
        Sprint,

        // Player
        AutoEject,
        FastBreak,
        FastUse,
        GhostHand,
        HandSwing,
        HotbarRefill,
        ItemSaver,
        MotionLogger,
        NoFall,
        NoPacketKick,
        NoRotate,
        PacketCancel,
        PacketLimiter,
        PacketMine,
        SwingLimiter,
        Timer,
        XCarry,

        // Render
        AntiFog,
        ExtraTab,
        FullBright,
        SelectionHighlight,
    )
}
