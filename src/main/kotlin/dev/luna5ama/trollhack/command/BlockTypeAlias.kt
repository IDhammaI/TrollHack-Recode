package dev.luna5ama.trollhack.command

import dev.luna5ama.trollhack.command.args.AbstractArg
import dev.luna5ama.trollhack.command.args.ArgIdentifier

/**
 * Block to run on invoking a command
 */
typealias ExecuteBlock<E> = suspend E.() -> Unit

/**
 * Block to build up arguments
 */
typealias BuilderBlock<T> = AbstractArg<T>.(ArgIdentifier<T>) -> Unit
