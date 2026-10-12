package dev.fastmc.common.collection

class CircularArray(private val size: Int, initial: Float) {
    private val array = FloatArray(size) { initial }
    private var index = 0
    private var count = size

    init {
        require(size > 0)
    }

    fun add(value: Float) {
        array[index] = value
        index = (index + 1) % size
        if (count < size) count++
    }

    fun clear() {
        array.fill(0.0f)
        index = 0
        count = 0
    }

    companion object {
        fun CircularArray.average(): Float {
            if (count == 0) return 20.0f
            var sum = 0.0f
            for (i in 0 until count) {
                sum += array[i]
            }
            return sum / count
        }
    }
}
