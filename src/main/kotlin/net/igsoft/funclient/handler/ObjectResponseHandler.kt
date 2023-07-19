package net.igsoft.funclient.handler

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response
import net.igsoft.funclient.model.StringBody

class ObjectResponseHandler<T>(private val typeToken: TypeToken<T>) : ResponseHandler<T> {
    override fun handle(request: Request, response: Response, gson: Gson): T {
        return when (response.status) {
            in 200..299 -> {
                val responseBody = (response.body as StringBody).string
                gson.fromJson(responseBody, typeToken)
            }

            else -> throw IllegalStateException("Wrong response http status. ${response.status}")
        }
    }
}
