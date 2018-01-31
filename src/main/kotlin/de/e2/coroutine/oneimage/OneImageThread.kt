package de.e2.coroutine.oneimage.thread

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) = JerseyClient.use {
    val image = loadOneImage("dogs")
    println("${image.width}x${image.height}")
}

fun loadOneImage(query: String): BufferedImage {
    val url = requestImageUrl(query)
    val image = requestImageData(url)
    return image
}

private fun requestImageUrl(query: String): String {
    val json = JerseyClient.pixabay("q=$query")
        .request()
        .get(String::class.java)
    return JsonPath.read<List<String>>(json, "$..previewURL").firstOrNull()
            ?: throw IllegalStateException("No image found")
}

private fun requestImageData(imageUrl: String): BufferedImage {
    val inputStream = JerseyClient.url(imageUrl)
        .request(MediaType.APPLICATION_OCTET_STREAM)
        .get(InputStream::class.java)
    return ImageIO.read(inputStream)
}


