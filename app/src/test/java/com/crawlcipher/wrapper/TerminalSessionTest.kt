package com.crawlcipher.wrapper

import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalSessionTest {
    @Test
    fun clearsOutputOnClearCommand() {
        val session = TerminalSession(PlaceholderRuntime())
        session.start()

        val output = session.submit("clear")

        assertTrue(output.isBlank())
    }

    @Test
    fun appendsCommandEcho() {
        val session = TerminalSession(PlaceholderRuntime())
        session.start()

        val output = session.submit("status")

        assertTrue(output.contains("> status"))
        assertTrue(output.contains("Bridge ready"))
    }
}
