package de.e2.coroutine.reactor

import kotlinx.coroutines.experimental.delay
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toMono


@SpringBootApplication
class ReactorApplication {

    @Bean
    fun router() = router {
        GET("/1") { request ->
            ServerResponse.ok().monoBody { "Hallo1" }
        }
        GET("/2") { request ->
            ServerResponse.ok().monoBody("Hallo2".toMono())
        }

        GET("/3") { request ->
            ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .fluxBody(onFinally = {
                    println("doFinally $it")
                }, onSubscribe = {
                    println("onSubscribe $it")
                }
                ) {
                    while (isActive) {
                        delay(1000)
                        send("Event")
                    }
                }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ReactorApplication::class.java, *args)
        }
    }
}
