package de.e2.coroutine.comparison

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SlidingTimeWindowArrayReservoir
import com.codahale.metrics.Timer
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

val metrics = MetricRegistry().apply {
    val memoryMXBean = ManagementFactory.getMemoryMXBean()
    val threadMXBean = ManagementFactory.getThreadMXBean()
    gauge(MetricRegistry.name("memory", "used")) {
        Gauge {
            memoryMXBean.heapMemoryUsage.used
        }
    }
    gauge(MetricRegistry.name("threads", "used")) {
        Gauge {
            threadMXBean.threadCount
        }
    }
}
val enqueueTimer = metrics.timer("enqueue")
val workingTimer = metrics.timer("working") {
    Timer(SlidingTimeWindowArrayReservoir(1, TimeUnit.MINUTES))
}


inline fun reportToConsole(crossinline block: () -> Unit) {
    val reporter = ConsoleReporter.forRegistry(metrics)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()

    reporter.start(5, TimeUnit.SECONDS)
    metrics.timer("startEnd").time {
        block()
    }
    reporter.report()
}

sealed class Payload
class WorkItem(val id: Int) : Payload() {
    val enqueueTimerContext = enqueueTimer.time()
    var workingTimerContext: Timer.Context? = null

    fun startWorking() {
        workingTimerContext = workingTimer.time()
    }

    fun finishWorking() {
        enqueueTimerContext.stop()
        workingTimerContext?.stop()
    }
}

object StopToken : Payload()

const val TEST_COUNT = 1000000
const val ENTRY_QUEUE_CAPACITY = 10000
const val HANDLE_COROUTINE_CAPACITY = 100000
const val HANDLE_THREAD_CAPACITY = 1000

