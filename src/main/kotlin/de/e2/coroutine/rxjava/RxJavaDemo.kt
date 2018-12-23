package de.e2.coroutine.rxjava

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {

    val windowManager = object {
        lateinit var emitter: FlowableEmitter<Unit>
        var windowSize: Long = 0

        fun createEmitter(emitter: FlowableEmitter<Unit>) {
            this.emitter = emitter
        }

        fun openWindowIfRequired(size: Long) {
            windowSize += size
            if (windowSize > 5) {
                windowSize = 0
                emitter.onNext(Unit)
            }
        }
    }

    val windowBoundary = Flowable.create<Unit>(windowManager::createEmitter, BackpressureStrategy.ERROR)

    Flowable.interval(1, TimeUnit.SECONDS)
        .doOnNext {
            windowManager.openWindowIfRequired(it)
        }.window(windowBoundary).subscribe {
            it.doOnSubscribe {
                println("Open window")
            }.doOnComplete {
                println("Close window")
            }.subscribe {
                println((it))
            }
        }

    Thread.sleep(5000)
}