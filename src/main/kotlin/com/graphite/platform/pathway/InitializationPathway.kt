package com.graphite.platform.pathway

import com.graphite.platform.graphics.glfw.GlfwManager
import com.graphite.platform.graphics.wgpu.WGPUManager
import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.platform.window.GlfwWindow
import com.graphite.platform.window.WindowHints
import com.graphite.platform.window.WindowManager
import com.graphite.renderer.pathway.splash.SplashManager
import com.graphite.renderer.pathway.ui.BatchedRenderer
import kotlinx.coroutines.runBlocking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.GameOptions
import net.minecraft.client.resource.ResourcePackLoader
import net.minecraft.client.resource.language.LanguageManager
import net.minecraft.client.texture.TextureManager
import net.minecraft.resource.ReloadableResourceManagerImpl
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

object InitializationPathway {
    private val LOGGER = LogManager.getLogger("Graphite Initializer")

    private val ICON_32_ID = Identifier("icons/icon_32x32.png")
    private val ICON_16_ID = Identifier("icons/icon_16x16.png")

    private lateinit var icon32Buffer: ByteBuffer
    private lateinit var icon16Buffer: ByteBuffer

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
        window.create(client.width, client.height, "Minecraft 1.8.9", hints = WindowHints(
            fullscreen = client.fullscreen
        ))
        runCatching {
            window.setupIcons(client)
        }.onFailure {
            LOGGER.warn("Failed to set window icons.", it)
        }

        WindowManager.setActiveWindow(window)

        runBlocking {
            WGPUManager.setupWGPU(window.nativeHandle)
        }

        withWGPU {
            LOGGER.info("WGPU initialized successfully.")
            LOGGER.info("Adapter: ${adapter.info.device} (${adapter.info.vendor}) - ${adapter.info.description}")
        }

        client.registerMetadataSerializers()
        client.loader = ResourcePackLoader(client.resourcePackDir, client.runDirectory.resolve("server-resource-packs"), client.defaultResourcePack, client.metadataSerializer, client.options)
        val resourceManager: ReloadableResourceManagerImpl = ReloadableResourceManagerImpl(client.metadataSerializer)
        client.resourceManager = resourceManager
        client.languageManager = LanguageManager(client.metadataSerializer, client.options.language)
        resourceManager.registerListener(client.languageManager)
        client.reloadResources()
        client.textureManager = TextureManager(resourceManager)
        resourceManager.registerListener(client.textureManager)
        InitializationThread(client).apply {
            start()
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { t, e ->
                LOGGER.error("Uncaught exception in initialization thread", e)
                SplashManager.stop()
            }
        }

        LOGGER.info("Initializing 2D graphics")
        BatchedRenderer.initialize()

        SplashManager.start()
    }

    private fun InputStream.readPixels(): ByteBuffer {
        val image = ImageIO.read(this)
            ?: error("Failed to decode image")

        val width = image.width
        val height = image.height

        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)

        val buffer = BufferUtils.createByteBuffer(width * height * 4)

        for (pixel in pixels) {
            val a = (pixel ushr 24 and 0xFF).toByte()
            val r = (pixel ushr 16 and 0xFF).toByte()
            val g = (pixel ushr 8  and 0xFF).toByte()
            val b = (pixel         and 0xFF).toByte()

            buffer.put(r)
            buffer.put(g)
            buffer.put(b)
            buffer.put(a)
        }

        buffer.flip()
        return buffer
    }


    private fun GlfwWindow.setupIcons(client: MinecraftClient) {
        icon32Buffer = client.defaultResourcePack.open(ICON_32_ID).readPixels()
        icon16Buffer = client.defaultResourcePack.open(ICON_16_ID).readPixels()

        val icon32 = GlfwWindow.Icon(32, 32, icon32Buffer)
        val icon16 = GlfwWindow.Icon(16, 16, icon16Buffer)

        setIcon(icon32, icon16)

        update()
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