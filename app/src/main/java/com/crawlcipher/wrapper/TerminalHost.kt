package com.crawlcipher.wrapper

import android.content.Context
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class TerminalHost(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onOutput(text: String)
        fun onStatus(text: String)
    }

    private val binaryProvider = RuntimeBinaryProvider(context)
    private val debugLog = RuntimeDebugLog()
    private val lock = Any()
    private var session: PtySession? = null

    fun start() {
        synchronized(lock) {
            if (session != null) {
                return
            }
        }

        val launch = binaryProvider.prepareLaunchCommand()
        if (launch is RuntimeBinaryProvider.LaunchPreparation.Error) {
            listener.onStatus(launch.message)
            debugLog.add("error", launch.message)
            return
        }
        val config = (launch as RuntimeBinaryProvider.LaunchPreparation.Success).config

        val launchedSession = try {
            PtySession.start(
                executablePath = config.executable.absolutePath,
                args = config.arguments,
                workingDirectory = binaryProvider.runtimeDirectory.absolutePath
            )
        } catch (error: Exception) {
            val message = "Failed to launch PTY runtime: ${error.message}"
            listener.onStatus(message)
            debugLog.add("error", message)
            return
        }

        synchronized(lock) {
            session = launchedSession
        }

        listener.onStatus("Runtime started: ${config.executable.name}")
        debugLog.add("runtime", "Started ${config.executable.absolutePath} ${config.arguments.joinToString(" ")}")

        thread(name = "runtime-output-reader", isDaemon = true) {
            readOutput(launchedSession)
        }
        thread(name = "runtime-exit-waiter", isDaemon = true) {
            val exitCode = launchedSession.waitFor()
            debugLog.add("exit", "Runtime exited with code $exitCode")
            listener.onStatus("Runtime exited ($exitCode)")
            synchronized(lock) {
                if (session == launchedSession) {
                    session = null
                }
            }
            launchedSession.close()
        }
    }

    fun send(text: String) {
        send(text.toByteArray(StandardCharsets.UTF_8))
    }

    fun send(bytes: ByteArray) {
        val activeSession = synchronized(lock) { session } ?: return
        val writeResult = activeSession.write(bytes)
        if (writeResult < 0) {
            listener.onStatus("Failed to send input to runtime")
            debugLog.add("error", "Input write failed")
        }
    }

    fun resize(rows: Int, cols: Int) {
        val activeSession = synchronized(lock) { session } ?: return
        activeSession.resize(rows, cols)
    }

    fun dumpDebugLog(): String = debugLog.dump()

    fun stop() {
        val activeSession = synchronized(lock) {
            val current = session
            session = null
            current
        } ?: return

        activeSession.stop()
        activeSession.close()
        debugLog.add("runtime", "Runtime stop requested")
    }

    private fun readOutput(activeSession: PtySession) {
        val buffer = ByteArray(4096)
        while (true) {
            val readCount = activeSession.read(buffer)
            if (readCount < 0) {
                return
            }
            if (readCount > 0) {
                val outputChunk = String(buffer, 0, readCount, StandardCharsets.UTF_8)
                listener.onOutput(outputChunk)
                val normalized = if (outputChunk.length > 300) {
                    outputChunk.take(300) + "...(truncated)"
                } else {
                    outputChunk
                }
                debugLog.add("stdout", normalized)
            }
        }
    }
}
