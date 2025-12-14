package com.graphite.mixins;

import com.graphite.game.MinecraftGameHandler;
import com.graphite.platform.graphics.wgpu.WGPUManager;
import com.graphite.platform.pathway.InitializationPathway;
import com.graphite.platform.window.DSLKt;
import com.graphite.utility.Hooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow @Final private static Logger LOGGER;

    @Shadow public boolean fullscreen;

    @Shadow public GameOptions options;

    @Shadow public int width;

    @Shadow public int height;

    @Shadow private int tempWidth;

    @Shadow private int tempHeight;

    @Shadow protected abstract void onResolutionChanged(int i, int j);

    @Shadow public Screen currentScreen;

    @Shadow protected abstract void resizeFramebuffer();

    @Shadow public abstract void updateDisplay();

    /**
     * @reason Use Graphite's game initialization pathway
     * @author Lunasa
     */
    @Overwrite
    public void initializeGame() {
        InitializationPathway.INSTANCE.initializeGame((MinecraftClient) (Object) this);
    }

    /**
     * @reason Use Graphite's game render pathway
     * @author Lunasa
     */
    @Overwrite
    public void runGameLoop() {
        MinecraftGameHandler.INSTANCE.runGameLoop((MinecraftClient) (Object) this);
    }

    /**
     * @reason Use Graphite's window system
     * @author Lunasa
     */
    @Overwrite
    public void toggleFullscreen() {
        var window = DSLKt.getWindow();

        try {
            this.fullscreen = !this.fullscreen;
            this.options.fullscreen = this.fullscreen;

            if (this.fullscreen) {
                this.width = window.getWidth();
                this.height = window.getHeight();
            } else {
                window.setSize(this.tempWidth, this.tempHeight);
                this.width = this.tempWidth;
                this.height = this.tempHeight;
            }

            if (this.width <= 0) {
                this.width = 1;
            }
            if (this.height <= 0) {
                this.height = 1;
            }

            if (this.currentScreen != null) {
                this.onResolutionChanged(this.width, this.height);
            }

            window.setFullscreen(this.fullscreen);
            window.setVsync(this.options.vsync);
            window.update();
        } catch (Exception exception) {
            LOGGER.error("Couldn't toggle fullscreen", exception);
        }
    }

    /**
     * @reason Use Wgpu
     * @author Lunasa
     */
    @Overwrite
    public static int getMaxTextureSize() {
        return Hooks.getMaxTexSize2D();
    }
}
