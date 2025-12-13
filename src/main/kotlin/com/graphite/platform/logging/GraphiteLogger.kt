package com.graphite.platform.logging

class GraphiteLogger(private val name: String) {
    fun info(message: String) {
        println("[GRAPHITE] ($name) INFO: $message")
    }
}