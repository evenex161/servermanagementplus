package com.servermanagement.gui;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Method;

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
    private static Method loadEffectMethod;
    private static Method shutdownEffectMethod;
    private static boolean reflectionInitialized = false;

    private BlurBackdrop() {}

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            loadEffectMethod = GameRenderer.class.getDeclaredMethod("loadEffect", ResourceLocation.class);
            loadEffectMethod.setAccessible(true);
            shutdownEffectMethod = GameRenderer.class.getDeclaredMethod("shutdownEffect");
            shutdownEffectMethod.setAccessible(true);
        } catch (Throwable t) {
            LOGGER.error("[ServerManagement] Failed to initialize reflection for GameRenderer methods", t);
            loadEffectMethod = null;
            shutdownEffectMethod = null;
        }
    }

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
        initReflection();
        if (loadEffectMethod == null) {
            LOGGER.info("[ServerManagement] loadEffectMethod is null (reflection failed)");
            activeCount = -1;
            return;
        }
        LOGGER.info("[ServerManagement] activeCount before logic: {}", activeCount);
        if (activeCount == 0) {
            try {
                LOGGER.info("[ServerManagement] Loading blur shader: {}", BLUR_SHADER);
                loadEffectMethod.invoke(mc.gameRenderer, BLUR_SHADER);
                LOGGER.info("[ServerManagement] Shader loaded successfully!");
            } catch (Throwable t) {
                LOGGER.error("[ServerManagement] Failed to load blur shader via reflection: {}", BLUR_SHADER, t);
                activeCount = -1;
                return;
            }
        }
        if (activeCount >= 0) {
            activeCount++;
        }
        LOGGER.info("[ServerManagement] activeCount after logic: {}", activeCount);
    }

    public static void disable() {
        LOGGER.info("[ServerManagement] BlurBackdrop.disable() called. activeCount = {}", activeCount);
        if (activeCount <= 0) {
            return;
        }
        activeCount--;
        if (activeCount == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameRenderer != null && shutdownEffectMethod != null) {
                try {
                    LOGGER.info("[ServerManagement] Shutting down blur shader.");
                    shutdownEffectMethod.invoke(mc.gameRenderer);
                } catch (Throwable t) {
                    LOGGER.error("[ServerManagement] Failed to shutdown blur shader via reflection", t);
                }
            }
        }
    }
}

