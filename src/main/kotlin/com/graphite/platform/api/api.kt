package com.graphite.platform.api

internal enum class Os {
    Linux,
    Windows,
    MacOs
}

internal object Platform {
    val os: Os
        get() = System.getProperty("os.name").let { name ->
            when {
                arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> Os.Linux
                arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> Os.MacOs
                arrayOf("Windows").any { name.startsWith(it) } -> Os.Windows
                else -> error("Unrecognized or unsupported operating system.")
            }
        }

}