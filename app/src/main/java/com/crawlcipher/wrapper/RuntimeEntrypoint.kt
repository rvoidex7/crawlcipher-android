package com.crawlcipher.wrapper

data class RuntimeEntrypoint(
    val binaryName: String,
    val arguments: List<String>
)

object RuntimeEntrypointParser {
    fun parse(content: String): RuntimeEntrypoint? {
        val lines = content
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()

        if (lines.isEmpty()) {
            return null
        }

        return RuntimeEntrypoint(
            binaryName = lines.first(),
            arguments = lines.drop(1)
        )
    }
}
