package net.igsoft.funclient.model

import java.io.File

/**
 * A streamed byte range of [file]. [offset] is zero-based; null [length] means
 * the remaining bytes, measured when the request is executed.
 *
 * The file must remain unchanged while sending. No snapshot of its contents is made.
 * Numeric arguments are checked on construction; file bounds are checked on execution.
 */
data class FileBody @JvmOverloads constructor(
    val file: File,
    val offset: Long = 0,
    val length: Long? = null,
) : Body {
    init {
        require(offset >= 0) { "File offset must be non-negative: $offset" }
        require(length == null || length >= 0) { "File length must be non-negative: $length" }
        require(length == null || length <= Long.MAX_VALUE - offset) { "File range overflows Long: offset=$offset, length=$length" }
    }
}
