package dev.luna5ama.trollhack.util

import java.io.InputStream
import java.net.URL

fun InputStream.readText(): String {
    return this.readBytes().decodeToString()
}

fun URL.readText(): String {
    return openStream().use { it.readText() }
}
