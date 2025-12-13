package com.graphite.mixins;

import com.graphite.platform.pathway.InitializationPathway;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    /**
     * @reason Use Graphite's game initialization pathway
     * @author Lunasa
     */
    @Overwrite
    public void initializeGame() {
        InitializationPathway.INSTANCE.initializeGame((MinecraftClient) (Object) this);
    }
}
