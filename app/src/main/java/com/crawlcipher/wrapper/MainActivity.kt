package com.crawlcipher.wrapper

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val session = TerminalSession(PlaceholderRuntime())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val output = findViewById<TextView>(R.id.outputText)
        val input = findViewById<EditText>(R.id.inputText)
        val send = findViewById<Button>(R.id.sendButton)
        val outputContainer = findViewById<ScrollView>(R.id.outputContainer)

        output.text = session.start()
        scrollToBottom(outputContainer)

        val submitAction = {
            val text = input.text?.toString().orEmpty()
            output.text = session.submit(text)
            input.text?.clear()
            scrollToBottom(outputContainer)
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

    private fun scrollToBottom(scrollView: ScrollView) {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
