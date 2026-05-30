package com.crawlcipher.wrapper

import android.content.Context
import java.io.File

class RuntimeBinaryProvider(private val context: Context) {
    val runtimeDirectory: File
        get() = File(context.noBackupFilesDir, RUNTIME_DIR_NAME).apply { mkdirs() }

    fun prepareLaunchCommand(): List<String>? {
        installBundledRuntimeIfNeeded()
        val entrypoint = loadEntrypoint()
        val executable = resolveExecutable(entrypoint?.binaryName) ?: return null
        executable.setExecutable(true, false)
        return buildList {
            add(executable.absolutePath)
            addAll(entrypoint?.arguments.orEmpty())
        }
    }

    private fun installBundledRuntimeIfNeeded() {
        if (hasRunnableBinary()) {
            return
        }

        val assetNames = context.assets.list(BUNDLED_RUNTIME_ASSET_DIR) ?: return
        for (name in assetNames) {
            if (name.isBlank()) {
                continue
            }

            val target = File(runtimeDirectory, name)
            context.assets.open("$BUNDLED_RUNTIME_ASSET_DIR/$name").use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!name.endsWith(".txt")) {
                target.setExecutable(true, false)
            }
        }
    }

    private fun hasRunnableBinary(): Boolean =
        runtimeDirectory.listFiles()
            ?.any { it.isFile && !it.name.endsWith(".txt") && it.canExecute() }
            ?: false

    private fun loadEntrypoint(): RuntimeEntrypoint? {
        val file = File(runtimeDirectory, ENTRYPOINT_FILE_NAME)
        if (!file.exists() || !file.isFile) {
            return null
        }
        return RuntimeEntrypointParser.parse(file.readText(Charsets.UTF_8))
    }

    private fun resolveExecutable(binaryNameFromConfig: String?): File? {
        if (!binaryNameFromConfig.isNullOrBlank()) {
            val configured = File(runtimeDirectory, binaryNameFromConfig)
            return if (configured.exists() && configured.isFile) configured else null
        }

        val files = runtimeDirectory.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".txt") }
            ?.sortedBy { it.name }
            .orEmpty()

        return files.firstOrNull()
    }

    companion object {
        private const val RUNTIME_DIR_NAME = "runtime"
        private const val BUNDLED_RUNTIME_ASSET_DIR = "runtime"
        private const val ENTRYPOINT_FILE_NAME = "entrypoint.txt"
    }
}
