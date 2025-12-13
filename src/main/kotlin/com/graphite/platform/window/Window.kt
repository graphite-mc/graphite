package com.graphite.platform.window

interface Window {
    val nativeHandle: Long

    val width: Int
    val height: Int

    var title: String

    val shouldClose: Boolean
    var vsync: Boolean

    fun create(width: Int, height: Int, title: String, hints: WindowHints = WindowHints())
    fun update()
    fun destroy()
}