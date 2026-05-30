package com.crawlcipher.wrapper

object PtyBridge {
    init {
        System.loadLibrary("crawlcipher_pty")
    }

    external fun nativeStart(
        executablePath: String,
        args: Array<String>,
        workingDirectory: String,
        rows: Int,
        cols: Int
    ): Long

    external fun nativeRead(handle: Long, buffer: ByteArray, offset: Int, length: Int): Int
    external fun nativeWrite(handle: Long, data: ByteArray, offset: Int, length: Int): Int
    external fun nativeResize(handle: Long, rows: Int, cols: Int)
    external fun nativeWait(handle: Long): Int
    external fun nativeStop(handle: Long)
    external fun nativeClose(handle: Long)
}
