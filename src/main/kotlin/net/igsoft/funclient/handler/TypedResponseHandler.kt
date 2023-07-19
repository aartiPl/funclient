package net.igsoft.funclient.handler

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.igsoft.funclient.model.*

class TypedResponseHandler<T>(private val typeToken: TypeToken<T>) : ResponseHandler<TypedResponse<T>> {
    override fun handle(request: Request, response: Response, gson: Gson): TypedResponse<T> {
        return when (response.status) {
            in 200..299 -> {
                val responseBody = (response.body as StringBody).string
                return TypedResponse(
                    response.status,
                    response.headers,
                    ObjectBody(gson.fromJson(responseBody, typeToken))
                )
            }

            else -> throw IllegalStateException("Wrong response http status. ${response.status}")
        }
    }
}
