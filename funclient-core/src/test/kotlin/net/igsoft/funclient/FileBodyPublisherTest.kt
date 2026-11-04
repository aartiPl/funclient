package net.igsoft.funclient

import net.igsoft.funclient.model.FileBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import java.util.concurrent.Flow

class FileBodyPublisherTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `constructor and copy reject negative values and overflowing ranges`() {
        val file = directory.resolve("missing").toFile()
        for ((offset, length) in listOf(-1L to null, -1L to 0L, 0L to -1L, Long.MAX_VALUE to 1L, 1L to Long.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { FileBody(file, offset, length) }
            assertThrows(IllegalArgumentException::class.java) { FileBody(file).copy(offset = offset, length = length) }
        }
        assertEquals(Long.MAX_VALUE, FileBody(file, Long.MAX_VALUE, 0).offset)
        assertEquals(Long.MAX_VALUE, FileBody(file, length = Long.MAX_VALUE).length)
        assertEquals(FileBody(file), FileBody::class.java.getConstructor(java.io.File::class.java).newInstance(file))
    }

    @Test
    fun `range stream closes channel at exact end and preserves unsigned single bytes`() {
        val path = directory.resolve("upload.bin").apply { toFile().writeBytes(byteArrayOf(1, -1, 3, 4, 5)) }
        val channel = FileChannel.open(path, READ).position(1)
        val stream = FileRangeInputStream(channel, 2)
        try {
            assertEquals(255, stream.read())
            assertTrue(channel.isOpen)
            val buffer = ByteArray(8)
            assertEquals(1, stream.read(buffer, 2, 5))
            assertEquals(3, buffer[2].toInt())
            assertFalse(channel.isOpen)
            assertEquals(-1, stream.read())
            assertEquals(0, stream.read(buffer, buffer.size, 0))
        } finally {
            stream.close()
        }
    }

    @Test
    fun `range stream closes channel on premature EOF`() {
        val path = directory.resolve("upload.bin").apply { toFile().writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val channel = FileChannel.open(path, READ)
        val stream = FileRangeInputStream(channel, 4)
        try {
            assertEquals(1, stream.read())
            FileChannel.open(path, WRITE).use { it.truncate(1) }
            assertThrows(EOFException::class.java) { stream.read(ByteArray(8)) }
            assertFalse(channel.isOpen)
        } finally {
            stream.close()
        }
    }

    @Test
    fun `publisher closes streams after completion and opens new stream on each subscription`() {
        val bytes = byteArrayOf(0, -1, 5, 10)
        val opened = mutableListOf<TrackedStream>()
        val publisher = FileBodyPublisher(bytes.size.toLong()) {
            TrackedStream(bytes).also { opened.add(it) }
        }
        repeat(2) {
            val subscriber = Subscriber()
            publisher.subscribe(subscriber)
            subscriber.subscription.request(Long.MAX_VALUE)
            assertArrayEquals(bytes, subscriber.bytes.toByteArray())
            assertTrue(subscriber.complete)
            assertNull(subscriber.failure)
            assertTrue(opened.last().closed)
        }
        assertEquals(2, opened.size)
    }

    @Test
    fun `publisher closes stream on cancellation before demand and after partial demand`() {
        for (requestFirst in listOf(false, true)) {
            val stream = TrackedStream(ByteArray(256 * 1024))
            val subscriber = Subscriber()
            FileBodyPublisher(256L * 1024) { stream }.subscribe(subscriber)
            if (requestFirst) subscriber.subscription.request(1)
            assertFalse(stream.closed)
            subscriber.subscription.cancel()
            assertTrue(stream.closed)
            assertFalse(subscriber.complete)
            assertNull(subscriber.failure)
        }
    }

    @Test
    fun `publisher closes stream when subscriber cancels from onNext`() {
        val path = directory.resolve("cancel.bin").apply { toFile().writeBytes(ByteArray(256 * 1024)) }
        val channel = FileChannel.open(path, READ)
        val stream = FileRangeInputStream(channel, 256L * 1024)
        val subscriber = Subscriber(cancelOnNext = true)
        FileBodyPublisher(256L * 1024) { stream }.subscribe(subscriber)
        try {
            subscriber.subscription.request(1)
            assertFalse(channel.isOpen)
            assertFalse(subscriber.complete)
            assertNull(subscriber.failure)
        } finally {
            stream.close()
        }
    }

    @Test
    fun `publisher signals opening failures and closes on read failures`() {
        val openFailure = IOException("cannot open")
        val unopened = Subscriber()
        FileBodyPublisher(1) { throw openFailure }.subscribe(unopened)
        assertSame(openFailure, unopened.failure)
        assertFalse(unopened.complete)

        val readFailure = IOException("cannot read")
        val stream = object : TrackedStream(byteArrayOf(1)) {
            override fun read(bytes: ByteArray, off: Int, len: Int): Int = throw readFailure
        }
        val subscriber = Subscriber()
        FileBodyPublisher(1) { stream }.subscribe(subscriber)
        subscriber.subscription.request(1)
        assertTrue(stream.closed)
        assertTrue(generateSequence(subscriber.failure) { it.cause }.any { it === readFailure })
        assertFalse(subscriber.complete)
    }

    @Test
    fun `publisher rejects file shortened between validation and subscription`() {
        val file = directory.resolve("upload.bin").toFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val publisher = FileBodyPublisher.create(FileBody(file, 2))
        file.writeBytes(byteArrayOf(1, 2, 3))
        val subscriber = Subscriber()
        publisher.subscribe(subscriber)
        assertInstanceOf(EOFException::class.java, subscriber.failure)
        assertFalse(subscriber.complete)
        assertEquals(2, publisher.contentLength())
    }

    @Test
    fun `publisher closes stream if subscriber callback fails`() {
        val stream = TrackedStream(ByteArray(256 * 1024))
        val failure = IllegalStateException("subscriber failed")
        val subscriber = object : Flow.Subscriber<ByteBuffer> {
            lateinit var subscription: Flow.Subscription
            override fun onSubscribe(subscription: Flow.Subscription) {
                this.subscription = subscription
            }
            override fun onNext(item: ByteBuffer) = throw failure
            override fun onComplete() = fail<Unit>("Unexpected completion")
            override fun onError(throwable: Throwable) = fail<Unit>("Unexpected error", throwable)
        }
        FileBodyPublisher(256L * 1024) { stream }.subscribe(subscriber)
        assertSame(failure, assertThrows(IllegalStateException::class.java) { subscriber.subscription.request(1) })
        assertTrue(stream.closed)
    }

    private open class TrackedStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false
        override fun close() {
            closed = true
            super.close()
        }
    }

    private class Subscriber(private val cancelOnNext: Boolean = false) : Flow.Subscriber<ByteBuffer> {
        lateinit var subscription: Flow.Subscription
        val bytes = ByteArrayOutputStream()
        var failure: Throwable? = null
        var complete = false

        override fun onSubscribe(subscription: Flow.Subscription) {
            this.subscription = subscription
        }

        override fun onNext(item: ByteBuffer) {
            val chunk = ByteArray(item.remaining())
            item.get(chunk)
            bytes.write(chunk)
            if (cancelOnNext) subscription.cancel()
        }

        override fun onError(throwable: Throwable) {
            failure = throwable
        }

        override fun onComplete() {
            complete = true
        }
    }
}
