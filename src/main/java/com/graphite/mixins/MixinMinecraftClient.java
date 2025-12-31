package com.graphite.mixins;

import com.graphite.game.MinecraftGameHandler;
import com.graphite.platform.graphics.wgpu.WGPUManager;
import com.graphite.platform.pathway.InitializationPathway;
import com.graphite.platform.window.DSLKt;
import com.graphite.utility.Hooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AchievementNotification;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LoadingScreenRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.resource.ResourcePackLoader;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.stat.StatHandler;
import net.minecraft.world.level.storage.LevelStorageAccess;
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

    @Shadow
    private IntegratedServer server;

    @Shadow
    public AchievementNotification notification;

    @Shadow
    public GameRenderer gameRenderer;

    @Shadow
    public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Shadow
    public LoadingScreenRenderer loadingScreenRenderer;

    @Shadow
    private ClientConnection clientConnection;

    @Shadow
    private Entity cameraEntity;

    @Shadow
    public ResourcePackLoader loader;

    @Shadow
    public InGameHud inGameHud;

    @Shadow
    public abstract void setCurrentServerEntry(ServerInfo serverInfo);

    @Shadow
    private boolean isIntegratedServerRunning;

    @Shadow
    public ClientWorld world;

    @Shadow
    public SoundManager soundManager;

    @Shadow
    public WorldRenderer worldRenderer;

    @Shadow
    public ParticleManager particleManager;

    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    public LevelStorageAccess currentSave;

    @Shadow
    private long sysTime;

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
     * @reason Use Graphite's connection pathway
     * @author Lunasa
     */
    @Overwrite
    public void connect(ClientWorld clientWorld, String string) {
        if (clientWorld == null) {
            ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
            if (clientPlayNetworkHandler != null) {
                clientPlayNetworkHandler.clearWorld();
            }

            if (this.server != null && this.server.hasGameDir()) {
                this.server.stopRunning();
                this.server.setIntegratedInstance();
            }

            this.server = null;
            this.notification.reset();
        }

        this.cameraEntity = null;
        this.clientConnection = null;
        if (this.loadingScreenRenderer != null) {
            this.loadingScreenRenderer.setTitleAndTask(string);
            this.loadingScreenRenderer.setTask("");
        }

        if (clientWorld == null && this.world != null) {
            this.loader.clear();
            this.inGameHud.resetDebugHudChunk();
            this.setCurrentServerEntry((ServerInfo)null);
            this.isIntegratedServerRunning = false;
        }

        this.soundManager.stopAll();
        this.world = clientWorld;
        if (clientWorld != null) {
            if (this.worldRenderer != null) {
                this.worldRenderer.setWorld(clientWorld);
            }

            if (this.particleManager != null) {
                this.particleManager.setWorld(clientWorld);
            }

            if (this.player == null) {
                this.player = this.interactionManager.createPlayer(clientWorld, new StatHandler());
                this.interactionManager.flipPlayer(this.player);
            }

            this.player.afterSpawn();
            clientWorld.spawnEntity(this.player);
            this.player.input = new KeyboardInput(this.options);
            this.interactionManager.copyAbilities(this.player);
            this.cameraEntity = this.player;
        } else {
            this.currentSave.clearAll();
            this.player = null;
        }

        this.sysTime = 0L;
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
