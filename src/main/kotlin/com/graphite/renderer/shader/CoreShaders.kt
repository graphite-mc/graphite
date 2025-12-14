package com.graphite.renderer.shader

import net.minecraft.util.Identifier

object CoreShaders {
    val SPLASH_BLIT = GraphiteShader(Identifier("graphite", "splash_blit"))
    val FULLSCREEN_BLIT = GraphiteShader(Identifier("graphite", "fullscreen_blit"))
    val UI_QUADS = GraphiteShader(Identifier("graphite", "ui_quads"))
}