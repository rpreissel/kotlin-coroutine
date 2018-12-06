package de.e2.coroutine.comparison.coroutine

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import de.e2.coroutine.comparison.ENTRY_QUEUE_CAPACITY
import de.e2.coroutine.comparison.HANDLE_COROUTINE_CAPACITY
import de.e2.coroutine.comparison.Payload
import de.e2.coroutine.comparison.StopToken
import de.e2.coroutine.comparison.TEST_COUNT
import de.e2.coroutine.comparison.WorkItem
import de.e2.coroutine.comparison.metrics
import de.e2.coroutine.comparison.reportToConsole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ThreadLocalRandom


suspend fun doBlockingWork() {
    val workTime = ThreadLocalRandom.current().nextInt(10, 50)
    delay(workTime.toLong())
}

suspend fun handleWorkItem(workItem: WorkItem) {
    repeat(5) {
        doBlockingWork()
    }
    workItem.finishWorking()
}

suspend fun handlePayload(entryQueue: ReceiveChannel<Payload>) = coroutineScope {
    val workItemsParentJob = coroutineContext[Job] ?: throw IllegalStateException("Job not found")

    metrics.gauge(MetricRegistry.name("coroutine", "serveWorkItem")) {
        Gauge {
            workItemsParentJob.children.count()
        }
    }

    var coroutinesToStart = HANDLE_COROUTINE_CAPACITY

    loop@ while (isActive) {
        while (coroutinesToStart-- > 0) {
            val payload = entryQueue.receive()
            when (payload) {
                is WorkItem -> {
                    payload.startWorking()
                    launch(Dispatchers.Default) {
                        handleWorkItem(payload)
                    }
                }

                StopToken -> break@loop
            }
        }

        while (true) {
            coroutinesToStart = HANDLE_COROUTINE_CAPACITY - workItemsParentJob.children.count()
            if (coroutinesToStart > 0) {
                break
            }

            delay(10)
        }
    }
}


fun main() = reportToConsole {
    runBlocking {

        val entryQueue = Channel<Payload>(ENTRY_QUEUE_CAPACITY)

        launch(Dispatchers.Default) {
            repeat(TEST_COUNT) {
                entryQueue.send(WorkItem(it))
            }

            entryQueue.send(StopToken)
        }

        handlePayload(entryQueue)
    }
}