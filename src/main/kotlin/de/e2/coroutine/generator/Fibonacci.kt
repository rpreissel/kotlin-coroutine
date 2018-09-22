package de.e2.coroutine.generator


fun fibonacci(): Sequence<Int> = sequence {
    var terms = Pair(0, 1)

    while(true) {
        yield(terms.first)
        terms = Pair(terms.second, terms.first + terms.second)
    }
}

fun main() {
    println(fibonacci().take(10).toList())
}