package net.igsoft.funclient

import net.igsoft.funclient.model.EmptyBody
import net.igsoft.funclient.model.FileBody
import net.igsoft.funclient.model.FileResponseBodyTarget
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
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

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

        val target = request.responseBodyTarget as? FileResponseBodyTarget
        if (target != null) return receiveFile(httpRequestBuilder.build(), target)
        val response = client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        return RawResponse(response.statusCode(), Headers(response.headers().map()), StringBody(response.body()))
    }

    private fun receiveFile(request: HttpRequest, target: FileResponseBodyTarget): Response {
        val file = target.file.toPath().toAbsolutePath().normalize()
        require(Files.isDirectory(file.parent)) { "Response destination parent must exist: ${file.parent}" }
        require(!Files.isSymbolicLink(file) && (!Files.exists(file, LinkOption.NOFOLLOW_LINKS) ||
            target.overwrite && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))) {
            "Response destination exists or is not a regular file: $file"
        }
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val headers = Headers(response.headers().map())
        if (response.statusCode() !in 200..299) {
            val text = response.body().use { it.bufferedReader(Charsets.UTF_8).readText() }
            return RawResponse(response.statusCode(), headers, StringBody(text))
        }
        val temporary = try { Files.createTempFile(file.parent, ".funclient-response-", ".tmp") }
        catch (failure: Exception) {
            try { response.body().close() } catch (cleanup: Exception) { failure.addSuppressed(cleanup) }
            throw failure
        }
        try {
            val count = response.body().use { input -> Files.newOutputStream(temporary).use { output -> input.copyTo(output) } }
            if (target.overwrite) Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            else Files.move(temporary, file)
            return RawResponse(response.statusCode(), headers, FileBody(file.toFile(), length = count))
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun resolveBody(request: Request): HttpRequest.BodyPublisher = when (request.body) {
        is StringBody -> HttpRequest.BodyPublishers.ofString(request.body.string)
        is EmptyBody -> HttpRequest.BodyPublishers.noBody()
        is FileBody -> FileBodyPublisher.create(request.body)
        is ObjectBody<*> -> HttpRequest.BodyPublishers.ofString(serializer.serialize(request.body.value, request.body.type))
        else -> throw IllegalStateException("Unknown body type: ${request.body::class.simpleName}")
    }
}
