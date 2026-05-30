package com.crawlcipher.wrapper

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var output: TextView
    private lateinit var outputContainer: ScrollView
    private lateinit var input: EditText
    private lateinit var send: Button
    private lateinit var terminalHost: TerminalHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        output = findViewById(R.id.outputText)
        input = findViewById(R.id.inputText)
        send = findViewById(R.id.sendButton)
        outputContainer = findViewById(R.id.outputContainer)

        terminalHost = TerminalHost(applicationContext, object : TerminalHost.Listener {
            override fun onOutput(text: String) {
                runOnUiThread {
                    output.append(text)
                    scrollToBottom(outputContainer)
                }
            }
        })

        val submitAction = {
            val text = input.text?.toString().orEmpty()
            if (text.isNotEmpty()) {
                terminalHost.send("$text\n")
            }
            input.text?.clear()
        }

        send.setOnClickListener { submitAction() }

        input.setOnEditorActionListener { _, _, _ ->
            submitAction()
            true
        }

        input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                submitAction()
                true
            } else {
                false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        terminalHost.start()
    }

    override fun onDestroy() {
        terminalHost.stop()
        super.onDestroy()
    }

    private fun scrollToBottom(scrollView: ScrollView) {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
