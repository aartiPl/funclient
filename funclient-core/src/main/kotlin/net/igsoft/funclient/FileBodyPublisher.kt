package net.igsoft.funclient

import net.igsoft.funclient.model.FileBody
import java.io.EOFException
import java.io.InputStream
import java.net.http.HttpRequest
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.nio.file.attribute.BasicFileAttributes
import java.util.Objects
import java.util.concurrent.Flow

/** Delegates demand and buffering to the JDK, but owns the stream's lifetime. */
internal class FileBodyPublisher(
    private val length: Long,
    private val openStream: () -> InputStream,
) : HttpRequest.BodyPublisher {
    override fun contentLength(): Long = length

    override fun subscribe(subscriber: Flow.Subscriber<in ByteBuffer>) {
        val stream = try {
            openStream()
        } catch (failure: Exception) {
            subscriber.onSubscribe(object : Flow.Subscription {
                override fun request(n: Long) = Unit
                override fun cancel() = Unit
            })
            subscriber.onError(failure)
            return
        }

        try {
            HttpRequest.BodyPublishers.ofInputStream { stream }.subscribe(object : Flow.Subscriber<ByteBuffer> {
                override fun onSubscribe(subscription: Flow.Subscription) {
                    subscriber.onSubscribe(object : Flow.Subscription {
                        override fun request(n: Long) = subscription.request(n)

                        override fun cancel() {
                            try {
                                subscription.cancel()
                            } finally {
                                // Cancellation cannot signal another terminal event to the subscriber.
                                runCatching { stream.close() }
                            }
                        }
                    })
                }

                override fun onNext(item: ByteBuffer) {
                    try {
                        subscriber.onNext(item)
                    } catch (failure: Throwable) {
                        stream.closeAfterFailure(failure)
                        throw failure
                    }
                }

                override fun onError(throwable: Throwable) {
                    stream.closeAfterFailure(throwable)
                    subscriber.onError(throwable)
                }

                override fun onComplete() {
                    try {
                        stream.close()
                    } catch (failure: Exception) {
                        subscriber.onError(failure)
                        return
                    }
                    subscriber.onComplete()
                }
            })
        } catch (failure: Throwable) {
            stream.closeAfterFailure(failure)
            throw failure
        }
    }

    companion object {
        fun create(body: FileBody): HttpRequest.BodyPublisher {
            val path = body.file.toPath()
            require(Files.readAttributes(path, BasicFileAttributes::class.java).isRegularFile) {
                "Request body must be a regular file: $path"
            }
            val length = FileChannel.open(path, READ).use { channel ->
                val size = channel.size()
                require(body.offset <= size) { "File offset ${body.offset} exceeds file size $size" }
                val remaining = size - body.offset
                require(body.length == null || body.length <= remaining) {
                    "File length ${body.length} exceeds remaining size $remaining"
                }
                body.length ?: remaining
            }
            // JDK may never subscribe to zero-length publishers. Validation above still applies.
            if (length == 0L) return HttpRequest.BodyPublishers.noBody()
            return FileBodyPublisher(length) { FileRangeInputStream.open(path, body.offset, length) }
        }
    }
}

/** Seeks once, then reads at most the selected number of bytes without buffering the file. */
internal class FileRangeInputStream(
    private val channel: FileChannel,
    private var remaining: Long,
) : InputStream() {
    @Volatile
    private var closed = false

    override fun read(): Int {
        val byte = ByteArray(1)
        return if (read(byte, 0, 1) == -1) -1 else byte[0].toInt() and 0xff
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        Objects.checkFromIndexSize(off, len, bytes.size)
        if (len == 0) return 0
        // The JDK iterator may read ahead after its subscriber cancels from onNext.
        if (remaining == 0L || closed) return -1
        try {
            val count = channel.read(ByteBuffer.wrap(bytes, off, minOf(len.toLong(), remaining).toInt()))
            if (count == -1) throw EOFException("File ended with $remaining bytes left in the request body")
            remaining -= count
            if (remaining == 0L) close()
            return count
        } catch (failure: Throwable) {
            val cancelledDuringRead = closed && failure is ClosedChannelException
            closeAfterFailure(failure)
            if (cancelledDuringRead) return -1
            throw failure
        }
    }

    override fun close() {
        closed = true
        channel.close()
    }

    companion object {
        fun open(path: Path, offset: Long, length: Long): FileRangeInputStream {
            val channel = FileChannel.open(path, READ)
            try {
                if (channel.size() < offset + length) {
                    throw EOFException("File was shortened before reading range offset=$offset, length=$length")
                }
                channel.position(offset)
                return FileRangeInputStream(channel, length)
            } catch (failure: Throwable) {
                channel.closeAfterFailure(failure)
                throw failure
            }
        }
    }
}

private fun AutoCloseable.closeAfterFailure(failure: Throwable) {
    try {
        close()
    } catch (closeFailure: Throwable) {
        if (closeFailure !== failure) failure.addSuppressed(closeFailure)
    }
}
