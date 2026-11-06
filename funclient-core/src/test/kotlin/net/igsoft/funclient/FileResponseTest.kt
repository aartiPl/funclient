package net.igsoft.funclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Path
import net.igsoft.funclient.model.FileBody
import net.igsoft.funclient.model.FileResponseBodyTarget
import net.igsoft.funclient.model.Method
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.StringBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileResponseTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `file target is data and success preserves exact bytes and length`() {
        val destination = directory.resolve("binary.dat").toFile()
        val target = FileResponseBodyTarget(destination)
        assertThat(target).isEqualTo(FileResponseBodyTarget(destination))
        assertThat(destination.exists()).isEqualTo(false)
        val bytes = ByteArray(2 * 1024 * 1024 + 13) { it.toByte() }
        serve(200, bytes) { url ->
            val response = FunClient(RecordingSerializer()).execute(Request(Method.GET, url, responseBodyTarget = target))
            assertThat(response.status).isEqualTo(200)
            assertThat(response.body).isEqualTo(FileBody(destination.toPath().toAbsolutePath().normalize().toFile(), length = bytes.size.toLong()))
            assertThat(destination.readBytes().contentEquals(bytes)).isEqualTo(true)
        }
    }

    @Test
    fun `HTTP errors and redirects preserve destination and expose text and headers`() {
        val destination = directory.resolve("existing.dat").toFile().apply { writeText("original") }
        for (status in listOf(302, 404, 500)) {
            serve(status, "response details".toByteArray()) { url ->
                val response = FunClient(RecordingSerializer()).execute(Request(Method.GET, url,
                    responseBodyTarget = FileResponseBodyTarget(destination, overwrite = true)))
                assertThat(response.status).isEqualTo(status)
                assertThat(response.body).isEqualTo(StringBody("response details"))
                assertThat(response.headers.all["location"]).isEqualTo(listOf("https://example.com/next"))
                assertThat(destination.readText()).isEqualTo("original")
            }
        }
    }

    @Test
    fun `empty and partial successful responses become complete separate files`() {
        for ((status, bytes) in listOf(204 to byteArrayOf(), 206 to byteArrayOf(0, -1, -128))) {
            val file = directory.resolve("$status.dat").toFile()
            serve(status, bytes) { url ->
                val response = FunClient(RecordingSerializer()).execute(Request(Method.GET, url,
                    responseBodyTarget = FileResponseBodyTarget(file)))
                assertThat(response.body).isInstanceOf(FileBody::class)
                assertThat(file.readBytes().contentEquals(bytes)).isEqualTo(true)
            }
        }
    }

    @Test
    fun `existing target requires overwrite and truncated transfers preserve old file and clean staging`() {
        val file = directory.resolve("existing.dat").toFile().apply { writeText("original") }
        serve(200, "new bytes".toByteArray()) { url ->
            assertThat(runCatching { FunClient(RecordingSerializer()).execute(Request(Method.GET, url,
                responseBodyTarget = FileResponseBodyTarget(file))) }).isFailure()
            assertThat(file.readText()).isEqualTo("original")
            FunClient(RecordingSerializer()).execute(Request(Method.GET, url,
                responseBodyTarget = FileResponseBodyTarget(file, overwrite = true)))
            assertThat(file.readText()).isEqualTo("new bytes")
        }
        file.writeText("original")
        serve(200, byteArrayOf(1, 2), declaredSize = 100) { url ->
            assertThat(runCatching { FunClient(RecordingSerializer()).execute(Request(Method.GET, url,
                responseBodyTarget = FileResponseBodyTarget(file, overwrite = true))) }).isFailure()
            assertThat(file.readText()).isEqualTo("original")
            assertThat(directory.toFile().list()!!.toList()).isEqualTo(listOf("existing.dat"))
        }
    }

    private fun serve(status: Int, bytes: ByteArray, declaredSize: Long = bytes.size.toLong(), action: (java.net.URL) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Location", "https://example.com/next")
            exchange.sendResponseHeaders(status, if (status == 204) -1 else declaredSize)
            try { if (status != 204) exchange.responseBody.write(bytes) } finally { runCatching { exchange.close() } }
        }
        server.start()
        try { action(URI("http://127.0.0.1:${server.address.port}/").toURL()) } finally { server.stop(0) }
    }
}
