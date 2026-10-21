package net.igsoft.funclient.serializer.kotlinx

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.igsoft.funclient.Serializer
import kotlin.reflect.KType

/** Uses the supplied JSON configuration and module for both serializer lookup and conversion. */
class KotlinxSerializer(private val json: Json) : Serializer {
    override fun serialize(value: Any?, type: KType): String =
        json.encodeToString(json.serializersModule.serializer(type), value)

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(text: String, type: KType): T =
        json.decodeFromString(json.serializersModule.serializer(type), text) as T
}
