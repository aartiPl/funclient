package net.igsoft.funclient.model

import kotlin.reflect.KType

/** [type] is the declared type of [value], not its runtime class. */
data class ObjectBody<T>(val value: T, val type: KType) : Body
