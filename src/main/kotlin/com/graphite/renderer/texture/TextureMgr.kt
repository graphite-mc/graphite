package com.graphite.renderer.texture

object TextureMgr {
    val idToTexture = mutableMapOf<Int, GraphiteTexture>()

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
}