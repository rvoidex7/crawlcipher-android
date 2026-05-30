package com.crawlcipher.wrapper

import android.os.Build
import org.json.JSONObject

data class RuntimeContract(
    val entrypoint: String?,
    val arguments: List<String>,
    val supportedAbis: List<String>,
    val runtimeVersion: String?,
    val minWrapperVersion: String?,
    val sha256: String?,
    val signature: String?
) {
    fun validate(wrapperVersion: String): String? {
        if (supportedAbis.isNotEmpty()) {
            val deviceAbis = Build.SUPPORTED_ABIS.orEmpty().map { it.lowercase() }
            val matches = supportedAbis.any { declared ->
                val normalized = declared.lowercase()
                deviceAbis.any { it == normalized || it.startsWith(normalized) }
            }
            if (!matches) {
                return "Runtime ABI mismatch. Runtime supports: ${supportedAbis.joinToString()} | Device supports: ${deviceAbis.joinToString()}"
            }
        }

        if (!minWrapperVersion.isNullOrBlank()) {
            if (compareVersions(wrapperVersion, minWrapperVersion) < 0) {
                return "Runtime requires wrapper >= $minWrapperVersion but app is $wrapperVersion."
            }
        }
        return null
    }

    private fun compareVersions(current: String, minimum: String): Int {
        val a = current.split(".").mapNotNull { it.toIntOrNull() }
        val b = minimum.split(".").mapNotNull { it.toIntOrNull() }
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) {
                return left.compareTo(right)
            }
        }
        return 0
    }
}

object RuntimeContractParser {
    fun parse(content: String): RuntimeContract? {
        val json = JSONObject(content)
        val abiArray = json.optJSONArray("supportedAbis")
        val argsArray = json.optJSONArray("arguments")
        return RuntimeContract(
            entrypoint = json.optString("entrypoint").ifBlank { null },
            arguments = buildList {
                if (argsArray != null) {
                    for (i in 0 until argsArray.length()) {
                        val arg = argsArray.optString(i)
                        if (arg.isNotBlank()) {
                            add(arg)
                        }
                    }
                }
            },
            supportedAbis = buildList {
                if (abiArray != null) {
                    for (i in 0 until abiArray.length()) {
                        val abi = abiArray.optString(i)
                        if (abi.isNotBlank()) {
                            add(abi)
                        }
                    }
                }
            },
            runtimeVersion = json.optString("runtimeVersion").ifBlank { null },
            minWrapperVersion = json.optString("minWrapperVersion").ifBlank { null },
            sha256 = json.optString("sha256").ifBlank { null },
            signature = json.optString("signature").ifBlank { null }
        )
    }
}
