package com.graphite.renderer.shader

import net.minecraft.util.Identifier

object CoreShaders {
    val SPLASH_BLIT = GraphiteShader(Identifier("graphite", "splash_blit"))
    val FULLSCREEN_BLIT = GraphiteShader(Identifier("graphite", "fullscreen_blit"))
    val UI_QUADS = GraphiteShader(Identifier("graphite", "ui_quads"))
    val PANORAMA_BLUR = GraphiteShader(Identifier("graphite", "panorama_blur"))
    val PANORAMA_SKYBOX = GraphiteShader(Identifier("graphite", "panorama_skybox"))
}