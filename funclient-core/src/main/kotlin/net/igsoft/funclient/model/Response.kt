package net.igsoft.funclient.model

interface Response {
    val status: Int
    val headers: Headers
    val body: Body
}

data class RawResponse(override val status: Int, override val headers: Headers, override val body: Body) : Response

data class TypedResponse<T>(override val status: Int, override val headers: Headers, override val body: ObjectBody<T>) :
    Response
