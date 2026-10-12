package dev.luna5ama.trollhack.util

object ClassUtils {
    @Suppress("UNCHECKED_CAST")
    val <T> Class<out T>.instance
        get() = this.getDeclaredField("INSTANCE")[null] as T
}