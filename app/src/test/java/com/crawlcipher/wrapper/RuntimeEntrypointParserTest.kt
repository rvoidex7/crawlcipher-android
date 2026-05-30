package com.crawlcipher.wrapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeEntrypointParserTest {
    @Test
    fun returnsNullForEmptyContent() {
        assertNull(RuntimeEntrypointParser.parse(" \n # comment only"))
    }

    @Test
    fun parsesBinaryAndArguments() {
        val entrypoint = RuntimeEntrypointParser.parse(
            """
            # runtime entrypoint
            crawlcipher
            --seed
            demo
            """.trimIndent()
        )

        requireNotNull(entrypoint)
        assertEquals("crawlcipher", entrypoint.binaryName)
        assertEquals(listOf("--seed", "demo"), entrypoint.arguments)
    }
}
