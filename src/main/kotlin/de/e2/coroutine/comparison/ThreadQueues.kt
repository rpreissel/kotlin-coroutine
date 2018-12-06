package de.e2.coroutine.comparison.thread

import de.e2.coroutine.comparison.ENTRY_QUEUE_CAPACITY
import de.e2.coroutine.comparison.HANDLE_THREAD_CAPACITY
import de.e2.coroutine.comparison.Payload
import de.e2.coroutine.comparison.StopToken
import de.e2.coroutine.comparison.TEST_COUNT
import de.e2.coroutine.comparison.WorkItem
import de.e2.coroutine.comparison.reportToConsole
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


fun doBlockingWork() {
    val workTime = ThreadLocalRandom.current().nextLong(10, 50)
    Thread.sleep(workTime)
}

fun handleWorkItem(workItem: WorkItem) {
    repeat(5) {
        doBlockingWork()
    }
    workItem.finishWorking()
}

fun handlePayload(entryQueue: BlockingQueue<Payload>) {
    val executor = Executors.newFixedThreadPool(HANDLE_THREAD_CAPACITY)

    loop@ while (true) {
        val payload = entryQueue.take()
        when (payload) {
            is WorkItem -> {
                payload.startWorking()
                executor.submit() {
                    handleWorkItem(payload)
                }
            }
            StopToken -> break@loop
        }
    }

    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.HOURS)
}


fun main(args: Array<String>) = reportToConsole {

    val entryQueue = ArrayBlockingQueue<Payload>(ENTRY_QUEUE_CAPACITY)

    thread {
        repeat(TEST_COUNT) {
            entryQueue.put(WorkItem(it))
        }

        entryQueue.put(StopToken)
    }

    handlePayload(entryQueue)
}