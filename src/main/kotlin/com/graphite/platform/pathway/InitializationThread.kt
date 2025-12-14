package com.graphite.platform.pathway

import com.graphite.game.screens.GraphiteTitleScreen
import com.graphite.platform.window.getWindow
import com.graphite.renderer.RenderSystem
import com.graphite.renderer.pathway.splash.SplashManager
import net.minecraft.advancement.AchievementsAndCriterions
import net.minecraft.client.MinecraftClient
import net.minecraft.client.MouseInput
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.AchievementNotification
import net.minecraft.client.gui.hud.InGameHud
import net.minecraft.client.option.GameOptions
import net.minecraft.client.particle.ParticleManager
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.LoadingScreenRenderer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.BlockRenderManager
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.render.model.BakedModelManager
import net.minecraft.client.sound.MusicTracker
import net.minecraft.client.sound.SoundManager
import net.minecraft.client.texture.PlayerSkinProvider
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.resource.FoliageColorResourceReloadListener
import net.minecraft.resource.GrassColorResourceReloadListener
import net.minecraft.util.Identifier
import net.minecraft.world.level.storage.AnvilLevelStorage
import org.apache.logging.log4j.LogManager
import java.io.File

class InitializationThread(private val client: MinecraftClient) : Thread("Initialization Thread") {
    private val logger = LogManager.getLogger("Graphite Initializer")

    override fun run() {
        logger.info("Initializing Minecraft")
        
        client.skinProvider = PlayerSkinProvider(client.textureManager, File(client.assetDirectory, "skins"), client.sessionService)
        client.currentSave = AnvilLevelStorage(File(client.runDirectory, "saves"))
        client.soundManager = SoundManager(client.resourceManager, client.options)
        client.resourceManager.registerListener(client.soundManager)
        client.musicTracker = MusicTracker(client)
        client.textRenderer = TextRenderer(client.options, Identifier("textures/font/ascii.png"), client.textureManager, false)
        if (client.options.language != null) {
            client.textRenderer.isUnicode = client.forcesUnicodeFont()
            client.textRenderer.isRightToLeft = client.languageManager.isRightToLeft
        }

        client.shadowTextRenderer = TextRenderer(client.options, Identifier("textures/font/ascii_sga.png"), client.textureManager, false)
        client.resourceManager.registerListener(client.textRenderer)
        client.resourceManager.registerListener(client.shadowTextRenderer)
        client.resourceManager.registerListener(GrassColorResourceReloadListener())
        client.resourceManager.registerListener(FoliageColorResourceReloadListener())
        AchievementsAndCriterions.TAKING_INVENTORY.setStatFormatter { string ->
            try {
                String.format(string, GameOptions.getFormattedNameForKeyCode(client.options.inventoryKey.code))
            } catch (exception: Exception) {
                "Error: " + exception.localizedMessage
            }
        }
        client.mouse = MouseInput()
        client.texture = SpriteAtlasTexture("textures")
        client.texture.setMaxTextureSize(client.options.mipmapLevels)
        client.textureManager.loadTickableTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX, client.texture)
        client.textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX)
        client.modelManager = BakedModelManager(client.texture)
        client.resourceManager.registerListener(client.modelManager)
        client.itemRenderer = ItemRenderer(client.textureManager, client.modelManager)
        client.entityRenderDispatcher = EntityRenderDispatcher(client.textureManager, client.itemRenderer)
        client.heldItemRenderer = HeldItemRenderer(client)
        client.resourceManager.registerListener(client.itemRenderer)
        client.blockRenderManager = BlockRenderManager(client.modelManager.modelShapes, client.options)
        client.resourceManager.registerListener(client.blockRenderManager)
        client.notification = AchievementNotification(client)

        RenderSystem.initialize()

        SplashManager.stop()

        client.loadingScreenRenderer = LoadingScreenRenderer(client)
        if (client.options.fullscreen && !client.fullscreen) {
            client.toggleFullscreen()
        }

        getWindow().vsync = false

        logger.info("Finished Minecraft initialization.")

        client.setScreen(GraphiteTitleScreen())
    }
}