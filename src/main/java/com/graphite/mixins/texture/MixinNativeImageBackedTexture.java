package com.graphite.mixins.texture;

import com.graphite.renderer.texture.GraphiteTexture;
import com.graphite.renderer.texture.TextureMgr;
import com.graphite.renderer.texture.impl.SimpleGraphiteTexture;
import com.graphite.renderer.texture.mc.MCTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.util.Random;

@Mixin(NativeImageBackedTexture.class)
public class MixinNativeImageBackedTexture extends AbstractTexture implements MCTexture {
    @Shadow @Final private int width;
    @Shadow @Final private int height;
    @Shadow @Final private int[] pixels;
    @Unique private GraphiteTexture graphiteTexture;

    @Redirect(method = "<init>(II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;prepareImage(III)V"))
    void prepareImageRedirect(int width, int height, int glFormat) {
        // No-op
    }

    /**
     * @reason Use Graphite's texture management system
     * @author Lunasa
     */
    @Overwrite
    public void upload() {
        this.graphiteTexture = new SimpleGraphiteTexture(
                new Identifier("graphite", "native_image_backed_texture/" + new Random().nextInt()),
                this.width,
                this.height,
                this.pixels
        );
        this.graphiteTexture.upload();

        this.glId = TextureMgr.registerTexture(this.graphiteTexture);
    }

    @Override
    public int getGlId() {
        return this.glId;
    }

    @Override
    public void clearGlId() {
        this.glId = -1;
        this.graphiteTexture.close();
    }

    @Override
    public void load(ResourceManager resourceManager) {
        // NIBTs are loaded differently, so we don't do anything here
    }

    @Override
    public @NotNull GraphiteTexture graphite$getTexture() {
        if (graphiteTexture == null) {
            throw new IllegalStateException("Texture has not been uploaded yet!");
        }

        return graphiteTexture;
    }
}
