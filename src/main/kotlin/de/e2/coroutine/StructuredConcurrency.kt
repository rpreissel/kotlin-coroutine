package de.e2.coroutine

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = runBlocking {
    val job = launch {

        val innerJob = Job(coroutineContext[Job])
        for (i in 1..100) {
            async(innerJob) {
                println("Start $i")
                try {
                    delay(1, TimeUnit.HOURS)
                } catch (e: Exception) {
                    println("${e::class} ${e.message}")
                } finally {
                    println("End $i")
                }
            }
        }
    }

    delay(2, TimeUnit.SECONDS)

    job.cancelChildren()
}