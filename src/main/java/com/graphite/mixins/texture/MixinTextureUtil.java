package com.graphite.mixins.texture;

import com.graphite.renderer.texture.TextureMgr;
import net.minecraft.client.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TextureUtil.class)
public class MixinTextureUtil {
    /**
     * @reason Use Wgpu
     * @author Lunasa
     */
    @Overwrite
    public static void method_7027(int[][] mips, int baseWidth, int baseHeight, int xOffset, int yOffset, boolean bl, boolean bl2) {
        for (var mip = 0; mip < mips.length; mip++) {
            TextureMgr.getBound().uploadMipChunked(
                    mip,
                    baseWidth >> mip,
                    baseHeight >> mip,
                    xOffset >> mip,
                    yOffset >> mip,
                    mips[mip]
            );
        }
    }
}
