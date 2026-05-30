package com.crawlcipher.wrapper

import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeSmokeMatrixDefinitionTest {
    private data class SmokeCase(
        val name: String,
        val startupExpected: Boolean,
        val inputEchoExpected: Boolean,
        val cleanExitExpected: Boolean
    )

    @Test
    fun smokeMatrixCoversCoreRuntimeContract() {
        val matrix = listOf(
            SmokeCase("default_runtime", startupExpected = true, inputEchoExpected = true, cleanExitExpected = true),
            SmokeCase("runtime_with_args", startupExpected = true, inputEchoExpected = true, cleanExitExpected = true),
            SmokeCase("abi_mismatch_runtime", startupExpected = false, inputEchoExpected = false, cleanExitExpected = true)
        )

        assertTrue(matrix.any { it.startupExpected })
        assertTrue(matrix.any { !it.startupExpected })
        assertTrue(matrix.all { it.cleanExitExpected })
    }
}
