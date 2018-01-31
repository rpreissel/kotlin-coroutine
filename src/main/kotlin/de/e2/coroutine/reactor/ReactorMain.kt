package de.e2.coroutine.reactor

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.reactor.mono
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.publisher.toMono
import kotlin.coroutines.experimental.CoroutineContext

inline fun <reified T> ServerResponse.BodyBuilder.monoBody(
    context: CoroutineContext = Unconfined,
    parent: Job? = null,
    noinline block: suspend CoroutineScope.() -> T?
): Mono<ServerResponse> = body(mono(context, parent, block), T::class.java)

inline fun <reified T> ServerResponse.BodyBuilder.monoBody(
    mono: Mono<T>
): Mono<ServerResponse> = body(mono, T::class.java)

fun <T> Flux<T>.applyCallbacks(onFinally: ((SignalType) -> Unit)?): Flux<T> {
    var result = this;

    onFinally?.let {
        result = result.doFinally(onFinally)
    }

    return result
}

inline fun <reified T> ServerResponse.BodyBuilder.fluxBody(
    context: CoroutineContext = Unconfined,
    noinline onFinally: ((SignalType) -> Unit)?,
    noinline block: suspend ProducerScope<T>.() -> Unit
): Mono<ServerResponse> = body(flux(context, block).run { applyCallbacks(onFinally) }, T::class.java)

data class Hello(var value: String)

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
                    println("onFinally $it")
                }) {
                    while (isActive) {
                        delay(1000)
                        send(Hello("Event"))
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
