package com.graphite.renderer.texture.mc

import com.graphite.renderer.texture.GraphiteTexture

@Suppress("INAPPLICABLE_JVM_NAME")
interface MCTexture {
    @get:JvmName($$"graphite$getTexture")
    val graphiteTexture: GraphiteTexture
}