package com.graphite.platform.pathway

import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.LogManager

class InitializationThread(private val client: MinecraftClient) : Thread("Initialization Thread") {
    private val logger = LogManager.getLogger("Graphite Initializer")

    override fun run() {
        logger.info("Initializing Minecraft")
    }
}