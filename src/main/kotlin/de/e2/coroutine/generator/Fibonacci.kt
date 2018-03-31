package de.e2.coroutine.generator

import kotlin.coroutines.experimental.buildSequence

fun fibonacci(): Sequence<Int> = buildSequence {
    var terms = Pair(0, 1)

    while(true) {
        yield(terms.first)
        terms = Pair(terms.second, terms.first + terms.second)
    }
}

fun main(args: Array<String>) {
    println(fibonacci().take(10).toList())
}