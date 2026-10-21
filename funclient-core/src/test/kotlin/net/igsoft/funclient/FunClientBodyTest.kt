package net.igsoft.funclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import net.igsoft.funclient.model.EmptyBody
import net.igsoft.funclient.model.FileBody
import net.igsoft.funclient.model.ObjectBody
import net.igsoft.funclient.model.Method
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.StringBody
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

class FunClientBodyTest {
    @Test
    fun `strict rejects explicit bodies before reading files or connecting`() {
        val serializer = RecordingSerializer()
        for (method in listOf(Method.GET, Method.DELETE)) {
            for (body in listOf(StringBody(""), StringBody("text"), ObjectBody(mapOf("id" to 1), typeOf<Map<String, Int>>()), ObjectBody(null, typeOf<String?>()), FileBody(java.io.File("missing-upload")))) {
                val failure = runCatching {
                    FunClient(serializer).execute(Request(method, java.net.URI("http://127.0.0.1:1/").toURL(), body = body))
                }.exceptionOrNull()
                assertThat(failure).isNotNull().isInstanceOf(IllegalArgumentException::class)
            }
        }
        assertThat(serializer.serialized).isEqualTo(emptyList())
    }

    @Test
    fun `permissive sends exact file and text bodies and strict permits empty bodies`() {
        val file = Files.createTempFile("funclient", ".bin").toFile()
        val bytes = byteArrayOf(0, -1, 127, -128, 13, 10)
        file.writeBytes(bytes)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val received = java.util.concurrent.LinkedBlockingQueue<Pair<String, List<Byte>>>()
        server.createContext("/") { exchange ->
            received.add(exchange.requestMethod to exchange.requestBody.use { it.readBytes().toList() })
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()
        try {
            for (method in listOf(Method.GET, Method.DELETE)) {
                val url = java.net.URI("http://127.0.0.1:${server.address.port}/").toURL()
                FunClient(RecordingSerializer()).execute(Request(method, url, body = EmptyBody))
                assertThat(received.poll(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(method.name to emptyList())
                for ((body, expected) in listOf(StringBody("żółć") to "żółć".toByteArray().toList(), FileBody(file) to bytes.toList())) {
                    FunClient(RecordingSerializer(), strict = false).execute(Request(method, url, body = body))
                    assertThat(received.poll(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(method.name to expected)
                }
            }
        } finally {
            server.stop(0)
            file.delete()
        }
    }
}
