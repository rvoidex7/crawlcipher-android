package com.crawlcipher.wrapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeContractParserTest {
    @Test
    fun parsesContractFields() {
        val contract = RuntimeContractParser.parse(
            """
            {
              "entrypoint": "crawlcipher",
              "arguments": ["--profile", "mobile"],
              "supportedAbis": ["arm64-v8a", "armeabi-v7a"],
              "runtimeVersion": "1.8.0",
              "minWrapperVersion": "1.0.0",
              "sha256": "abc",
              "signature": "sig"
            }
            """.trimIndent()
        )

        requireNotNull(contract)
        assertEquals("crawlcipher", contract.entrypoint)
        assertEquals(listOf("--profile", "mobile"), contract.arguments)
        assertEquals(listOf("arm64-v8a", "armeabi-v7a"), contract.supportedAbis)
        assertEquals("1.8.0", contract.runtimeVersion)
        assertEquals("1.0.0", contract.minWrapperVersion)
        assertEquals("abc", contract.sha256)
        assertEquals("sig", contract.signature)
    }

    @Test
    fun acceptsEmptyOptionalFields() {
        val contract = RuntimeContractParser.parse("{}")
        requireNotNull(contract)
        assertNull(contract.entrypoint)
        assertEquals(emptyList<String>(), contract.arguments)
        assertEquals(emptyList<String>(), contract.supportedAbis)
        assertNull(contract.runtimeVersion)
        assertNull(contract.minWrapperVersion)
        assertNull(contract.sha256)
        assertNull(contract.signature)
    }
}
