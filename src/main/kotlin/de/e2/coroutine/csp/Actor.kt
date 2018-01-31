package de.e2.coroutine.csp.actor

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.runBlocking
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType
import kotlin.coroutines.experimental.suspendCoroutine
import kotlinx.coroutines.experimental.swing.Swing as UI


fun main(args: Array<String>): Unit = runBlocking {
    JerseyClient.use {
        val channel = Channel<BufferedImage>()
        launch(Unconfined) {
            retrieveImages("dogs", channel)
        }

        launch(Unconfined) {
            retrieveImages("cats", channel)
        }

        launch(Unconfined) {
            createCollage(channel, 4)
        }
        delay(1, TimeUnit.HOURS)
    }
}

sealed class PixabayMsg
data class RequestImageUrlMsg(
    val query: String,
    val resultChannel: SendChannel<String>
) : PixabayMsg()


val PixabayActor: SendChannel<PixabayMsg> = actor<PixabayMsg> {
    for (msg in channel) {
        when (msg) {
            is RequestImageUrlMsg -> msg.apply {
                resultChannel.send(requestImageUrl(query))
            }
        }
        delay(100)
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
    val resultChannel = Channel<String>(1)
    val requestImageUrlMsg = RequestImageUrlMsg(query, resultChannel)
    while (true) {
        PixabayActor.send(requestImageUrlMsg)
        val url = resultChannel.receive()
        val image = requestImageData(url)
        channel.send(image)
        delay(2, TimeUnit.SECONDS)
    }
}

private suspend fun requestImageUrl(query: String) = suspendCoroutine<String> { cont ->
    JerseyClient.pixabay("q=$query")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                val url = urls.firstOrNull() ?: return failed(IllegalStateException("No image found"))
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



