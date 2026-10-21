package net.igsoft.funclient.handler

import net.igsoft.funclient.Serializer
import kotlin.reflect.KType
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response
import net.igsoft.funclient.model.StringBody

class ObjectResponseHandler<T>(private val type: KType) : ResponseHandler<T> {
    override fun handle(request: Request, response: Response, serializer: Serializer): T {
        return when (response.status) {
            in 200..299 -> {
                val responseBody = (response.body as StringBody).string
                serializer.deserialize(responseBody, type)
            }

            else -> throw IllegalStateException("Wrong response http status. ${response.status}")
        }
    }
}
