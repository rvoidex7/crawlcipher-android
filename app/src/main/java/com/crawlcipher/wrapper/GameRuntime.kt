package com.crawlcipher.wrapper

interface GameRuntime {
    fun boot(): List<String>
    fun onCommand(command: String): List<String>
}
