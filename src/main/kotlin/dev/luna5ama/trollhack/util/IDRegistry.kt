package dev.luna5ama.trollhack.util

import java.util.BitSet

class IDRegistry {
    private val bitSet = BitSet()

    fun register(): Int {
        synchronized(bitSet) {
            val id = bitSet.nextClearBit(0)
            bitSet.set(id)
            return id
        }
    }

    fun unregister(id: Int) {
        synchronized(bitSet) {
            bitSet.clear(id)
        }
    }
}
