package com.crawlcipher.wrapper

import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private lateinit var terminalWebView: WebView
    private lateinit var terminalRenderer: TerminalWebRenderer
    private lateinit var terminalHost: TerminalHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        terminalWebView = findViewById(R.id.terminalWebView)
        terminalRenderer = TerminalWebRenderer(terminalWebView)
        terminalRenderer.initialize()

        terminalHost = TerminalHost(applicationContext, object : TerminalHost.Listener {
            override fun onOutput(text: String) {
                runOnUiThread {
                    terminalRenderer.appendOutput(text)
                }
            }

            override fun onStatus(text: String) {
                runOnUiThread {
                    terminalRenderer.setStatus(text)
                }
            }
        })

        findViewById<Button>(R.id.controlUp).setOnClickListener { sendControl("\u001B[A") }
        findViewById<Button>(R.id.controlDown).setOnClickListener { sendControl("\u001B[B") }
        findViewById<Button>(R.id.controlRight).setOnClickListener { sendControl("\u001B[C") }
        findViewById<Button>(R.id.controlLeft).setOnClickListener { sendControl("\u001B[D") }
        findViewById<Button>(R.id.controlEnter).setOnClickListener { sendControl("\n") }
        findViewById<Button>(R.id.controlEsc).setOnClickListener { sendControl("\u001B") }
        findViewById<Button>(R.id.controlLogs).setOnClickListener { showDebugLogs() }

    }

    override fun onStart() {
        super.onStart()
        terminalHost.start()
        terminalWebView.post {
            val cols = max(20, terminalWebView.width / 9)
            val rows = max(10, terminalWebView.height / 18)
            terminalHost.resize(rows = rows, cols = cols)
        }
    }

    override fun onDestroy() {
        terminalHost.stop()
        terminalWebView.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                sendControl("\u001B[A")
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                sendControl("\u001B[B")
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                sendControl("\u001B[D")
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                sendControl("\u001B[C")
                true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                sendControl("\n")
                true
            }
            else -> {
                val unicode = event?.unicodeChar ?: 0
                if (unicode > 0 && !event.isCtrlPressed && !event.isAltPressed) {
                    terminalHost.send(String(charArrayOf(unicode.toChar())))
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
        }
    }

    private fun showDebugLogs() {
        AlertDialog.Builder(this)
            .setTitle("Runtime Debug Logs")
            .setMessage(terminalHost.dumpDebugLog().ifBlank { "No logs yet." })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun sendControl(payload: String) {
        terminalHost.send(payload)
    }
}
