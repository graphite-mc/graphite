package com.graphite.mixins.texture;

import com.graphite.renderer.texture.mc.MCTexture;
import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractTexture.class)
public abstract class MixinAbstractTexture implements MCTexture {
    // We won't be implementing this because we need additional data to create textures

    /**
     * @reason Use Graphite's texture management system
     * @author Lunasa
     */
    @Overwrite
    public int getGlId() {
        return -1;
    }

    /**
     * @reason Use Graphite's texture management system
     * @author Lunasa
     */
    @Overwrite
    public void clearGlId() {
    }
}
