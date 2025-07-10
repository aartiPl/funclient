package net.igsoft.funclient

import com.google.gson.Gson
import net.igsoft.funclient.model.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.io.path.Path

//NOTE: Doesn't work with JDK8
@Suppress("unused")
class FunClient(val gson: Gson) {
    private val client = HttpClient.newHttpClient()

    fun execute(request: Request): Response {
        val httpRequestBuilder = HttpRequest.newBuilder()

        when (request.method) {
            Method.GET -> httpRequestBuilder.GET()
            Method.POST -> httpRequestBuilder.POST(resolveBody(request))
            Method.PUT -> httpRequestBuilder.PUT(resolveBody(request))
            Method.DELETE -> httpRequestBuilder.DELETE()
            Method.PATCH -> httpRequestBuilder.method("PATCH", resolveBody(request))
            Method.HEAD -> httpRequestBuilder.method("HEAD", resolveBody(request))
            Method.OPTIONS -> httpRequestBuilder.method("OPTIONS", resolveBody(request))
        }

        httpRequestBuilder.uri(request.url.toURI())

        for (header in request.headers.all.entries) {
            for (value in header.value) {
                httpRequestBuilder.header(header.key, value)
            }
        }

        val response = client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        return RawResponse(response.statusCode(), Headers(response.headers().map()), StringBody(response.body()))
    }

    private fun resolveBody(request: Request): HttpRequest.BodyPublisher = when (request.body) {
        is StringBody -> HttpRequest.BodyPublishers.ofString(request.body.string)
        is EmptyBody -> HttpRequest.BodyPublishers.noBody()
        is FileBody -> HttpRequest.BodyPublishers.ofFile(Path(request.body.file.path))
        is ObjectBody<*> -> HttpRequest.BodyPublishers.ofString(gson.toJson(request.body.value))
        else -> throw IllegalStateException("Unknown body type: ${request.body::class.simpleName}")
    }
}
