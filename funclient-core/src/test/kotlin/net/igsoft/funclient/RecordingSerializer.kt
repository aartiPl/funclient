package net.igsoft.funclient

import kotlin.reflect.KType

/** A transport test double: deliberately contains no serialization library. */
internal class RecordingSerializer : Serializer {
    val serialized = mutableListOf<Pair<Any?, KType>>()
    val deserialized = mutableListOf<Pair<String, KType>>()
    var encoded = "encoded by test serializer"
    var decoded: Any? = null
    var failure: RuntimeException? = null

    override fun serialize(value: Any?, type: KType): String {
        serialized += value to type
        failure?.let { throw it }
        return encoded
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(text: String, type: KType): T {
        deserialized += text to type
        failure?.let { throw it }
        return decoded as T
    }
}
