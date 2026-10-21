package net.igsoft.funclient

import com.sun.net.httpserver.HttpServer
import net.igsoft.funclient.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf

class FunClientTransportTest {
    private data class Received(val method: String, val body: List<Byte>, val headers: List<String>)

    @ParameterizedTest
    @EnumSource(Method::class)
    fun `all methods preserve bodies headers and response status`(method: Method) {
        val serializer = RecordingSerializer()
        val declaredType = typeOf<List<Map<String, Int?>>?>()
        val value = listOf(mapOf("id" to 1, "missing" to null))
        val objectBody = Body.serialized(value, declaredType)
        val inferredBody = Body.serialized<List<Map<String, Int?>>?>(value)
        val file = Files.createTempFile("funclient-transport", ".bin").toFile()
        val fileBytes = byteArrayOf(0, -1, 127, -128, 13, 10)
        file.writeBytes(fileBytes)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val received = LinkedBlockingQueue<Received>()
        server.createContext("/") { exchange ->
            received.add(Received(exchange.requestMethod, exchange.requestBody.use { it.readBytes().toList() }, exchange.requestHeaders["X-Request"]!!.toList()))
            exchange.responseHeaders.add("X-Response", "first")
            exchange.responseHeaders.add("X-Response", "second")
            val responseBytes = "odpowiedź".toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=UTF-8")
            exchange.sendResponseHeaders(418, if (method == Method.HEAD) -1 else responseBytes.size.toLong())
            if (method != Method.HEAD) exchange.responseBody.write(responseBytes)
            exchange.close()
        }
        server.start()
        try {
            val url = URI("http://127.0.0.1:${server.address.port}/").toURL()
            val bodies = listOf(
                Body.empty() to emptyList(),
                Body.text("") to emptyList(),
                Body.text("żółć") to "żółć".toByteArray().toList(),
                Body.file(file) to fileBytes.toList(),
                objectBody to serializer.encoded.toByteArray().toList(),
                inferredBody to serializer.encoded.toByteArray().toList(),
                Body.serialized<List<Map<String, Int?>>?>(null) to serializer.encoded.toByteArray().toList(),
            )
            for (strict in listOf(false, true)) {
                for ((body, expected) in bodies) {
                    if (strict && method in listOf(Method.GET, Method.DELETE) && body !== EmptyBody) continue
                    val response = FunClient(serializer, strict).execute(
                        Request(method, url, Headers.builder().append("X-Request", "one").append("X-Request", "two").build(), body)
                    )
                    assertEquals(Received(method.name, expected, listOf("one", "two")), received.poll(5, TimeUnit.SECONDS))
                    assertEquals(418, response.status)
                    assertEquals(listOf("first", "second"), response.headers.all["x-response"])
                    assertEquals(StringBody(if (method == Method.HEAD) "" else "odpowiedź"), response.body)
                }
            }
            val repetitions = if (method in listOf(Method.GET, Method.DELETE)) 1 else 2
            assertEquals(List(repetitions) { listOf(value to declaredType, value to declaredType, null to declaredType) }.flatten(), serializer.serialized)
            assertTrue(serializer.deserialized.isEmpty())
        } finally {
            server.stop(0)
            file.delete()
        }
    }

    @Test
    fun `serialization failure reaches caller before any network connection`() {
        val failure = IllegalArgumentException("test serialization failure")
        val serializer = RecordingSerializer().apply { this.failure = failure }
        val request = Request(Method.POST, URI("http://127.0.0.1:1/").toURL(), body = ObjectBody(null, typeOf<List<String?>?>()))
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { FunClient(serializer).execute(request) })
    }
}
