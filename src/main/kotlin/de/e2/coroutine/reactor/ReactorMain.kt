package de.e2.coroutine.reactor

import de.e2.coroutine.collage.reactive.createCollage
import de.e2.coroutine.collage.reactive.requestImageData
import de.e2.coroutine.collage.reactive.requestImageUrls
import de.e2.coroutine.combineImages
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.reactor.mono
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.selects.selectUnbiased
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.coroutines.experimental.CoroutineContext


@SpringBootApplication
class ReactorApplication {

    @Bean
    fun router() = router {
        GET("/query/{query}") { request ->
            val query = request.pathVariable("query")
            val mono = mono {
                val collage = createCollage(query, 20)
                val byteArrayResource = ByteArrayOutputStream()
                ImageIO.write(collage, "png", byteArrayResource)
                ByteArrayResource(byteArrayResource.toByteArray())
            }

            ServerResponse.ok().contentType(MediaType.IMAGE_PNG).body(mono, ByteArrayResource::class.java)

        }

        val eventImages = ConcurrentHashMap<String, ByteArrayResource>()

        GET("/event/{client}") { request ->
            val client = request.pathVariable("client")
            val imageData = eventImages[client]
            if (imageData == null) {
                ServerResponse.notFound().build()
            } else {
                ServerResponse.ok().contentType(MediaType.IMAGE_PNG).body(fromObject(imageData))
            }
        }

        GET("/event") { _ ->
            val clientId = UUID.randomUUID().toString()
            val flux = flux {
                val dogs = retrieveImages("dog", coroutineContext);
                val cats = retrieveImages("cat", coroutineContext);
                val turtle = retrieveImages("turtle", coroutineContext);
                while (isActive) {
                    val collage = createCollage(20, dogs, cats, turtle)
                    val byteArrayResource = ByteArrayOutputStream()
                    ImageIO.write(collage, "png", byteArrayResource)
                    eventImages.put(clientId, ByteArrayResource(byteArrayResource.toByteArray()))
                    send("/event/$clientId?${System.currentTimeMillis()}")
                }
            }.doFinally {
                println("doFinally $it")
                eventImages.remove(clientId)
            }

            ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(flux, String::class.java)
        }


    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ReactorApplication::class.java, *args)
        }
    }
}

suspend fun createCollage(
    count: Int,
    vararg channels: ReceiveChannel<BufferedImage>
): BufferedImage {
    while (true) {
        val images = (1..count).map {
            selectUnbiased<BufferedImage> {
                channels.forEach { channel ->
                    channel.onReceive { it }
                }
            }
        }
        return combineImages(images)
    }
}

suspend fun retrieveImages(
    query: String,
    context: CoroutineContext
): ReceiveChannel<BufferedImage> = produce(context) {
    while (isActive) {
        try {
            val urls = requestImageUrls(query, 20)
            for (url in urls) {
                val image = requestImageData(url)
                send(image)
                delay(1000)
            }
        } catch (exc: Exception) {
            delay(1, TimeUnit.SECONDS)
        }
    }
}