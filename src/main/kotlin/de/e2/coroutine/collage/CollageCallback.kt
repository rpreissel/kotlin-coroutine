@file:Suppress("PackageDirectoryMismatch")

package de.e2.coroutine.collage.callback

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.MediaType

typealias OnSuccess<T> = (T) -> Unit
typealias OnFailure = (Throwable) -> Unit


fun main(args: Array<String>) = JerseyClient.use {
    createCollage("dogs", 20) { collage ->
        ImageIO.write(collage, "png", FileOutputStream("dogs.png"))
    }

    Thread.sleep(5000)
}

fun createCollage(query: String, count: Int, onSuccess: OnSuccess<BufferedImage>) {
    requestImageUrls(query, count) { urls ->
        fun loadImage(
            urlIter: Iterator<String>,
            retrievedImages: List<BufferedImage>
        ) {
            if (urlIter.hasNext()) {
                requestImageData(urlIter.next()) { image ->
                    loadImage(urlIter, retrievedImages + image)
                }
            } else {
                onSuccess(combineImages(retrievedImages))
            }
        }
        loadImage(urls.iterator(), listOf())
    }
}

private fun requestImageUrls(
    query: String,
    count: Int = 20,
    onFailure: OnFailure = {},
    onSuccess: OnSuccess<List<String>>
) {
    JerseyClient.pixabay("q=$query&per_page=$count")
        .request()
        .async()
        .get(object : InvocationCallback<String> {
            override fun completed(response: String) {
                val urls = JsonPath.read<List<String>>(response, "$..previewURL")
                onSuccess(urls)
            }

            override fun failed(throwable: Throwable) {
                onFailure(throwable)
            }
        })
}

private fun requestImageData(
    imageUrl: String,
    onFailure: OnFailure = {},
    onSuccess: OnSuccess<BufferedImage>
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


