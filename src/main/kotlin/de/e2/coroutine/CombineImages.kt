package de.e2.coroutine

import java.awt.Color
import java.awt.image.BufferedImage

fun combineImages(imageList: Collection<BufferedImage>): BufferedImage {
    if(imageList.isEmpty()) {
        return BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)
    }

    val yDim = Math.sqrt(imageList.size.toDouble()).toInt()
    val xDim = (imageList.size + yDim - 1) / yDim

    val maxDim = imageList.asSequence().map { Pair(it.width, it.height) }.fold(Pair(0, 0)) { a, b ->
        Pair(maxOf(a.first, b.first), maxOf(a.second, b.second))
    }

    val newImage = BufferedImage(maxDim.first * xDim, maxDim.second * yDim, BufferedImage.TYPE_3BYTE_BGR)
    val graphics = newImage.graphics
    graphics.color= Color.WHITE
    graphics.fillRect(0,0,newImage.width,newImage.height)

    imageList.forEachIndexed { index, subImage ->
        val x = index % xDim
        val y = index / xDim
        val posX = maxDim.first * x + (maxDim.first - subImage.width) / 2
        val posY = maxDim.second * y + (maxDim.second - subImage.height) / 2
        graphics.drawImage(subImage, posX, posY, null)
    }
    return newImage
}
