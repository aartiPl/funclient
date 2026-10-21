package net.igsoft.funclient.serializer.kotlinx

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

class KotlinxSerializerTest {
    @Serializable
    data class Item(val id: Int, val label: String = "default")

    @Serializable
    data class Box<T>(val value: T)

    data class ExternalId(val value: String)

    @Serializable
    data class WithExternalId(@Contextual val id: ExternalId)

    @Serializable(with = UppercaseCodeSerializer::class)
    data class Code(val value: String)

    @Serializable
    abstract class Event

    @Serializable
    @SerialName("created")
    data class Created(val item: Item) : Event()

    object UppercaseCodeSerializer : KSerializer<Code> {
        override val descriptor = PrimitiveSerialDescriptor("Code", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Code) = encoder.encodeString(value.value.uppercase())
        override fun deserialize(decoder: Decoder) = Code(decoder.decodeString().lowercase())
    }

    private object ExternalIdSerializer : KSerializer<ExternalId> {
        override val descriptor = PrimitiveSerialDescriptor("ExternalId", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: ExternalId) = encoder.encodeString("id:${value.value}")
        override fun deserialize(decoder: Decoder): ExternalId {
            val text = decoder.decodeString()
            if (!text.startsWith("id:")) throw SerializationException("Invalid external ID")
            return ExternalId(text.removePrefix("id:"))
        }
    }

    private val serializer = KotlinxSerializer(Json)

    private inline fun <reified T> roundTrip(value: T, adapter: KotlinxSerializer = serializer) {
        val type = typeOf<T>()
        assertEquals(value, adapter.deserialize<T>(adapter.serialize(value, type), type))
    }

    @Test
    fun `round trip serializable object and primitive`() {
        roundTrip(Item(7, "żółć"))
        roundTrip("hello")
        roundTrip(42)
    }

    @Test
    fun `collections retain generic arguments and nullable elements`() {
        roundTrip(listOf(Item(1), Item(2)))
        roundTrip(emptyList<Item>())
        roundTrip(mapOf("items" to listOf(Item(1), null), "empty" to emptyList()))
        roundTrip(setOf(Item(1), Item(2)))
        roundTrip(Box(listOf(Box<Item?>(null), Box(Item(2)))))
    }

    @Test
    fun `nullable types support null and non null values`() {
        roundTrip<Item?>(null)
        roundTrip<Item?>(Item(1))
        roundTrip<List<Item?>?>(null)
        roundTrip<List<Item?>?>(listOf(null, Item(2)))
        assertEquals("null", serializer.serialize(null, typeOf<Item?>()))
        assertNull(serializer.deserialize<Item?>("null", typeOf<Item?>()))
        assertThrows(SerializationException::class.java) { serializer.deserialize<Item>("null", typeOf<Item>()) }
    }

    @Test
    fun `custom serializer on class is used`() {
        val type = typeOf<Code>()
        assertEquals("\"ABC\"", serializer.serialize(Code("abc"), type))
        assertEquals(Code("abc"), serializer.deserialize<Code>("\"ABC\"", type))
        roundTrip(listOf(Code("abc")))
    }

    @Test
    fun `supplied module resolves contextual top level nullable generic and nested types`() {
        val adapter = KotlinxSerializer(Json {
            serializersModule = SerializersModule { contextual(ExternalIdSerializer) }
        })
        assertEquals("\"id:123\"", adapter.serialize(ExternalId("123"), typeOf<ExternalId>()))
        roundTrip(ExternalId("123"), adapter)
        roundTrip<ExternalId?>(ExternalId("123"), adapter)
        roundTrip<ExternalId?>(null, adapter)
        roundTrip(listOf(ExternalId("123"), null), adapter)
        roundTrip(Box(listOf(ExternalId("123"))), adapter)
        roundTrip(WithExternalId(ExternalId("123")), adapter)
    }

    @Test
    fun `declared polymorphic type uses supplied module and discriminator`() {
        val adapter = KotlinxSerializer(Json {
            classDiscriminator = "kind"
            serializersModule = SerializersModule {
                polymorphic(Event::class) { subclass(Created::class) }
            }
        })
        val event: Event = Created(Item(7))
        val encoded = adapter.serialize(event, typeOf<Event>())
        assertTrue(encoded.contains("\"kind\":\"created\""))
        assertEquals(event, adapter.deserialize<Event>(encoded, typeOf<Event>()))
        roundTrip<List<Event?>>(listOf(event, null), adapter)
    }

    @Test
    fun `supplied Json configuration controls encoding and decoding`() {
        val adapter = KotlinxSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        })
        val text = adapter.serialize(Item(1), typeOf<Item>())
        assertTrue(text.contains("\n"))
        assertTrue(text.contains("\"label\": \"default\""))
        assertEquals(Item(1), adapter.deserialize<Item>("""{"id":1,"extra":true}""", typeOf<Item>()))
        assertThrows(SerializationException::class.java) {
            serializer.deserialize<Item>("""{"id":1,"extra":true}""", typeOf<Item>())
        }
        assertFalse(serializer.serialize(Item(1), typeOf<Item>()).contains("label"))
    }

    @Test
    fun `invalid or incomplete data propagates errors`() {
        for (text in listOf("not JSON", "{}", """{"id":"wrong"}""", "[]", "")) {
            assertThrows(SerializationException::class.java) { serializer.deserialize<Item>(text, typeOf<Item>()) }
        }
    }

    @Test
    fun `missing required serializer fails in both directions`() {
        assertThrows(SerializationException::class.java) { serializer.serialize(ExternalId("123"), typeOf<ExternalId>()) }
        assertThrows(SerializationException::class.java) { serializer.deserialize<ExternalId>("\"123\"", typeOf<ExternalId>()) }
        assertThrows(SerializationException::class.java) {
            serializer.serialize(WithExternalId(ExternalId("123")), typeOf<WithExternalId>())
        }
        assertThrows(SerializationException::class.java) {
            serializer.deserialize<WithExternalId>("""{"id":"123"}""", typeOf<WithExternalId>())
        }
    }

    @Test
    fun `custom serializer failures propagate without wrapping`() {
        val failure = IllegalArgumentException("custom serializer failed")
        val failing = object : KSerializer<ExternalId> {
            override val descriptor = ExternalIdSerializer.descriptor
            override fun serialize(encoder: Encoder, value: ExternalId): Unit = throw failure
            override fun deserialize(decoder: Decoder): ExternalId = throw failure
        }
        val adapter = KotlinxSerializer(Json { serializersModule = SerializersModule { contextual(failing) } })
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { adapter.serialize(ExternalId("123"), typeOf<ExternalId>()) })
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { adapter.deserialize<ExternalId>("\"123\"", typeOf<ExternalId>()) })
    }
}
