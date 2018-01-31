package de.e2.coroutine.collage.coroutine

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.Timer
import de.e2.coroutine.combineImages
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.withContext
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType
import kotlin.coroutines.experimental.suspendCoroutine
import kotlinx.coroutines.experimental.swing.Swing as UI


fun main(args: Array<String>): Unit = runBlocking {
    JerseyClient.use {
        val collageTurtle = createCollage("turtle", 20)
        ImageIO.write(collageTurtle, "png", FileOutputStream("turtle.png"))
        val collageDogs = createCollage("dogs", 20)
        ImageIO.write(collageDogs, "png", FileOutputStream("dogs.png"))
        val collageLions = createCollage("lion", 20)
        ImageIO.write(collageLions, "png", FileOutputStream("lions.png"))
    }
    Unit
}

fun builderExamples() {
    //Startet die Koroutine und "blockiert" den aktuellen Thread
    val collage = runBlocking {
        createCollage("dogs", 20)
    }

    //Startet die Koroutine und setzt den aktuellen Thread fort
    val job = launch {
        val collage = createCollage("dogs", 20)
        ImageIO.write(collage, "png", FileOutputStream("dogs.png"))
    }

    //Stoppt die Koroutine
    job.cancel()
}


fun builderExamplesWithContext() {
    //Den Fork-Join-Pool für die Koroutine nutzen
    val collage = runBlocking(CommonPool) {
        createCollage("dogs", 20)
    }

    //Einen eigenen Thread-Pool für die Koroutine nutzen
    val fixedThreadPoolContext = newFixedThreadPoolContext(1, "collage")
    val job = launch(fixedThreadPoolContext) {
        val collage = createCollage("dogs", 20)

        // Wechsel in den UI-Thread und zurück
        withContext(UI) {
            ImageIO.write(collage, "png", FileOutputStream("dogs.png"))
        }
    }
}

suspend fun createCollage(
    query: String, count: Int
): BufferedImage {
    val urls = requestImageUrls(query, count)
    val images = urls.map { requestImageData(it) }
    val newImage = combineImages(images)
    return newImage
}

suspend fun createCollageForLoop(query: String, count: Int): BufferedImage {
    val urls = requestImageUrls(query, count) //Label 0
    val images = mutableListOf<BufferedImage>() //Label 1
    for (index in 0 until urls.size) {
        val image = requestImageData(urls[index])
        images += image //Label 2
    }
    val newImage = combineImages(images)
    return newImage
}

private suspend fun createCollageAsyncAwait(
    query: String, count: Int
): BufferedImage {
    val urls = requestImageUrls(query, count)
    val deferredImages: List<Deferred<BufferedImage>> = urls.map {
        async {
            requestImageData(it)
        }
    }

    val images: List<BufferedImage> = deferredImages.map { it.await() }

    val newImage = combineImages(images)
    return newImage
}

suspend fun loadFastImages(urls: List<String>, timeoutMs: Long): List<BufferedImage> {
    val timer = Timer(timeoutMs)
    val result = mutableListOf<BufferedImage>()
    val deferredImages = urls.map {
        async { requestImageData(it) }
    }

    val imagesToRetrieve = deferredImages.toMutableSet()
    while (timer.isRunning() && imagesToRetrieve.isNotEmpty()) {
        select<Unit> {
            for (deferredImage in imagesToRetrieve) {
                deferredImage.onAwait { image ->
                    imagesToRetrieve -= deferredImage
                    result += image
                }
            }

            onTimeout(timer.timeToGo()) { }
        }

    }

    return result
}

private suspend fun requestImageUrls(
    query: String,
    count: Int = 20
) = suspendCoroutine<List<String>> { continuation ->
    JerseyClient.pixabay("q=$query&per_page=$count").request().async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                continuation.resume(urls)
            }

            override fun failed(throwable: Throwable) {
                continuation.resumeWithException(throwable)
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



