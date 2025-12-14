package com.graphite.mixins.texture;

import com.graphite.renderer.texture.GraphiteTexture;
import com.graphite.renderer.texture.TextureMgr;
import com.graphite.renderer.texture.impl.SimpleGraphiteTexture;
import com.graphite.renderer.texture.mc.MCTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteAtlasTexture.class)
public abstract class MixinSpriteAtlasTexture extends AbstractTexture implements MCTexture {
    @Unique
    private GraphiteTexture graphiteTexture;

    @Redirect(method = "method_7005", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;prepareImage(IIII)V"))
    void method_7005$prepareImage(int id, int max, int w, int h) {
        this.graphiteTexture = new SimpleGraphiteTexture(
                new Identifier("sat"),
                w,
                h,
                new int[]{},
                MinecraftClient.getInstance().options.mipmapLevels + 1
        );

        this.glId = TextureMgr.registerTexture(this.graphiteTexture);
        TextureMgr.bind(this.glId);
    }

    @Override
    public int getGlId() {
        return this.glId;
    }

    @Override
    public void clearGlId() {
        if (glId == -1) return;

        this.glId = -1;
        this.graphiteTexture.close();
    }

    @Override
    public @NotNull GraphiteTexture graphite$getTexture() {
        if (graphiteTexture == null) {
            throw new IllegalStateException("Texture has not been uploaded yet!");
        }

        return graphiteTexture;
    }
}
