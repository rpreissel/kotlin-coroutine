package de.e2.coroutine.collage.reactive

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.ReactorClient
import de.e2.coroutine.combineImages
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactive.awaitSingle
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.reactor.mono
import kotlinx.coroutines.experimental.runBlocking
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlinx.coroutines.experimental.swing.Swing as UI


fun main(args: Array<String>): Unit = runBlocking {
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
    val newImage = combineImages(images)
    return newImage
}

fun createCollageAsMono(
    query: String, count: Int
): Mono<BufferedImage> = mono {
    val urls = requestImageUrls(query, count)
    val images = urls.map { requestImageData(it) }
    val newImage = combineImages(images)
    newImage
}

fun retrieveImagesAsFlux(
    query: String,
    batchSize: Int
): Flux<BufferedImage> = flux {
    while (isActive) {
        val urls = requestImageUrls(query, batchSize)
        for (url in urls) {
            val image = requestImageData(url)
            send(image)
        }
        delay(2, TimeUnit.SECONDS)
    }
}

private suspend fun requestImageUrls(query: String, count: Int = 20): List<String> {
    return ReactorClient
        .pixabay("q=$query&per_page=$count")
        .exchange()
        .flatMap { clientResponse ->
            when(clientResponse.statusCode()) {
                HttpStatus.OK -> clientResponse.bodyToMono<String>().map { response->
                    JsonPath.read<List<String>>(response, "$..previewURL")
                }
                else -> listOf<String>().toMono()
            }
        }.awaitSingle()
}

private suspend fun requestImageData(imageUrl: String): BufferedImage {
    return ReactorClient
        .url(imageUrl)
        .accept(APPLICATION_OCTET_STREAM)
        .retrieve()
        .bodyToMono(ByteArrayResource::class.java)
        .map { byteArray ->
            ImageIO.read(byteArray.inputStream)
        }.awaitSingle()
}



