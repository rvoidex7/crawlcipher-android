package com.crawlcipher.wrapper

import android.content.Context
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TerminalHost(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onOutput(text: String)
    }

    private val binaryProvider = RuntimeBinaryProvider(context)
    private val lock = Any()
    private var process: Process? = null

    fun start() {
        synchronized(lock) {
            if (process != null) {
                return
            }
        }

        val command = binaryProvider.prepareLaunchCommand()
        if (command == null) {
            listener.onOutput(
                "No runtime binary found.\n" +
                    "Place an executable in app assets/runtime or /data/data/<package>/no_backup/runtime.\n"
            )
            return
        }

        val launched = try {
            ProcessBuilder(command)
                .directory(binaryProvider.runtimeDirectory)
                .redirectErrorStream(true)
                .start()
        } catch (error: IOException) {
            listener.onOutput("Failed to launch runtime: ${error.message}\n")
            return
        }

        synchronized(lock) {
            process = launched
        }

        listener.onOutput("Runtime started: ${command.first()}\n")

        thread(name = "runtime-output-reader", isDaemon = true) {
            readOutput(launched)
        }
        thread(name = "runtime-exit-waiter", isDaemon = true) {
            val exitCode = launched.waitFor()
            listener.onOutput("\n[Runtime exited with code $exitCode]\n")
            synchronized(lock) {
                if (process == launched) {
                    process = null
                }
            }
        }
    }

    fun send(text: String) {
        val activeProcess = synchronized(lock) { process } ?: return
        try {
            activeProcess.outputStream.write(text.toByteArray(StandardCharsets.UTF_8))
            activeProcess.outputStream.flush()
        } catch (_: IOException) {
            listener.onOutput("\n[Failed to send input to runtime]\n")
        }
    }

    fun stop() {
        val activeProcess = synchronized(lock) {
            val current = process
            process = null
            current
        } ?: return

        activeProcess.destroy()
        if (!activeProcess.waitFor(300, TimeUnit.MILLISECONDS)) {
            activeProcess.destroyForcibly()
        }
    }

    private fun readOutput(activeProcess: Process) {
        val buffer = ByteArray(4096)
        try {
            val stream = activeProcess.inputStream
            while (true) {
                val readCount = stream.read(buffer)
                if (readCount < 0) {
                    return
                }
                if (readCount > 0) {
                    listener.onOutput(String(buffer, 0, readCount, StandardCharsets.UTF_8))
                }
            }
        } catch (_: IOException) {
            // Process has likely stopped.
        }
    }
}
