package net.igsoft.funclient.model

import java.io.File

/** Receive a successful response into [file]. The caller owns exclusive access to this destination. */
data class FileResponseBodyTarget(
    val file: File,
    val overwrite: Boolean = false,
) : ResponseBodyTarget
