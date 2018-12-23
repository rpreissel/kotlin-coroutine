@file:Suppress("PackageDirectoryMismatch")
@file:UseExperimental(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package de.e2.coroutine.csp.actor

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
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

sealed class PixabayMsg
data class RequestImageMsg(
    val query: String
) : PixabayMsg()


fun CoroutineScope.pixabayActor(resultChannel: SendChannel<BufferedImage>, parallel: Int) = actor<PixabayMsg> {
    val urlChannel = Channel<String>()
    repeat(parallel) {
        launch {
            for (url in urlChannel) {
                val image = requestImageData(url)
                resultChannel.send(image)
            }
        }
    }

    for (msg in channel) {
        when (msg) {
            is RequestImageMsg -> msg.apply {
                urlChannel.send(requestImageUrl(query))
            }
        }
        delay(Duration.ofMillis(100))
    }
}


suspend fun main(): Unit = coroutineScope {
    JerseyClient.use {
        val resultChannel = Channel<BufferedImage>()
        val pixabayActor = pixabayActor(resultChannel, 2)
        val dogsJob = launch(Dispatchers.Unconfined) {
            retrieveImages("dogs", pixabayActor)
        }

        val catsJob = launch(Dispatchers.Unconfined) {
            retrieveImages("cats", pixabayActor)
        }

        val collageJob = launch(Dispatchers.Unconfined) {
            createCollage(resultChannel, 4)
        }
        delay(Duration.ofHours(1))

        dogsJob.cancel()
        catsJob.cancel()
        collageJob.cancel()
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

suspend fun retrieveImages(
    query: String,
    pixabayActor: SendChannel<PixabayMsg>
) {
    val requestImageUrlMsg = RequestImageMsg(query)
    while (true) {
        pixabayActor.send(requestImageUrlMsg)
        delay(Duration.ofSeconds(2))
    }
}

private suspend fun requestImageUrl(query: String) = suspendCoroutine<String> { cont ->
    JerseyClient.pixabay("q=$query&per_page=20")
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



