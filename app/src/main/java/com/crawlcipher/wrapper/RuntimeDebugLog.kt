package com.crawlcipher.wrapper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RuntimeDebugLog(private val maxEntries: Int = 400) {
    private val lock = Any()
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val lines = ArrayDeque<String>()

    fun add(tag: String, message: String) {
        val timestamp = formatter.format(Date())
        synchronized(lock) {
            lines.addLast("[$timestamp][$tag] $message")
            while (lines.size > maxEntries) {
                lines.removeFirst()
            }
        }
    }

    fun dump(): String = synchronized(lock) {
        lines.joinToString(separator = "\n")
    }
}
