package com.graphite.mixins.texture;

import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager {
    @Shadow @Final private Map<Identifier, Texture> textures;

    @Shadow public abstract boolean loadTexture(Identifier identifier, Texture texture);

    /**
     * @reason Use Graphite's texture management system
     * @author Lunasa
     */
    @Overwrite
    public void bindTexture(Identifier identifier) {
        Texture texture = this.textures.get(identifier);

        if (texture == null) {
            texture = new ResourceTexture(identifier); // TODO: Implement ResourceTexture to use Graphite's texture system
            this.loadTexture(identifier, texture);
        }

        // In wgpu, there is no binding step like in OpenGL, so this is effectively a no-op
    }
}
