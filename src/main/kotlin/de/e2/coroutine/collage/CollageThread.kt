@file:Suppress("PackageDirectoryMismatch")

package de.e2.coroutine.collage.thread

import com.jayway.jsonpath.JsonPath
import de.e2.coroutine.JerseyClient
import de.e2.coroutine.combineImages
import java.awt.image.BufferedImage
import java.io.FileOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.ws.rs.core.MediaType

fun main(): Unit = JerseyClient.use {
    val collage = createCollage("dogs", 20)
    ImageIO.write(collage, "png", FileOutputStream("dogs.png"))
}

fun createCollage(query: String, count: Int): BufferedImage {
    val urls = requestImageUrls(query, count)
    val images = urls.map { requestImageData(it) }
    val newImage = combineImages(images)
    return newImage
}

private fun requestImageUrls(query: String, count: Int = 20): List<String> {
    val json = JerseyClient.pixabay("q=$query&per_page=$count")
        .request()
        .get(String::class.java)
    return JsonPath.read(json, "$..previewURL")
}

private fun requestImageData(imageUrl: String): BufferedImage {
    val inputStream = JerseyClient.url(imageUrl)
        .request(MediaType.APPLICATION_OCTET_STREAM)
        .get(InputStream::class.java)
    return ImageIO.read(inputStream)
}


