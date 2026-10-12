package dev.fastmc.common

class DoubleBuffered<T>(initializer: () -> T) {
    var front = initializer()
        private set
    var back = initializer()
        private set

    fun swap(): DoubleBuffered<T> {
        val temp = front
        front = back
        back = temp
        return this
    }
}
