package com.crawlcipher.wrapper

import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import java.nio.charset.StandardCharsets

class TerminalWebRenderer(
    private val webView: WebView
) {
    fun initialize() {
        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/terminal/index.html")
    }

    fun appendOutput(text: String) {
        val payload = Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        webView.post {
            webView.evaluateJavascript("window.CrawlCipherTerminal.appendBase64('$payload');", null)
        }
    }

    fun setStatus(status: String) {
        val payload = Base64.encodeToString(status.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        webView.post {
            webView.evaluateJavascript("window.CrawlCipherTerminal.setStatusBase64('$payload');", null)
        }
    }
}
