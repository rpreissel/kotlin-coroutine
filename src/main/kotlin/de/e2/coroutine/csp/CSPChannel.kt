@file:UseExperimental(ExperimentalCoroutinesApi::class)
@file:Suppress("PackageDirectoryMismatch")

package de.e2.coroutine.csp.channel

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import java.time.Duration
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.swing.Swing as UI


suspend fun main(): Unit = coroutineScope {
    JerseyClient.use {
        val channel = Channel<BufferedImage>()
        val dogsJob = launch(Dispatchers.Unconfined) {
            retrieveImages("dogs", channel)
        }

        val catsJob = launch(Dispatchers.Unconfined) {
            retrieveImages("cats", channel)
        }

        val collageJob = launch(Dispatchers.Unconfined) {
            createCollage(channel, 4)
        }
        delay(Duration.ofHours(1))

        dogsJob.cancel()
        catsJob.cancel()
        collageJob.cancel()

        Unit
    }
}

suspend fun createCollage(channel: ReceiveChannel<BufferedImage>, count: Int) {
    var imageId = 0
    while (true) {
        val images = (1..count).map {
            channel.receive()
        }
        val collage = combineImages(images)
        ImageIO.write(collage, "png", FileOutputStream("image-${imageId++}.png"))
    }
}

suspend fun retrieveImages(query: String, channel: SendChannel<BufferedImage>) {
    while (true) {
        try {
            val url = requestImageUrl(query)
            val image = requestImageData(url)
            channel.send(image)
            delay(Duration.ofSeconds(2))
        } catch (exc: Exception) {
            delay(Duration.ofSeconds(1))
        }
    }
}

private suspend fun requestImageUrl(query: String) = suspendCoroutine<String> { cont ->
    JerseyClient.pixabay("q=$query&per_page=200")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                val url = urls.shuffled().firstOrNull() ?: return failed(IllegalStateException("No image found"))
                cont.resume(url)
            }

            override fun failed(throwable: Throwable) {
                cont.resumeWithException(throwable)
            }
        })
}

private suspend fun requestImageData(imageUrl: String) = suspendCoroutine<BufferedImage> { cont ->
    JerseyClient.url(imageUrl)
        .request(MediaType.APPLICATION_OCTET_STREAM)
        .async()
        .get(object : InvocationCallback<InputStream> {
            override fun completed(response: InputStream) {
                val image = ImageIO.read(response)
                cont.resume(image)
            }

            override fun failed(throwable: Throwable) {
                cont.resumeWithException(throwable)
            }
        })
}



