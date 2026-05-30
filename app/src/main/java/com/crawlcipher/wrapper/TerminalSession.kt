package com.crawlcipher.wrapper

class TerminalSession(private val runtime: GameRuntime) {
    private val lines = mutableListOf<String>()

    fun start(): String {
        lines.clear()
        lines.addAll(runtime.boot())
        return lines.joinToString(separator = "\n")
    }

    fun submit(rawCommand: String): String {
        val command = rawCommand.trim()
        if (command.isEmpty()) {
            return lines.joinToString(separator = "\n")
        }

        lines.add("> $command")
        val result = runtime.onCommand(command)
        if (result.size == 1 && result[0] == "__CLEAR__") {
            lines.clear()
        } else {
            lines.addAll(result)
        }
        return lines.joinToString(separator = "\n")
    }
}
