package net.igsoft.funclient

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import net.igsoft.funclient.model.FileBody
import net.igsoft.funclient.model.Method
import net.igsoft.funclient.model.Request
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

@Timeout(20)
class FileBodyHttpTest {
    @TempDir
    lateinit var directory: Path

    private data class Received(val bytes: ByteArray, val length: String?, val transferEncoding: String?)

    @ParameterizedTest
    @EnumSource(value = Method::class, names = ["POST", "PUT"])
    fun `sends exact ranges with content length including empty ranges`(method: Method) {
        val bytes = ByteArray(256 * 1024 + 49) { (it * 31 + 7).toByte() }
        val file = directory.resolve("upload.bin").toFile().apply { writeBytes(bytes) }
        val received = LinkedBlockingQueue<Received>()
        withServer({ exchange ->
            received.add(Received(
                exchange.requestBody.use { it.readBytes() },
                exchange.requestHeaders.getFirst("Content-Length"),
                exchange.requestHeaders.getFirst("Transfer-Encoding"),
            ))
            exchange.sendResponseHeaders(204, -1)
        }) { url ->
            val client = FunClient(RecordingSerializer())
            val cases = listOf(
                FileBody(file) to bytes,
                FileBody(file, 0, bytes.size.toLong()) to bytes,
                FileBody(file, length = 5) to bytes.copyOfRange(0, 5),
                FileBody(file, 1, 1) to bytes.copyOfRange(1, 2),
                FileBody(file, bytes.lastIndex.toLong(), 1) to bytes.takeLast(1).toByteArray(),
                FileBody(file, 12345) to bytes.copyOfRange(12345, bytes.size),
                FileBody(file, 31, 70000) to bytes.copyOfRange(31, 70031),
                FileBody(file, length = 0) to byteArrayOf(),
                FileBody(file, 3, 0) to byteArrayOf(),
                FileBody(file, bytes.size.toLong(), 0) to byteArrayOf(),
                FileBody(file, bytes.size.toLong()) to byteArrayOf(),
            )
            for ((body, expected) in cases) {
                assertEquals(204, client.execute(Request(method, url, body = body)).status)
                val actual = requireNotNull(received.poll(5, SECONDS))
                assertArrayEquals(expected, actual.bytes, body.toString())
                assertEquals(expected.size.toString(), actual.length, body.toString())
                assertNull(actual.transferEncoding)
            }

            val empty = directory.resolve("empty.bin").toFile().apply { writeBytes(byteArrayOf()) }
            client.execute(Request(method, url, body = FileBody(empty)))
            val actual = requireNotNull(received.poll(5, SECONDS))
            assertArrayEquals(byteArrayOf(), actual.bytes)
            assertEquals("0", actual.length)
        }
    }

