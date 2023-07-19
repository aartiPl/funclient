package net.igsoft.funclient

import com.google.gson.Gson
import kong.unirest.Unirest
import net.igsoft.funclient.handler.ResponseHandler
import net.igsoft.funclient.model.*

//TODO: create common content types
class FunClient(val gson: Gson) {
    init {
        //socket timeout - time to receive the data
        // 300_000 == 5m
        Unirest.config().reset().socketTimeout(300_000).connectTimeout(2_000)
    }

    fun <T> execute(request: Request, responseHandler: ResponseHandler<T>): T {
        return responseHandler.handle(request, execute(request), gson)
    }

    fun execute(request: Request): Response {
        val uniRequest = Unirest.request(request.method.name, request.url.toString())

        for (header in request.headers.all.entries) {
            for (value in header.value) {
                uniRequest.header(header.key, value)
            }
        }

        val finalRequest = when (request.body) {
            is StringBody -> uniRequest.body(request.body.string)
            is FileBody -> uniRequest.body(request.body.file.readBytes())
            is ObjectBody<*> -> uniRequest.body(gson.toJson(request.body.value))
            is EmptyBody -> uniRequest
            else -> throw IllegalStateException("Unknown body type: ${request.body::class.simpleName}")
        }

        val response = finalRequest.asString()

        val headersBuilder = Headers.builder()
        response.headers.all().forEach { headersBuilder.append(it.name, it.value) }

        return RawResponse(response.status, headersBuilder.build(), StringBody(response.body))
    }
}
