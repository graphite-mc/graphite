package com.graphite.game.screens

import com.graphite.game.screens.titlescreen.PanoramaRenderer
import com.graphite.renderer.RenderSystem
import com.graphite.renderer.pathway.ui.BatchedRenderer
import com.graphite.renderer.utility.baseLevel
import com.graphite.renderer.utility.createTexture
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.SettingsScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.resource.language.I18n
import net.minecraft.util.Identifier
import net.minecraft.world.DemoServerWorld

object GraphiteTitleScreen : Screen() {
    private val minecraftTitleTextureView by lazy {
        client.textureManager.createTexture(Identifier("textures/gui/title/minecraft.png"))
    }

    init {
        client = MinecraftClient.getInstance()
        PanoramaRenderer.init(MinecraftClient.getInstance())
    }

    override fun init(minecraftClient: MinecraftClient?, i: Int, j: Int) {
        val j = client.height / 2
        this.buttons.clear()
        this.buttons.add(
            GraphiteButton(
                1,
                client.width / 2 - 200,
                client.height / 2 - 48,
                I18n.translate("menu.singleplayer")
            )
        )
        this.buttons.add(
            GraphiteButton(
                2,
                client.width / 2 - 200,
                j * 1,
                I18n.translate("menu.multiplayer")
            )
        )

        this.buttons.add(
            GraphiteButton(
                0,
                client.width / 2 - 200,
                j + 72 + 12,
                98,
                20,
                I18n.translate("menu.options")
            )
        )
        this.buttons.add(
            GraphiteButton(
                4,
                client.width / 2 + 4,
                j + 72 + 12,
                98,
                20,
                I18n.translate("menu.quit")
            )
        )
    }

    override fun render(mouseX: Int, mouseY: Int, tickDelta: Float) {
        PanoramaRenderer.render(RenderSystem.defaultRenderTarget().colorView, tickDelta)

        BatchedRenderer.withBatch {
            val mcTitleTexture = bindFrom(minecraftTitleTextureView)

            val l = client.width / 4 - 137 // 274 / 2
            val m = 30
            val scale = 2.0

            drawQuad(
                ((l + 0) * scale).toFloat(), (m * scale).toFloat(), (155 * scale).toFloat(), (44 * scale).toFloat(),
                (0f / 256f), (0f / 256f), (155f / 256f), (44f / 256f),
                1f, 1f, 1f, 1f,
                mcTitleTexture
            )

            drawQuad(
                ((l + 155) * scale).toFloat(), (m * scale).toFloat(), (155 * scale).toFloat(), (44 * scale).toFloat(),
                (0f / 256f), (45f / 256f), (155f / 256f), (89f / 256f),
                1f, 1f, 1f, 1f,
                mcTitleTexture
            )

            val copyRightText = "Copyright Mojang AB. Do not distribute!"
            val versionText = "Minecraft 1.8.9"
            drawTextWithShadow(versionText, 4f, client.height - 10f * 2f, 2f, -1)
            drawTextWithShadow(
                copyRightText,
                client.width - getTextWidth(copyRightText) * 2f - 4f,
                client.height - 20f,
                2f,
                -1
            )

            this@GraphiteTitleScreen.buttons.forEach {
                it.render(client, mouseX, mouseY)
            }
        }
    }
    override fun buttonClicked(buttonWidget: ButtonWidget) {
        if (buttonWidget.id == 0) {
//            this.client.setScreen(SettingsScreen(this, this.client.options))
        }

        if (buttonWidget.id == 1) {
            baseLevel(client)
        }

        if (buttonWidget.id == 2) {
//            this.client.setScreen(MultiplayerScreen(this))
        }

        if (buttonWidget.id == 4) {
            this.client.scheduleStop()
        }
    }
}