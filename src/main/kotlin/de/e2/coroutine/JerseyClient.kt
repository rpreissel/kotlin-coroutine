package de.e2.coroutine

import de.e2.coroutine.Constants.PIXABAY_URL
import org.glassfish.jersey.client.ClientConfig
import java.io.Closeable
import javax.ws.rs.client.ClientBuilder


object JerseyClient : Closeable {
    private val config = ClientConfig()
    private val clientLazyDelegate = lazy {
        ClientBuilder.newClient(config)
    }
    private val client by clientLazyDelegate

    override fun close() {
        if (clientLazyDelegate.isInitialized()) {
            client.close()
        }
    }

    fun pixabay(params: String) = client.target("$PIXABAY_URL&$params")
    fun url(url: String) = client.target(url)
}