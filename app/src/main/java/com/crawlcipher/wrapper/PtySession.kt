package com.crawlcipher.wrapper

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class PtySession private constructor(handle: Long) {
    private val handleRef = AtomicLong(handle)

    fun read(buffer: ByteArray): Int {
        val handle = handleRef.get()
        if (handle == 0L) return -1
        return PtyBridge.nativeRead(handle, buffer, 0, buffer.size)
    }

    fun write(data: ByteArray): Int {
        val handle = handleRef.get()
        if (handle == 0L) return -1
        return PtyBridge.nativeWrite(handle, data, 0, data.size)
    }

    fun resize(rows: Int, cols: Int) {
        val handle = handleRef.get()
        if (handle == 0L) return
        PtyBridge.nativeResize(handle, rows, cols)
    }

    fun waitFor(): Int {
        val handle = handleRef.get()
        if (handle == 0L) return -1
        return PtyBridge.nativeWait(handle)
    }

    fun stop() {
        val handle = handleRef.get()
        if (handle == 0L) return
        PtyBridge.nativeStop(handle)
    }

    fun close() {
        val handle = handleRef.getAndSet(0L)
        if (handle != 0L) {
            PtyBridge.nativeClose(handle)
        }
    }

    companion object {
        fun start(
            executablePath: String,
            args: List<String>,
            workingDirectory: String,
            rows: Int = 24,
            cols: Int = 80
        ): PtySession {
            val handle = PtyBridge.nativeStart(
                executablePath = executablePath,
                args = args.toTypedArray(),
                workingDirectory = workingDirectory,
                rows = rows,
                cols = cols
            )
            if (handle == 0L) {
                throw IOException("Failed to allocate PTY runtime session")
            }
            return PtySession(handle)
        }
    }
}
