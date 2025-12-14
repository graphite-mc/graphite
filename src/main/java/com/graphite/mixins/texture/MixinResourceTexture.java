package com.graphite.mixins.texture;

import com.graphite.renderer.texture.GraphiteTexture;
import com.graphite.renderer.texture.TextureMgr;
import com.graphite.renderer.texture.impl.SimpleGraphiteTexture;
import com.graphite.renderer.texture.mc.MCTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

@Mixin(ResourceTexture.class)
public class MixinResourceTexture extends AbstractTexture implements MCTexture {
    @Shadow @Final protected Identifier field_6555;
    @Unique
    private GraphiteTexture graphiteTexture;
    @Unique
    private int[] pixels;

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
    public void load(ResourceManager resourceManager) throws IOException {
        if (this.glId != -1) {
            this.clearGlId();
        }
        Resource resource = resourceManager.getResource(this.field_6555);
        InputStream inputStream = resource.getInputStream();
        BufferedImage bufferedImage = TextureUtil.create(inputStream);
        this.pixels = new int[bufferedImage.getWidth() * bufferedImage.getHeight()];
        bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), this.pixels, 0, bufferedImage.getWidth());

        this.graphiteTexture = new SimpleGraphiteTexture(
                this.field_6555,
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                this.pixels
        );
        this.graphiteTexture.upload();

        this.glId = TextureMgr.registerTexture(this.graphiteTexture);
    }

    @Override
    public @NotNull GraphiteTexture graphite$getTexture() {
        if (graphiteTexture == null) {
            throw new IllegalStateException("Texture has not been uploaded yet!");
        }

        return graphiteTexture;
    }
}
