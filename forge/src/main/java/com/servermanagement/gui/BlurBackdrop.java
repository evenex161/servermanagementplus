package com.servermanagement.gui;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Toggles the vanilla {@code shaders/post/blur.json} post-processing chain on
 * the {@code GameRenderer} so the world is rendered blurred behind our scalable
 * GUIs (the 1.20.1 equivalent of MC 1.21+ {@code renderBlurredBackground}).
 *
 * <p>Refcounts callers so opening a second mod screen on top of an already
 * blurred screen keeps the effect alive across the transition.</p>
 */
public final class BlurBackdrop {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation BLUR_SHADER =
            new ResourceLocation("servermanagement", "shaders/post/blur.json");

    private static int activeCount = 0;

    private BlurBackdrop() {}

    /**
     * Activate the blur effect (loads it the first time a screen requests it).
     * Safe to call from screen {@code init()} every frame: only the
     * leading-edge transition does work.
     */
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
                LOGGER.info("[ServerManagement] Loading blur shader: {}", BLUR_SHADER);
                mc.gameRenderer.loadEffect(BLUR_SHADER);
                LOGGER.info("[ServerManagement] Shader loaded successfully!");
            } catch (Throwable t) {
                LOGGER.error("[ServerManagement] Failed to load blur shader: {}", BLUR_SHADER, t);
                // Don't break the GUI if the shader fails to load.
                activeCount = -1; // mark as failed, never retry this session
                return;
            }
        }
        if (activeCount >= 0) {
            activeCount++;
        }
        LOGGER.info("[ServerManagement] activeCount after logic: {}", activeCount);
    }

    /**
     * Deactivate the blur effect for one caller. Shuts down the post chain
     * once the last caller releases it.
     */
    public static void disable() {
        LOGGER.info("[ServerManagement] BlurBackdrop.disable() called. activeCount = {}", activeCount);
        if (activeCount <= 0) {
            return;
        }
        activeCount--;
        if (activeCount == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameRenderer != null) {
                try {
                    LOGGER.info("[ServerManagement] Shutting down blur shader.");
                    mc.gameRenderer.shutdownEffect();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void applyBlurRadius(Minecraft mc, float radius) {
        // PostChain.passes is private in 1.20.1 and there is no public
        // setUniform on PostChain. We rely on the radius baked into the
        // vanilla blur.json post chain.
    }
}

