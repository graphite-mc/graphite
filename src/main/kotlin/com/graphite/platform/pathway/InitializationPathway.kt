package com.graphite.platform.pathway

import com.graphite.platform.graphics.glfw.GlfwManager
import com.graphite.platform.graphics.wgpu.WGPUManager
import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.platform.logging.GraphiteLogger
import com.graphite.platform.window.GlfwWindow
import com.graphite.platform.window.WindowHints
import com.graphite.platform.window.WindowManager
import kotlinx.coroutines.runBlocking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.GameOptions
import org.lwjgl.Version
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

object InitializationPathway {
    private val LOGGER = GraphiteLogger("Graphite Initializer")

    fun initializeGame(client: MinecraftClient) {
        GlfwManager.initializeGlfw()

        client.options = GameOptions(client, client.runDirectory)
        client.resourcePacks.add(client.defaultResourcePack)
        this.initializeTimerHackThread(client)

        if (client.options.overrideHeight > 0 && client.options.overrideWidth > 0) {
            client.width = client.options.overrideWidth
            client.height = client.options.overrideHeight
        }

        LOGGER.info("LWJGL version: ${Version.getVersion()}")

        val window = GlfwWindow()
        window.create(client.width, client.height, "Graphite Minecraft Client", hints = WindowHints(
            fullscreen = client.fullscreen
        ))

        WindowManager.setActiveWindow(window)

        runBlocking {
            WGPUManager.setupWGPU(window.nativeHandle)
        }

        withWGPU {
            LOGGER.info("WGPU initialized successfully.")
            LOGGER.info("Adapter: ${adapter.info.device} by ${adapter.info.vendor}")
        }

        while (!window.shouldClose) {
            window.update()
        }
    }

    @Throws(IOException::class)
    private fun readInputStreamAsImage(inputStream: InputStream): ByteBuffer {
        val bufferedImage = ImageIO.read(inputStream)
        val `is` = bufferedImage.getRGB(
            0,
            0,
            bufferedImage.width,
            bufferedImage.height,
            null as IntArray?,
            0,
            bufferedImage.width
        )
        val byteBuffer = ByteBuffer.allocate(4 * `is`.size)

        for (k in `is`) {
            byteBuffer.putInt(k shl 8 or (k shr 24 and 255))
        }

        byteBuffer.flip()
        return byteBuffer
    }


    private fun initializeTimerHackThread(client: MinecraftClient) {
        val thread: Thread = object : Thread("Timer Hack Thread") {
            override fun run() {
                while (client.running) {
                    try {
                        sleep(2147483647L)
                    } catch (var2: InterruptedException) {
                    }
                }
            }
        }
        thread.setDaemon(true)
        thread.start()
    }
}