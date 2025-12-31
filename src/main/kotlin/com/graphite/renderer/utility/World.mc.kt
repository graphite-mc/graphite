package com.graphite.renderer.utility

import net.minecraft.client.MinecraftClient
import net.minecraft.world.level.LevelInfo
import net.minecraft.world.level.LevelProperties
import net.minecraft.world.level.LevelGeneratorType
import net.minecraft.world.level.LevelInfo.GameMode
import net.minecraft.client.gui.screen.Screen
import net.minecraft.world.level.storage.LevelStorageAccess

fun baseLevel(client: MinecraftClient) {
    // Set up world properties
    val worldName = "BaseLevel"
    val saveDirectoryName = "BaseLevel"

    // Set default seed, game mode (survival), and world generation type (default)
    val seed = System.currentTimeMillis() // Use current time as the seed
    val gameMode = GameMode.SURVIVAL
    val generatorType = LevelGeneratorType.DEFAULT

    // Set up level info with default properties
    val levelInfo = LevelInfo(seed, gameMode, true, false, generatorType)

    // Create or load the world
    val levelStorageAccess = client.getCurrentSave()

    // Check if the world already exists
    if (levelStorageAccess.getLevelProperties(saveDirectoryName) != null) {
        // If the world exists, load it
        loadWorld(client, saveDirectoryName)
    } else {
        // If the world doesn't exist, create a new one
        createNewWorld(client, saveDirectoryName, worldName, levelInfo)
    }
}

// Method to load an existing world
fun loadWorld(client: MinecraftClient, saveDirectoryName: String) {
    val levelStorageAccess = client.getCurrentSave()
    val levelProperties = levelStorageAccess.getLevelProperties(saveDirectoryName)

    if (levelProperties != null) {
        println("Loading world: $saveDirectoryName")
        client.startIntegratedServer(saveDirectoryName, saveDirectoryName, LevelInfo(levelProperties))
    } else {
        println("World $saveDirectoryName does not exist.")
    }
}

// Method to create a new world
fun createNewWorld(client: MinecraftClient, saveDirectoryName: String, worldName: String, levelInfo: LevelInfo) {
    println("Creating new world: $worldName")

    // This would normally initiate the server creation process with the given level info
    client.startIntegratedServer(saveDirectoryName, worldName, levelInfo)
}
