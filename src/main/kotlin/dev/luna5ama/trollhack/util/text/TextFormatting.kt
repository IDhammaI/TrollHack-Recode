package dev.luna5ama.trollhack.util.text

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

fun formatValue(value: String) = ChatFormatting.GRAY format "[$value]"

fun formatValue(value: Char) = ChatFormatting.GRAY format "[$value]"

fun formatValue(value: Any) = ChatFormatting.GRAY format "[$value]"

fun formatValue(value: Int) = ChatFormatting.GRAY format "($value)"

infix fun ChatFormatting.format(value: Any) = "$this$value${ChatFormatting.RESET}"

infix fun ChatFormatting.format(value: Int) = "$this$value${ChatFormatting.RESET}"

infix fun EnumTextColor.format(value: Any) = "$this$value${ChatFormatting.RESET}"

val Component.unformatted get() = ChatFormatting.stripFormatting(this.string)!!
