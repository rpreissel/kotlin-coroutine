import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

suspend fun dummy() {
    CoroutineScope(coroutineContext)
}
//
//fun main(args: Array<String>) = runBlocking {
//    val ss = GlobalScope.produce {
//        repeat(100) { i ->
//            println(i)
//            send(i)
//        }
//    }
//    println(ss.take(3).toList())
//    delay(100)
//    println(ss.take(3).toList())
//}

    fun main() = runBlocking {
        go()
        go()
        go()
        println("End")
    }

    fun CoroutineScope.go() = launch {
        println("go!")
    }
