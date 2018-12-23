@file:UseExperimental(ExperimentalCoroutinesApi::class)
@file:Suppress("PackageDirectoryMismatch")

package de.e2.coroutine.csp.producer

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.selectUnbiased
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


suspend fun main() = coroutineScope {
    JerseyClient.use {
        val dogsChannel = retrieveImages("dogs")
        val catsChannel = retrieveImages("cats")

        val collageJob = launch(Dispatchers.Unconfined) {
            var imageId = 0
            while (isActive) {
                val collage = createCollage(4, catsChannel, dogsChannel)
                ImageIO.write(collage, "png", FileOutputStream("image-${imageId++}.png"))
            }
        }
        delay(Duration.ofHours(1))

        dogsChannel.cancel()
        catsChannel.cancel()
        collageJob.cancel()
    }
}

suspend fun createCollage(
    count: Int,
    vararg channels: ReceiveChannel<BufferedImage>
): BufferedImage {
    val images = (1..count).map {
        selectUnbiased<BufferedImage> {
            channels.forEach { channel ->
                channel.onReceive { image -> image }
            }
        }
    }
    return combineImages(images)
}

fun CoroutineScope.retrieveImages(
    query: String
): ReceiveChannel<BufferedImage> =
    produce {
        while (isActive) {
            try {
                val url = requestImageUrl(query)
                val image = requestImageData(url)
                send(image)
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



