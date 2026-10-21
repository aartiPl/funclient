package net.igsoft.funclient.model

class HeadersBuilder {
    private val headers: MutableMap<String, MutableList<String>> = mutableMapOf()

    fun append(name: String, value: String) = apply {
        val currentValue = headers.getOrPut(name) { mutableListOf() }
        currentValue.add(value)
    }

    fun append(name: String, value: String, onlyIf: Boolean) = apply {
        if (onlyIf) {
            append(name, value)
        }
    }

    fun build(): Headers = Headers(headers)
}
