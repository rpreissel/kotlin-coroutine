package de.e2.coroutine

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val urls = runBlocking {
        dummy()
    }
}

suspend fun dummy() {

}