package net.igsoft.funclient.model

data class Exchange(
    val request: Request,
    val responses: List<Response>


) {
    fun last() = responses.last()
}
