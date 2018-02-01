package de.e2.coroutine.reactor

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactor.flux
import kotlinx.coroutines.experimental.reactor.mono
import org.reactivestreams.Subscription
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import kotlin.coroutines.experimental.CoroutineContext


inline fun <reified T> ServerResponse.BodyBuilder.monoBody(
    context: CoroutineContext = Unconfined,
    parent: Job? = null,
    noinline block: suspend CoroutineScope.() -> T?
): Mono<ServerResponse> = body(mono(context, parent, block), T::class.java)

inline fun <reified T> ServerResponse.BodyBuilder.monoBody(
    mono: Mono<T>
): Mono<ServerResponse> = body(mono, T::class.java)


inline fun <reified T> ServerResponse.BodyBuilder.fluxBody(
    context: CoroutineContext = Unconfined,
    noinline block: suspend ProducerScope<T>.() -> Unit
): Mono<ServerResponse> = body(flux(context, block), T::class.java)

inline fun <reified T> ServerResponse.BodyBuilder.fluxBody(
    flux: Flux<T>
): Mono<ServerResponse> = body(flux, T::class.java)
