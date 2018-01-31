package de.e2.coroutine

import java.io.BufferedReader
import java.io.InputStreamReader


object Constants {
    private val API_KEY_FILE = "pixabay.apikey"
    private val PIXABAY_URL_TEMPLATE = "https://pixabay.com/api/?key=%s&image_type=photo"

    val PIXABAY_URL: String by lazy {
        Constants::class.java.classLoader.getResourceAsStream(API_KEY_FILE)?.use {
            val apikey = BufferedReader(InputStreamReader(it)).readLine()
            String.format(PIXABAY_URL_TEMPLATE, apikey)
        } ?: throw IllegalStateException("Could not find $API_KEY_FILE in classpath")
    }
}