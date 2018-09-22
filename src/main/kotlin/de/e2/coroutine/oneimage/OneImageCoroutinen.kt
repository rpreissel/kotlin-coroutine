package de.e2.coroutine.oneimage.coroutine

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.swing.Swing as UI

val DEFAULT_IMAGE: BufferedImage = BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)

fun main(args: Array<String>): Unit = runBlocking {
    JerseyClient.use {
        val image = loadOneImage("dogs")
        println("${image.width}x${image.height}")

        val fastImage = loadFastestImage("dogs", 20)
        println("${fastImage.width}x${fastImage.height}")


//        val fastImageOrNull: BufferedImage? = withTimeoutOrNull(200) {
//            loadFastestImage("dogs", 20)
//        }
//        fastImageOrNull?.let {
//            println("${it.width}x${it.height}")
//        }
//        Unit
    }
}

suspend fun loadOneImage(query: String): BufferedImage {
    val url = requestImageUrl(query)
    val image = requestImageData(url)
    return image
}


suspend fun loadFastestImage(query: String, count: Int): BufferedImage = coroutineScope {
    val urls = requestImageUrls(query, count)
    val deferredImages = urls.map {
        async { requestImageData(it) }
    }
    val image: BufferedImage = select {
        for (deferredImage in deferredImages) {
            deferredImage.onAwait { image ->
                image
            }
        }
    }
    deferredImages.forEach { it.cancel() }
    image
}

suspend fun loadFastestImage(query: String, count: Int, timeoutMs: Long): BufferedImage = coroutineScope {
    val urls = requestImageUrls(query, count)
    val deferredImages = urls.map {
        async { requestImageData(it) }
    }
    val image: BufferedImage = select {
        for (deferredImage in deferredImages) {
            deferredImage.onAwait { image ->
                image
            }
        }

        onTimeout(timeoutMs) {
            DEFAULT_IMAGE
        }
    }

    deferredImages.forEach { it.cancel() }
    image
}


private suspend fun requestImageUrls(query: String, count: Int = 20) = suspendCoroutine<List<String>> { cont ->
    JerseyClient.pixabay("q=$query&per_page=$count")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                cont.resume(urls)
            }

            override fun failed(throwable: Throwable) {
                cont.resumeWithException(throwable)
            }
        })
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


