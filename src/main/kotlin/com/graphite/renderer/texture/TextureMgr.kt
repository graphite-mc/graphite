package com.graphite.renderer.texture

object TextureMgr {
    val idToTexture = mutableMapOf<Int, GraphiteTexture>()

    private var boundTex = 0

    @JvmStatic
    fun registerTexture(texture: GraphiteTexture): Int {
        val index = idToTexture.size
        idToTexture[index] = texture
        return index
    }

    @JvmStatic
    fun getTexture(id: Int): GraphiteTexture? {
        return idToTexture[id]
    }

    @JvmStatic
    fun bind(texture: Int) {
        this.boundTex = texture
    }

    @JvmStatic
    fun getBound() = idToTexture[boundTex]!!
}