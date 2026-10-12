package dev.fastmc.common.collection

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

/**
 * Minimal stand-ins for the fastmc-common collection types used by TrollHack.
 */
class FastIntMap<V> {
    private val delegate = Int2ObjectOpenHashMap<V>()

    val size: Int get() = delegate.size
    val values: MutableCollection<V> get() = delegate.values

    operator fun get(key: Int): V? {
        return delegate.get(key)
    }

    operator fun set(key: Int, value: V) {
        delegate.put(key, value)
    }

    fun put(key: Int, value: V): V? {
        return delegate.put(key, value)
    }

    fun remove(key: Int): V? {
        return delegate.remove(key)
    }

    fun clear() {
        delegate.clear()
    }

    fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    inline fun getOrPut(key: Int, block: () -> V): V {
        return get(key) ?: block().also { set(key, it) }
    }
}

typealias FastObjectArrayList<E> = ArrayList<E>

class DynamicBitSet {
    private val bitSet = java.util.BitSet()

    fun add(index: Int) {
        bitSet.set(index)
    }

    fun remove(index: Int) {
        bitSet.clear(index)
    }

    operator fun contains(index: Int): Boolean {
        return bitSet.get(index)
    }

    operator fun iterator(): IntIterator {
        return object : IntIterator() {
            private var next = bitSet.nextSetBit(0)

            override fun hasNext(): Boolean {
                return next != -1
            }

            override fun nextInt(): Int {
                val result = next
                next = bitSet.nextSetBit(next + 1)
                return result
            }
        }
    }
}
