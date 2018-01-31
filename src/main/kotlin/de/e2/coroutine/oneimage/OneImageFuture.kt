package de.e2.coroutine.oneimage.future

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) = JerseyClient.use {
    loadOneImage("dogs").thenAccept { image ->
        println("${image.width}x${image.height}")
    }

    Thread.sleep(5000)
}

fun loadOneImage(query: String): CompletableFuture<BufferedImage> {
    return requestImageUrl(query)
        .thenCompose(::requestImageData)
}

private fun requestImageUrl(query: String) = CompletableFuture<String>().also { future ->
    val json = JerseyClient.pixabay("q=$query")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                val url = urls.firstOrNull() ?: return failed(IllegalStateException("No image found"))
                future.complete(url)
            }

            override fun failed(throwable: Throwable) {
                future.completeExceptionally(throwable)
            }
        })
}

private fun requestImageData(imageUrl: String) = CompletableFuture<BufferedImage>().also { future ->
    JerseyClient.url(imageUrl)
        .request(MediaType.APPLICATION_OCTET_STREAM)
        .async()
        .get(object : InvocationCallback<InputStream> {
            override fun completed(response: InputStream) {
                val image = ImageIO.read(response)
                future.complete(image)
            }

            override fun failed(throwable: Throwable) {
                future.completeExceptionally(throwable)
            }
        })
}


