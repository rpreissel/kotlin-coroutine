##### Timeouts

```kotlin
val fastImageOrNull: BufferedImage? = withTimeoutOrNull(100) {
    loadFastestImage("dogs", 20)
}
```
<span class="fragment current-only" data-code-focus="1"></span>

---

##### Timeouts und Selects

```kotlin
suspend fun loadFastestImage(query: String, count: Int, timeoutMs: Long): BufferedImage {
    val urls = requestImageUrls(query, count)
    val deferredImages = urls.map {
        async { requestImageData(it) }
    }
    val image: BufferedImage = select {
        for (deferredImage in deferredImages) {
            deferredImage.onAwait { image ->
                image
            }
        }

        onTimeout(timeoutMs) {
            DEFAULT_IMAGE
        }
    }
    return image
}
```
<span class="fragment current-only" data-code-focus="6,13-15"></span>

---

##### Continuation

```kotlin
public interface Continuation<in T> {
    public val context: CoroutineContext

    //Die unterbrochene Continuation beim gespeicherten
    //Label mit dem Wert wieder starten
    public fun resume(value: T)

    //Die unterbrochene Continuation beim gespeicherten
    //Label mit einer Exception wieder starten.
    public fun resumeWithException(exception: Throwable)
}
```

<small class="fragment current-only" data-code-focus="6"></small>
<small class="fragment current-only" data-code-focus="10"></small>
