import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

interface Downloader {
    suspend fun download()
}

class MockkTest {
    @Test
    fun mockk1() {
    val mock =  mockk<Downloader> {
        coEvery {
            this@mockk.download()
        } coAnswers {
            delay(1000 )
        }
    }

    runBlocking {
        val a= async {
            mock.download()
        }
        val b= async {
            val time = measureTimeMillis {
                mock.download()
            }
            println("B $time")
        }
        val c= async {
            val time = measureTimeMillis {
                mock.download()
            }
            println("C $time")
        }

        a.await()
        b.await()
        c.await()
    }
    }
}