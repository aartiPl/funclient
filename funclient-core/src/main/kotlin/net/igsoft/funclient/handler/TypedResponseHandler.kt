package net.igsoft.funclient.handler

import net.igsoft.funclient.Serializer
import net.igsoft.funclient.model.ObjectBody
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response
import net.igsoft.funclient.model.StringBody
import net.igsoft.funclient.model.TypedResponse
import kotlin.reflect.KType

class TypedResponseHandler<T>(private val type: KType) : ResponseHandler<TypedResponse<T>> {
    override fun handle(request: Request, response: Response, serializer: Serializer): TypedResponse<T> {
        return when (response.status) {
            in 200..299 -> {
                val responseBody = (response.body as StringBody).string
                TypedResponse(
                    response.status,
                    response.headers,
                    ObjectBody(serializer.deserialize(responseBody, type), type)
                )
            }

            else -> throw IllegalStateException("Wrong response http status. ${response.status}")
        }
    }
}
