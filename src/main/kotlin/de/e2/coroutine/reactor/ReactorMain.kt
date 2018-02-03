package de.e2.coroutine.reactor

import de.e2.coroutine.collage.reactive.requestImageData
import de.e2.coroutine.collage.reactive.requestImageUrls
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.reactor.mono
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.coroutines.experimental.CoroutineContext
import de.e2.coroutine.collage.reactive.createCollage as createCollageReactive
import de.e2.coroutine.csp.producer.createCollage as createCollageProducer


@SpringBootApplication
class ReactorApplication {

    @Bean
    fun router() = router {

        //Workaround for https://github.com/spring-projects/spring-boot/issues/9785
        val indexHtml = ClassPathResource("static/index.html")

        GET("/") {
            ServerResponse.ok().contentType(MediaType.TEXT_HTML).syncBody(indexHtml)
        }

        GET("/query/{query}") { request ->
            val query = request.pathVariable("query")
            val mono = mono {
                val collage = createCollageReactive(query, 20)
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
                    val collage = createCollageProducer(20, dogs, cats, turtle)
                    val byteArrayResource = ByteArrayOutputStream()
                    ImageIO.write(collage, "png", byteArrayResource)
                    eventImages.put(clientId, ByteArrayResource(byteArrayResource.toByteArray()))
                    send("/event/$clientId")
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

suspend fun retrieveImages(
    query: String,
    context: CoroutineContext
): ReceiveChannel<BufferedImage> = produce(context) {
    while (isActive) {
        val urls = requestImageUrls(query, 20)
        for (url in urls) {
            val image = requestImageData(url)
            send(image)
            delay(1000)
        }
        if (urls.isEmpty()) {
            delay(1000)
        }
    }
}