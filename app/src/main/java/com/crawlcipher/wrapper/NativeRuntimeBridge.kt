package com.crawlcipher.wrapper

object NativeRuntimeBridge {
    private var loadError: String? = null

    fun initialize() {
        if (loadError != null) {
            return
        }
        loadError = try {
            System.loadLibrary("crawlcipher_runtime")
            null
        } catch (error: UnsatisfiedLinkError) {
            error.message ?: "Native runtime library is not bundled yet."
        }
    }

    fun status(): String {
        initialize()
        return loadError ?: "Native runtime library loaded from APK."
    }
}
