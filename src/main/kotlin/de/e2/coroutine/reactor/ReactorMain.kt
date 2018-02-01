package de.e2.coroutine.reactor

import de.e2.coroutine.collage.reactive.createCollage
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.reactor.mono
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


@SpringBootApplication
class ReactorApplication {

    @Bean
    fun router() = router {
        GET("/1") { _ ->
            ServerResponse.ok().monoBody { "Hallo1" }
        }
        GET("/query/{query}") { request ->
            val query = request.pathVariable("query")
            val mono = mono {
                val collage = createCollage(query, 20)
                val byteArrayResource = ByteArrayOutputStream().let {
                    ImageIO.write(collage, "png", it)
                    ByteArrayResource(it.toByteArray())
                }
                byteArrayResource
            }


            ServerResponse.ok().contentType(MediaType.IMAGE_PNG).monoBody(mono)

        }

        GET("/event") { _ ->
            val flux = flux {
                while (isActive) {
                    send("/query/dog")
                    delay(5000)
                    send("/query/cat")
                    delay(5000)
                    send("/query/turtle")
                    delay(5000)
                }
            }.doFinally { println("doFinally $it") }
                .doOnSubscribe { println("onSubscribe $it") }

            ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .fluxBody(flux)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ReactorApplication::class.java, *args)
        }
    }
}
