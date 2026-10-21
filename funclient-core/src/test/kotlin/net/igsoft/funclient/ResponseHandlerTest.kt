package net.igsoft.funclient

import net.igsoft.funclient.handler.*
import net.igsoft.funclient.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import kotlin.reflect.typeOf

class ResponseHandlerTest {
    private val request = Request(Method.GET, URI("http://localhost/").toURL())
    private val type = typeOf<List<Map<String, Int?>>?>()
    private val headers = Headers(mapOf("X-Response" to listOf("one", "two")))

    @ParameterizedTest
    @ValueSource(ints = [200, 201, 204, 299])
    fun `object and typed handlers delegate complete response type`(status: Int) {
        val response = RawResponse(status, headers, StringBody("serialized response"))
        for (decoded in listOf(listOf(mapOf("id" to 1, "missing" to null)), null)) {
            val serializer = RecordingSerializer().apply { this.decoded = decoded }
            val result = ObjectResponseHandler<List<Map<String, Int?>>?>(type).handle(request, response, serializer)
            assertSame(decoded, result)
            val typed = TypedResponseHandler<List<Map<String, Int?>>?>(type).handle(request, response, serializer)
            assertEquals(status, typed.status)
            assertSame(headers, typed.headers)
            assertSame(decoded, typed.body.value)
            assertEquals(type, typed.body.type)
            assertEquals(List(2) { "serialized response" to type }, serializer.deserialized)
            assertTrue(serializer.serialized.isEmpty())
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [199, 300, 400, 404, 500])
    fun `object handlers reject unsuccessful statuses before deserializing while raw passes through`(status: Int) {
        val serializer = RecordingSerializer()
        val response = RawResponse(status, headers, EmptyBody)
        for (handler in listOf(ObjectResponseHandler<Any?>(type), TypedResponseHandler<Any?>(type))) {
            val failure = assertThrows(IllegalStateException::class.java) { handler.handle(request, response, serializer) }
            assertEquals("Wrong response http status. $status", failure.message)
        }
        assertSame(response, RawResponseHandler().handle(request, response, serializer))
        assertTrue(serializer.deserialized.isEmpty())
    }

    @Test
    fun `handlers propagate serializer failures and raw handler leaves success untouched`() {
        val failure = IllegalArgumentException("test deserialization failure")
        val serializer = RecordingSerializer().apply { this.failure = failure }
        val response = RawResponse(200, headers, StringBody("invalid input"))
        for (handler in listOf(ObjectResponseHandler<Any?>(type), TypedResponseHandler<Any?>(type))) {
            assertSame(failure, assertThrows(IllegalArgumentException::class.java) { handler.handle(request, response, serializer) })
        }
        assertSame(response, RawResponseHandler().handle(request, response, serializer))
    }
}
