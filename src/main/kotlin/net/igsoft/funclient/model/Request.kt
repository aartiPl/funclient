package net.igsoft.funclient.model

import java.net.URL

data class Request(
    val method: Method,
    val url: URL,
    val headers: Headers = EmptyHeaders,
    val body: Body = EmptyBody
)
