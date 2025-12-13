package com.graphite.platform.graphics.wgpu.ffi

import com.graphite.platform.api.Architecture
import com.graphite.platform.api.Os
import com.graphite.platform.api.Platform
import ffi.LibraryLoader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val WGPU_VERSION = "v25.0.0.1"
private val LOGGER = LogManager.getLogger("WGPU Loader")

object LibraryLoader {
    @Volatile
    private var loaded = false

    fun load() {
        if (loaded) {
            LOGGER.info("Library already loaded, skipping")
            return
        }

        synchronized(this) {
            if (loaded) return

            LOGGER.info("Initializing native library loader")
            LOGGER.info("OS=${Platform.os}, ARCH=${Platform.architecture}")

            val resourcePath = resolveClasspathResource()
            val extractedFile = extractToAbsoluteTemp(resourcePath)

            LOGGER.info("Loading native library from ${extractedFile.absolutePath}")

            System.load(extractedFile.absolutePath)

            loaded = true
            LOGGER.info("Native library loaded successfully")
        }
    }
}

private fun resolveClasspathResource(): String {
    val osSegment = when (Platform.os) {
        Os.Windows -> "win32"
        Os.Linux -> "linux"
        Os.MacOs -> "darwin"
    }

    val archSegment = when (Platform.architecture) {
        Architecture.X86_64 -> "x86-64"
        Architecture.AARCH64 -> {
            if (Platform.os == Os.Windows) {
                error("WGPU aarch64 is not supported on Windows")
            }
            "aarch64"
        }
    }

    val fileName = when (Platform.os) {
        Os.Windows -> "WGPU.dll"
        Os.Linux -> "libWGPU.so"
        Os.MacOs -> "libWGPU.dylib"
    }

    val fullPath = "/$osSegment-$archSegment/$fileName"

    LOGGER.info("Resolved classpath resource $fullPath")

    return fullPath
}

private fun extractToAbsoluteTemp(resourcePath: String): File {
    val input: InputStream = LibraryLoader::class.java.getResourceAsStream(resourcePath)
        ?: error("Native library not found on classpath: $resourcePath")

    val suffix = when (Platform.os) {
        Os.Windows -> ".dll"
        Os.Linux -> ".so"
        Os.MacOs -> ".dylib"
    }

    val tempFile = Files.createTempFile(
        "wgpu-${WGPU_VERSION}-",
        suffix
    ).toFile()

    LOGGER.info("Extracting native library to ${tempFile.absolutePath}")

    input.use {
        Files.copy(it, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    tempFile.deleteOnExit()

    if (!tempFile.exists()) {
        error("Extraction failed, temp file does not exist")
    }

    return tempFile
}
