package net.igsoft.funclient

import kotlin.reflect.KType

/** Converts values using their full declared type, including type arguments and nullability. */
interface Serializer {
    fun serialize(value: Any?, type: KType): String

    fun <T> deserialize(text: String, type: KType): T
}
