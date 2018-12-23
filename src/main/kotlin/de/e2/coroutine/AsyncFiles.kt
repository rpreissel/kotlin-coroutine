@file:UseExperimental(ExperimentalCoroutinesApi::class)

package de.e2.coroutine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.springframework.util.FileCopyUtils
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.StandardOpenOption
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun AsynchronousFileChannel.readBuffer(
    dst: ByteBuffer,
    position: Long
): Int = suspendCoroutine { cont ->
    read(dst, position, Unit, object : CompletionHandler<Int, Unit> {
        override fun completed(result: Int, attachment: Unit) {
            cont.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Unit) {
            cont.resumeWithException(exc)
        }
    })
}

suspend fun File.readAsync(): ReceiveChannel<ByteArray> = coroutineScope {
    produce {
        AsynchronousFileChannel.open(
            toPath(),
            StandardOpenOption.READ
        ).use { channel ->
            val buffer = ByteBuffer.allocate(FileCopyUtils.BUFFER_SIZE)
            var pos = 0L
            while (isActive) {
                buffer.clear()

                val bytesRead = channel.readBuffer(buffer, pos)
                if (bytesRead <= 0) {
                    break
                }
                pos += bytesRead

                val data = with(buffer) {
                    flip()
                    ByteArray(limit()).also { get(it) }
                }

                send(data)
            }
        }
    }
}

fun main() = runBlocking {
    val channel = AsynchronousFileChannel.open(
        File("/Users/rene/workspaces/kotlin/kotlin-coroutine-kt/src/main/kotlin/de/e2/coroutine/AsyncFiles.kt").toPath(),
        StandardOpenOption.READ
    )

    channel.use {
        val buffer = ByteBuffer.allocate(FileCopyUtils.BUFFER_SIZE)
        var pos = 0L
        while (true) {
            val bytesRead = channel.readBuffer(buffer, pos)
            if (bytesRead <= 0) {
                return@use
            }
            pos += bytesRead

            buffer.clear()
        }
    }

    val result =
        File("/Users/rene/workspaces/kotlin/kotlin-coroutine-kt/src/main/kotlin/de/e2/coroutine/AsyncFiles.kt").readAsync()
    for (d in result) {
        println(d.size)
    }
    println("End")
}