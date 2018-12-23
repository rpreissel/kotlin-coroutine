package de.e2.coroutine.comparison.coroutinenewer

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.CoroutineContext


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

    val stopSignal = CompletableDeferred<Unit>()
    repeat(HANDLE_COROUTINE_CAPACITY) {
        launchWorker(entryQueue, stopSignal)
    }
}

private fun CoroutineScope.launchWorker(
    entryQueue: ReceiveChannel<Payload>,
    stopSignal: CompletableDeferred<Unit>,
    dispatcher: CoroutineContext = Dispatchers.Default
) = launch(dispatcher) {
    while (true) {
        val stop = select<Boolean> {
            entryQueue.onReceive { payload ->
                when (payload) {
                    is WorkItem -> {
                        payload.startWorking()
                        handleWorkItem(payload)
                    }

                    StopToken -> {
                        stopSignal.complete(Unit)
                    }
                }
                false
            }
            stopSignal.onAwait {
                true
            }
        }
        if (stop) break
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