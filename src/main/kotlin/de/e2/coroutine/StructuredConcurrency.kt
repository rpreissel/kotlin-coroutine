package de.e2.coroutine

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit = runBlocking {
    //    val job = launch {
//
//        val innerJob = Job(coroutineContext[Job])
//        for (i in 1..100) {
//            async(innerJob) {
//                println("Start $i")
//                try {
//                    delay(10000)
//                } catch (e: Exception) {
//                    println("${e::class} ${e.message}")
//                } finally {
//                    println("End $i")
//                }
//            }
//        }
//    }
//
//    delay(2000)
//
//    job.cancelChildren()
//
//
//    val deferredArray: Array<Deferred<Unit>>  =  arrayOf()
//    val awaitAllArray = awaitAll(*deferredArray)
//
//    val deferredList: List<Deferred<Unit>>  =  listOf()
//    val awaitAllList = deferredList.awaitAll()
//
//
//    Unit

//    val result1 = coroutineScope {
//        async {
//            throw IllegalStateException()
//        }
//        10
//    }

//    val result2 = supervisorScope {
//        async {
//            throw IllegalStateException()
//        }
//        10
//    }

//
//    println(result)
}

