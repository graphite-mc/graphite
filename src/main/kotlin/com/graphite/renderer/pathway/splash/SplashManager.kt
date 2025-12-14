package com.graphite.renderer.pathway.splash

import com.graphite.platform.window.getWindow
import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.LogManager

class SplashManager {
    private val logger = LogManager.getLogger("Graphite Splash Renderer")

    fun run() {
        val client = MinecraftClient.getInstance()
        val textureManager = client.textureManager

        logger.info("Initializing splash renderer")

        SplashScreenRenderer.init(client, textureManager)

        logger.info("Starting render")

        while (rendering) {
            SplashScreenRenderer.render(client.width.toFloat(), client.height.toFloat())
            getWindow().update()
        }
    }

    companion object {
        @Volatile
        var rendering: Boolean = false

        val INSTANCE = SplashManager()

        fun start() {
            rendering = true
            INSTANCE.run()
        }

        fun stop() {
            rendering = false
        }
    }
}