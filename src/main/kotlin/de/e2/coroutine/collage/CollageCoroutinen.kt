@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_VARIABLE")
@file:UseExperimental(ExperimentalCoroutinesApi::class)
package de.e2.coroutine.collage.coroutine

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.Timer
import de.e2.coroutine.combineImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.swing.Swing as UI


fun main(): Unit = runBlocking {
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

fun CoroutineScope.builderExamples() {
    //Startet die Koroutine und "blockiert" den aktuellen Thread
    val collage1 = runBlocking {
        createCollage("dogs", 20)
    }

    //Startet die Koroutine und setzt den aktuellen Thread fort
    val job = launch {
        val collage2 = createCollage("dogs", 20)
        ImageIO.write(collage2, "png", FileOutputStream("dogs.png"))
    }

    //Stoppt die Koroutine
    job.cancel()
}


fun CoroutineScope.builderExamplesWithContext() {
    //Den Fork-Join-Pool für die Koroutine nutzen
    val collage1 = runBlocking(Dispatchers.Default) {
        createCollage("dogs", 20)
    }

    //Einen eigenen Thread-Pool für die Koroutine nutzen
    val fixedThreadPoolContext = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    val job = launch(fixedThreadPoolContext) {
        val collage2 = createCollage("dogs", 20)

        // Wechsel in den UI-Thread und zurück
        withContext(Dispatchers.UI) {
            ImageIO.write(collage2, "png", FileOutputStream("dogs.png"))
        }
    }
}

suspend fun createCollage(
    query: String, count: Int
): BufferedImage {
    val urls = requestImageUrls(query, count)
    val images = urls.map { requestImageData(it) }
    return combineImages(images)
}

suspend fun createCollageForLoop(query: String, count: Int): BufferedImage {
    val urls = requestImageUrls(query, count) //Label 0
    val images = mutableListOf<BufferedImage>() //Label 1
    for (index in 0 until urls.size) {
        val image = requestImageData(urls[index])
        images += image //Label 2
    }
    return combineImages(images)
}

private suspend fun createCollageAsyncAwait(
    query: String, count: Int
): BufferedImage = coroutineScope {
    val urls = requestImageUrls(query, count)
    val deferredImages: List<Deferred<BufferedImage>> = urls.map {
        async {
            requestImageData(it)
        }
    }

    val images: List<BufferedImage> = deferredImages.awaitAll()

    combineImages(images)
}


suspend fun loadFastImages(urls: List<String>, timeoutMs: Long): List<BufferedImage> = coroutineScope {
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
    imagesToRetrieve.forEach { it.cancel() }
    result
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



