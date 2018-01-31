package de.e2.coroutine

import de.e2.coroutine.Constants.PIXABAY_URL
import org.springframework.web.reactive.function.client.WebClient
import java.io.Closeable


object ReactorClient : Closeable {
    private val clientLazyDelegate = lazy {
        WebClient.create()
    }
    private val client by clientLazyDelegate

    override fun close() {
    }

    fun pixabay(params: String) = client.get().uri("$PIXABAY_URL&$params")
    fun url(url: String) = client.get().uri(url)
}