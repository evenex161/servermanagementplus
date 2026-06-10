package com.servermanagement.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/**
 * Toggles the vanilla {@code shaders/post/blur.json} post-processing chain
 * manually via a managed {@link PostChain} so the world is rendered blurred
 * behind our scalable GUIs (the 1.20.1 equivalent of MC 1.21+ {@code renderBlurredBackground}).
 *
 * <p>Refcounts callers so opening a second mod screen on top of an already
 * blurred screen keeps the effect alive across the transition.</p>
 */
public final class BlurBackdrop {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation BLUR_SHADER =
            new ResourceLocation("servermanagement", "shaders/post/blur.json");

    private static int activeCount = 0;
    private static PostChain blurChain;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private BlurBackdrop() {}

    public static void enable() {
        LOGGER.info("[ServerManagement] BlurBackdrop.enable() called. activeCount = {}", activeCount);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            LOGGER.info("[ServerManagement] mc is null");
            return;
        }
        if (mc.gameRenderer == null) {
            LOGGER.info("[ServerManagement] mc.gameRenderer is null");
            return;
        }
        if (mc.level == null) {
            LOGGER.info("[ServerManagement] mc.level is null");
            return;
        }

        LOGGER.info("[ServerManagement] activeCount before logic: {}", activeCount);
        if (activeCount == 0) {
            try {
                if (blurChain == null) {
                    LOGGER.info("[ServerManagement] Loading blur shader: {}", BLUR_SHADER);
                    blurChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), BLUR_SHADER);
                    lastWidth = mc.getWindow().getWidth();
                    lastHeight = mc.getWindow().getHeight();
                    blurChain.resize(lastWidth, lastHeight);
                    LOGGER.info("[ServerManagement] Shader loaded successfully!");
                } else {
                    int width = mc.getWindow().getWidth();
                    int height = mc.getWindow().getHeight();
                    if (width != lastWidth || height != lastHeight) {
                        lastWidth = width;
                        lastHeight = height;
                        blurChain.resize(width, height);
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("[ServerManagement] Failed to load blur shader: {}", BLUR_SHADER, t);
                if (blurChain != null) {
                    try {
                        blurChain.close();
                    } catch (Throwable ignored) {}
                }
                blurChain = null;
                activeCount = -1;
                return;
            }
        }
        if (activeCount >= 0) {
            activeCount++;
        }
        LOGGER.info("[ServerManagement] activeCount after logic: {}", activeCount);
    }

    public static void processBlur(float partialTick) {
        if (activeCount <= 0 || blurChain == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (width != lastWidth || height != lastHeight) {
            lastWidth = width;
            lastHeight = height;
            try {
                blurChain.resize(width, height);
            } catch (Throwable t) {
                LOGGER.error("[ServerManagement] Failed to resize blur shader", t);
                try {
                    blurChain.close();
                } catch (Throwable ignored) {}
                blurChain = null;
                return;
            }
        }

        try {
            blurChain.process(partialTick);
            
            // Construct and force the correct GUI projection matrix (depth range 1000 to 21000)
            Matrix4f guiProj = new Matrix4f().setOrtho(0.0F, (float)mc.getWindow().getGuiScaledWidth(), (float)mc.getWindow().getGuiScaledHeight(), 0.0F, 1000.0F, 21000.0F);
            RenderSystem.setProjectionMatrix(guiProj, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Throwable t) {
            LOGGER.error("[ServerManagement] Failed to process blur shader, resetting chain", t);
            try {
                blurChain.close();
            } catch (Throwable ignored) {}
            blurChain = null;
        }
    }

    public static void disable() {
        LOGGER.info("[ServerManagement] BlurBackdrop.disable() called. activeCount = {}", activeCount);
        if (activeCount <= 0) {
            return;
        }
        activeCount--;
        // Keep the blurChain cached in memory for subsequent GUI openings to eliminate compilation stutters.
        // It remains allocated until game exit or resource reload triggers an error/reset.
    }
}

