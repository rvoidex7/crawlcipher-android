package com.crawlcipher.wrapper

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class RuntimeBinaryProvider(private val context: Context) {
    data class LaunchConfiguration(
        val executable: File,
        val arguments: List<String>,
        val contract: RuntimeContract?
    )

    sealed class LaunchPreparation {
        data class Success(val config: LaunchConfiguration) : LaunchPreparation()
        data class Error(val message: String) : LaunchPreparation()
    }

    val runtimeDirectory: File
        get() = File(context.noBackupFilesDir, RUNTIME_DIR_NAME).apply { mkdirs() }

    fun prepareLaunchCommand(): LaunchPreparation {
        installBundledRuntimeIfNeeded()
        val entrypoint = loadEntrypoint()
        val contract = loadRuntimeContract()

        val configuredBinary = contract?.entrypoint ?: entrypoint?.binaryName
        val executable = resolveExecutable(configuredBinary)
            ?: return LaunchPreparation.Error(
                "No runtime binary found. Put runtime files under assets/runtime or no_backup/runtime."
            )

        val wrapperVersion = readWrapperVersionName()
        val contractValidationError = contract?.validate(wrapperVersion)
        if (contractValidationError != null) {
            return LaunchPreparation.Error(contractValidationError)
        }

        val expectedHash = contract?.sha256
        if (!expectedHash.isNullOrBlank()) {
            val actualHash = sha256Hex(executable)
            if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                return LaunchPreparation.Error(
                    "Runtime integrity check failed for ${executable.name}. Expected sha256=$expectedHash, actual=$actualHash"
                )
            }
        }

        val expectedSignature = contract?.signature
        if (!expectedSignature.isNullOrBlank()) {
            val publicKeyFile = File(runtimeDirectory, PUBLIC_KEY_FILE_NAME)
            if (!publicKeyFile.exists()) {
                return LaunchPreparation.Error(
                    "Runtime signature is provided, but $PUBLIC_KEY_FILE_NAME is missing in runtime directory."
                )
            }
            val verified = verifySignature(
                executable = executable,
                signatureBase64 = expectedSignature,
                publicKeyPem = publicKeyFile.readText(Charsets.UTF_8)
            )
            if (!verified) {
                return LaunchPreparation.Error("Runtime signature verification failed for ${executable.name}.")
            }
        }

        executable.setExecutable(true, false)
        val arguments = contract?.arguments ?: entrypoint?.arguments.orEmpty()
        return LaunchPreparation.Success(
            LaunchConfiguration(
                executable = executable,
                arguments = arguments,
                contract = contract
            )
        )
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

            if (!isConfigFile(name)) {
                target.setExecutable(true, false)
            }
        }
    }

    private fun hasRunnableBinary(): Boolean =
        runtimeDirectory.listFiles()
            ?.any { it.isFile && !isConfigFile(it.name) && it.canExecute() }
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
            ?.filter { it.isFile && !isConfigFile(it.name) }
            ?.sortedBy { it.name }
            .orEmpty()

        return files.firstOrNull()
    }

    private fun loadRuntimeContract(): RuntimeContract? {
        val file = File(runtimeDirectory, RUNTIME_CONTRACT_FILE_NAME)
        if (!file.exists() || !file.isFile) {
            return null
        }
        return try {
            RuntimeContractParser.parse(file.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    private fun readWrapperVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private fun verifySignature(
        executable: File,
        signatureBase64: String,
        publicKeyPem: String
    ): Boolean {
        return try {
            val pemBody = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")
            val keyBytes = Base64.getDecoder().decode(pemBody)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            executable.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    signature.update(buffer, 0, read)
                }
            }
            signature.verify(Base64.getDecoder().decode(signatureBase64))
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val RUNTIME_DIR_NAME = "runtime"
        private const val BUNDLED_RUNTIME_ASSET_DIR = "runtime"
        private const val ENTRYPOINT_FILE_NAME = "entrypoint.txt"
        private const val RUNTIME_CONTRACT_FILE_NAME = "runtime.json"
        private const val PUBLIC_KEY_FILE_NAME = "public_key.pem"
    }

    private fun isConfigFile(name: String): Boolean {
        return name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".sig") || name.endsWith(".pem")
    }
}
