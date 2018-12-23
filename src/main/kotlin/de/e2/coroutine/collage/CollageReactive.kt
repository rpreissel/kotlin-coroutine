@file:UseExperimental(ExperimentalCoroutinesApi::class)
@file:Suppress("PackageDirectoryMismatch")

package de.e2.coroutine.collage.reactive

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.ReactorClient
import de.e2.coroutine.combineImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.swing.Swing as UI


fun main(): Unit = runBlocking {
    ReactorClient.use {
        val collage = createCollage("dogs", 20)
        ImageIO.write(collage, "png", FileOutputStream("dogs.png"))
    }
    Unit
}

suspend fun createCollage(
    query: String, count: Int
): BufferedImage {
    val urls = requestImageUrls(query, count)
    val images = urls.map { requestImageData(it) }
    return combineImages(images)
}

fun CoroutineScope.createCollageAsMono(
    query: String, count: Int
): Mono<BufferedImage> = mono {
    val urls = requestImageUrls(query, count)
    val images = urls.map { requestImageData(it) }
    combineImages(images)
}

fun CoroutineScope.retrieveImagesAsFlux(
    query: String,
    batchSize: Int
): Flux<BufferedImage> = flux {
    while (isActive) {
        val urls = requestImageUrls(query, batchSize)
        for (url in urls) {
            val image = requestImageData(url)
            send(image)
        }
        delay(2000)
    }
}

suspend fun requestImageUrls(query: String, count: Int = 20): List<String> {
    return ReactorClient
        .pixabay("q=$query&per_page=200")
        .retrieve()
        .bodyToMono<String>()
        .map { response ->
            JsonPath.read<List<String>>(response, "$..previewURL")
        }.map { result ->
            result.shuffled().take(count)
        }
        .onErrorReturn(listOf())
        .awaitSingle()
}

suspend fun requestImageData(imageUrl: String): BufferedImage {
    return ReactorClient
        .url(imageUrl)
        .accept(APPLICATION_OCTET_STREAM)
        .retrieve()
        .bodyToMono(ByteArrayResource::class.java)
        .map { byteArray ->
            ImageIO.read(byteArray.inputStream)
        }.awaitSingle()
}



