package net.igsoft.funclient.model

import java.io.File
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Body {
    companion object {
        fun empty(): EmptyBody = EmptyBody

        fun text(value: String): StringBody = StringBody(value)

        fun file(file: File): FileBody = FileBody(file)

        /** Captures the full declared [T]; serialization occurs when the request is executed. */
        inline fun <reified T> serialized(value: T): ObjectBody<T> =
            serialized(value, typeOf<T>())

        /** [type] must describe [value], including generic arguments and nullability. */
        fun <T> serialized(value: T, type: KType): ObjectBody<T> = ObjectBody(value, type)
    }
}

object EmptyBody : Body
