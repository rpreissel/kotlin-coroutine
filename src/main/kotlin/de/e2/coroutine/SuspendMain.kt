package de.e2.coroutine

import kotlinx.coroutines.delay

//fun main() = runBlocking{
//    withContext(Dispatchers.Default) {
//        delay(100)
//        println("Dummy")
//    }
//}
suspend fun main() {
    delay(100)
    println("Dummy")
}