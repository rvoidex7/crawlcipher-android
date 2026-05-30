package com.crawlcipher.wrapper

class PlaceholderRuntime : GameRuntime {
    override fun boot(): List<String> = listOf(
        "CrawlCipher Android wrapper started.",
        "Bundled runtime is not attached yet.",
        "Type 'help' for wrapper commands."
    )

    override fun onCommand(command: String): List<String> {
        return when (command.trim().lowercase()) {
            "help" -> listOf(
                "help        - show this help",
                "status      - show bridge status",
                "clear       - clear terminal output"
            )
            "clear" -> listOf("__CLEAR__")
            "status" -> listOf(
                "Bridge ready for packaged CrawlCipher runtime integration.",
                NativeRuntimeBridge.status()
            )
            else -> listOf("Command received: $command")
        }
    }
}
