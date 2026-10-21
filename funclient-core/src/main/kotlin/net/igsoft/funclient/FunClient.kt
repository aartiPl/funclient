package net.igsoft.funclient

import net.igsoft.funclient.model.EmptyBody
import net.igsoft.funclient.model.FileBody
import net.igsoft.funclient.model.Headers
import net.igsoft.funclient.model.Method
import net.igsoft.funclient.model.ObjectBody
import net.igsoft.funclient.model.RawResponse
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response
import net.igsoft.funclient.model.StringBody
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.io.path.Path

//NOTE: Doesn't work with JDK8
@Suppress("unused")
class FunClient(private val serializer: Serializer, private val strict: Boolean = true) {
    private val client = HttpClient.newHttpClient()

    fun execute(request: Request): Response {
        require(!strict || (request.method != Method.GET && request.method != Method.DELETE) || request.body === EmptyBody) {
            "Request body is not allowed for ${request.method} in strict mode."
        }
        val httpRequestBuilder = HttpRequest.newBuilder()

        when (request.method) {
            Method.GET -> httpRequestBuilder.method("GET", resolveBody(request))
            Method.POST -> httpRequestBuilder.POST(resolveBody(request))
            Method.PUT -> httpRequestBuilder.PUT(resolveBody(request))
            Method.DELETE -> httpRequestBuilder.method("DELETE", resolveBody(request))
            Method.PATCH -> httpRequestBuilder.method("PATCH", resolveBody(request))
            Method.HEAD -> httpRequestBuilder.method("HEAD", resolveBody(request))
            Method.OPTIONS -> httpRequestBuilder.method("OPTIONS", resolveBody(request))
        }

        httpRequestBuilder.uri(request.url.toURI())

        for ((name, values) in request.headers.all) {
            for (value in values) {
                httpRequestBuilder.header(name, value)
            }
        }

        val response = client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        return RawResponse(response.statusCode(), Headers(response.headers().map()), StringBody(response.body()))
    }

    private fun resolveBody(request: Request): HttpRequest.BodyPublisher = when (request.body) {
        is StringBody -> HttpRequest.BodyPublishers.ofString(request.body.string)
        is EmptyBody -> HttpRequest.BodyPublishers.noBody()
        is FileBody -> HttpRequest.BodyPublishers.ofFile(Path(request.body.file.path))
        is ObjectBody<*> -> HttpRequest.BodyPublishers.ofString(serializer.serialize(request.body.value, request.body.type))
        else -> throw IllegalStateException("Unknown body type: ${request.body::class.simpleName}")
    }
}
