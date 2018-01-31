package de.e2.coroutine

class Timer(timeToGo: Long) {
    val endTime = System.currentTimeMillis() + timeToGo

    fun timeToGo() = (endTime - System.currentTimeMillis()).takeIf { it > 0 } ?: 0

    fun isRunning() = timeToGo() > 0
}