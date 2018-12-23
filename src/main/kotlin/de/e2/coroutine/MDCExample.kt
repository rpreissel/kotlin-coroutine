package de.e2.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

const val MDC_KEY = "abc"

fun printKey() {
    println("${Thread.currentThread().name} - ${MDC.get(MDC_KEY)}")
}

class MDCContextElement : ThreadContextElement<Map<String, String>?>,
    AbstractCoroutineContextElement(MDCContextElement) {
    companion object Key : CoroutineContext.Key<MDCContextElement>

    private val value: Map<String, String>? = MDC.getCopyOfContextMap()

    override fun updateThreadContext(context: CoroutineContext): Map<String, String>? {
        val old: Map<String, String>? = MDC.getCopyOfContextMap()
        if (value == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(value)
        }

        return old
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Map<String, String>?) {
        if (oldState == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(oldState)
        }
    }
}

fun CoroutineDispatcher.withMDC(): CoroutineDispatcher =
    object : CoroutineDispatcher()/*, CoroutineContext by this@withMDC*/ {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            this@withMDC.dispatch(context + MDCContextElement(), block)
        }

//        override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
//            return this@withMDC.get(key)
//        }
//
        override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
            return this@withMDC.fold(initial, operation)
        }
//
//        override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
//            return this@withMDC.minusKey(key)
//        }
//
//        @ExperimentalCoroutinesApi
//        override fun isDispatchNeeded(context: CoroutineContext): Boolean {
//            return this@withMDC.isDispatchNeeded(context)
//        }
//
//        override fun plus(context: CoroutineContext): CoroutineContext {
//            return this@withMDC.plus(context)
//        }
//
//        override fun releaseInterceptedContinuation(continuation: Continuation<*>) {
//            this@withMDC.releaseInterceptedContinuation(continuation)
//        }
    }


fun CoroutineScope.launchWithMDC(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + MDCContextElement(), start, block)

suspend fun <T> withContextAndMDC(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T = withContext(context + MDCContextElement(), block)

fun main() = runBlocking {

    MDC.put(MDC_KEY, "def")
    printKey()

    withContext(Dispatchers.Default.withMDC()) {
//    withContextAndMDC(Dispatchers.Default) {
        printKey()
    }
}