package de.e2.coroutine.collage.future

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) = JerseyClient.use {
    createCollage("dogs", 20).thenAccept { collage ->
        ImageIO.write(collage, "png", FileOutputStream("dogs.png"))
    }

    Thread.sleep(5000)
}

fun createCollage(query: String, count: Int): CompletableFuture<BufferedImage> {
    return requestImageUrls(query, count)
        .thenCompose {urls ->
            val startFuture = completedFuture<List<BufferedImage>>(listOf())
            urls.fold(startFuture) { lastFuture, url ->
                lastFuture.thenCompose { images ->
                    requestImageData(url).thenApply { image ->
                        images + image
                    }
                }
            }
        }.thenApply(::combineImages)
}

private fun requestImageUrls(query: String, count: Int) = CompletableFuture<List<String>>().also { future ->
    val json = JerseyClient.pixabay("q=$query&per_page=$count")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                future.complete(urls)
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


