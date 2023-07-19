package net.igsoft.funclient

//import java.net.http.HttpClient
//import java.net.http.HttpRequest
//import java.net.http.HttpResponse

//NOTE: Doesn't work with JDK8
//class ApiClient_Jdk11 {
//    private val client = HttpClient.newHttpClient()

//    fun execute(apiRequest: ApiRequest): ApiResponse {
//        val httpRequestBuilder = HttpRequest.newBuilder()
//
//        when (apiRequest.method) {
//            ApiMethod.GET -> httpRequestBuilder.GET()
//            ApiMethod.POST -> httpRequestBuilder.POST(resolveBody(apiRequest))
//            ApiMethod.PUT -> httpRequestBuilder.PUT(resolveBody(apiRequest))
//            ApiMethod.DELETE -> httpRequestBuilder.DELETE()
//            ApiMethod.HEAD -> TODO()
//            ApiMethod.OPTIONS -> TODO()
//        }
//
//        httpRequestBuilder.uri(apiRequest.uri)
//
//        for (header in apiRequest.headers.all.entries) {
//            for (value in header.value) {
//                httpRequestBuilder.header(header.key, value)
//            }
//        }
//
//        val response = client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())
//
//        return ApiResponse(ApiHeaders(response.headers().map()), StringApiBody(response.body()))
//    }
//
//    private fun resolveBody(apiRequest: ApiRequest) = when (apiRequest.body) {
//        is StringApiBody -> HttpRequest.BodyPublishers.ofString(apiRequest.body.body)
//        is EmptyBody -> HttpRequest.BodyPublishers.ofString("")
//        else -> TODO()
//    }
//}
