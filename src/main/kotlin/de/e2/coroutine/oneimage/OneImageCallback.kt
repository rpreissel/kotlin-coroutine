@file:Suppress("PackageDirectoryMismatch")

package de.e2.coroutine.oneimage.callback

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType

typealias OnSuccess<T> = (T) -> Unit
typealias OnFailure = (Throwable) -> Unit


fun main(args: Array<String>) = JerseyClient.use {
    loadOneImage("dogs") { image ->
        println("${image.width}x${image.height}")
    }

    Thread.sleep(5000)
}

fun loadOneImage(
    query: String,
    onFailure: OnFailure = {},
    onSuccess: OnSuccess<BufferedImage>
) {
    requestImageUrl(query, onFailure) { url ->
        requestImageData(url, onFailure) { image ->
            onSuccess(image)
        }
    }
}

private fun requestImageUrl(
    query: String, onFailure: OnFailure = {}, onSuccess: OnSuccess<String>
) {
    JerseyClient.pixabay("q=$query")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                val url = urls.firstOrNull() ?: return failed(IllegalStateException("No image found"))
                onSuccess(url)
            }

            override fun failed(throwable: Throwable) {
                onFailure(throwable)
            }
        })
}

private fun requestImageData(
    imageUrl: String, onFailure: OnFailure = {}, onSuccess: OnSuccess<BufferedImage>
) {
    JerseyClient.url(imageUrl)
        .request(MediaType.APPLICATION_OCTET_STREAM)
        .async()
        .get(object : InvocationCallback<InputStream> {
            override fun completed(response: InputStream) {
                val image = ImageIO.read(response)
                onSuccess(image)
            }

            override fun failed(throwable: Throwable) {
                onFailure(throwable)
            }
        })
}