    @Test
    fun `invalid ranges and inaccessible files fail before HTTP requests`() {
        val file = directory.resolve("upload.bin").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val requests = AtomicInteger()
        withServer({ exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(204, -1)
        }) { url ->
            val client = FunClient(RecordingSerializer())
            for (body in listOf(
                FileBody(file, 4), FileBody(file, 4, 0), FileBody(file, 3, 1),
                FileBody(file, 1, 3), FileBody(file, length = Long.MAX_VALUE),
                FileBody(file, Long.MAX_VALUE, 0),
            )) {
                assertThrows(IllegalArgumentException::class.java) {
                    client.execute(Request(Method.PUT, url, body = body))
                }
            }
            for (length in listOf(null, 0L, 1L)) {
                assertThrows(IOException::class.java) {
                    client.execute(Request(Method.PUT, url, body = FileBody(directory.resolve("missing").toFile(), length = length)))
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                client.execute(Request(Method.PUT, url, body = FileBody(directory.toFile())))
            }
            assertEquals(0, requests.get())
        }
    }

    @Test
    fun `large offsets use long byte positions`() {
        val path = directory.resolve("sparse.bin")
        val offset = Int.MAX_VALUE.toLong() + 1024
        val bytes = ByteArray(257) { it.toByte() }
        FileChannel.open(path, CREATE_NEW, WRITE, SPARSE).use { channel ->
            channel.position(offset)
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
        }
        val received = LinkedBlockingQueue<ByteArray>()
        withServer({ exchange ->
            received.add(exchange.requestBody.use { it.readBytes() })
            assertEquals(bytes.size.toString(), exchange.requestHeaders.getFirst("Content-Length"))
            exchange.sendResponseHeaders(204, -1)
        }) { url ->
            val client = FunClient(RecordingSerializer())
            for (length in listOf(null, bytes.size.toLong())) {
                client.execute(Request(Method.PUT, url, body = FileBody(path.toFile(), offset, length)))
                assertArrayEquals(bytes, received.poll(5, SECONDS))
            }
        }
    }

    @Test
    fun `publisher fixes range length despite growth and supports repeated sends`() {
        val file = directory.resolve("growing.bin").toFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val body = FileBody(file, 1)
        val publisher = FileBodyPublisher.create(body)
        file.appendBytes(byteArrayOf(5, 6))
        assertEquals(3, publisher.contentLength())
        val received = LinkedBlockingQueue<Received>()
        withServer({ exchange ->
            received.add(Received(exchange.requestBody.use { it.readBytes() }, exchange.requestHeaders.getFirst("Content-Length"), null))
            exchange.sendResponseHeaders(204, -1)
        }) { url ->
            HttpClient.newHttpClient().use { client ->
                repeat(2) {
                    client.send(HttpRequest.newBuilder(url.toURI()).PUT(publisher).build(), HttpResponse.BodyHandlers.discarding())
                    val actual = requireNotNull(received.poll(5, SECONDS))
                    assertArrayEquals(byteArrayOf(2, 3, 4), actual.bytes)
                    assertEquals("3", actual.length)
                }
            }
            // A new execute resolves length=null again against the current file size.
            FunClient(RecordingSerializer()).execute(Request(Method.PUT, url, body = body))
            val actual = requireNotNull(received.poll(5, SECONDS))
            assertArrayEquals(byteArrayOf(2, 3, 4, 5, 6), actual.bytes)
            assertEquals("5", actual.length)
        }
    }

    @Test
    fun `shortening file during HTTP upload fails instead of silently completing`() {
        val path = directory.resolve("truncated.bin")
        val size = 64L * 1024 * 1024
        FileChannel.open(path, CREATE_NEW, WRITE, SPARSE).use { channel ->
            channel.position(size - 1)
            channel.write(ByteBuffer.wrap(byteArrayOf(1)))
        }
        val serverResult = LinkedBlockingQueue<Pair<Long, IOException?>>()
        withServer({ exchange ->
            var count = 0L
            var failure: IOException? = null
            try {
                exchange.requestBody.use { input ->
                    assertEquals(size.toString(), exchange.requestHeaders.getFirst("Content-Length"))
                    assertNotEquals(-1, input.read())
                    count++
                    FileChannel.open(path, WRITE).use { it.truncate(0) }
                    val buffer = ByteArray(8192)
                    while (true) {
                        val n = input.read(buffer)
                        if (n == -1) break
                        count += n
                    }
                }
                exchange.sendResponseHeaders(204, -1)
            } catch (expected: IOException) {
                failure = expected
            } finally {
                serverResult.add(count to failure)
            }
        }) { url ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val result = executor.submit<Throwable?> {
                    runCatching {
                        FunClient(RecordingSerializer()).execute(Request(Method.PUT, url, body = FileBody(path.toFile())))
                    }.exceptionOrNull()
                }
                val failure = result.get(10, SECONDS)
                assertInstanceOf(IOException::class.java, failure)
                assertTrue(generateSequence(failure) { it.cause }.any { it is EOFException }, failure.toString())
                val (received, readFailure) = requireNotNull(serverResult.poll(5, SECONDS))
                assertTrue(received in 1 until size)
                assertNotNull(readFailure)
            } finally {
                executor.shutdownNow()
            }
        }
    }

    private fun withServer(handler: (HttpExchange) -> Unit, action: (URL) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val failures = LinkedBlockingQueue<Throwable>()
        server.createContext("/") { exchange ->
            try {
                handler(exchange)
            } catch (failure: Throwable) {
                failures.add(failure)
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            action(URI("http://127.0.0.1:${server.address.port}/").toURL())
            failures.poll()?.let { throw AssertionError("HTTP server failed", it) }
        } finally {
            server.stop(0)
        }
    }
}
