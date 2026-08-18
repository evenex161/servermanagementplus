package com.servermanagement.gui.overlay;

import com.servermanagement.client.ClientConfig;
import com.servermanagement.client.ClientDailyTasksData;
import com.servermanagement.features.economy.DailyTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class StatsBarOverlay {

    public static final int WIDTH = 150;
    private static final Map<Integer, Float> currentYMap = new HashMap<>();
    private static long lastRenderTime = 0;

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        if (!ClientConfig.isShowStatsBar()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.options.renderDebug) return;

        int x = ClientConfig.getStatsBarX();
        int y = ClientConfig.getStatsBarY();
        Font font = mc.font;

        List<DailyTask> tasks = ClientDailyTasksData.getTasks();
        if (tasks == null || tasks.isEmpty()) return;

        // Render Title
        guiGraphics.drawString(font, Component.literal("§6=== Daily Tasks ==="), x, y, 0xFFFFFF, true);
        
        long currentTime = System.currentTimeMillis();
        float delta = (lastRenderTime > 0) ? (currentTime - lastRenderTime) / 1000f : 0.016f;
        lastRenderTime = currentTime;
        
        int targetY = y + 12;

        int count = 1;
        for (DailyTask task : tasks) {
            float currentY = currentYMap.getOrDefault(count, (float)targetY);
            currentY += (targetY - currentY) * 10.0f * delta;
            currentYMap.put(count, currentY);

            String prefix = "§7[" + count + "] §f";
            String status = task.isClaimed() ? "§a[Claimed]" : (task.isCompleted() ? "§a[Completed]" : "§e[" + task.getProgress() + "/" + task.getGoal() + "]");
            
            int prefixWidth = font.width(prefix);
            int statusWidth = font.width(" " + status);
            int availableWidth = WIDTH - prefixWidth - statusWidth;
            
            String desc = task.getDescription();
            if (font.width(desc) > availableWidth && availableWidth > 20) {
                desc = font.plainSubstrByWidth(desc, availableWidth - font.width("...")) + "...";
            }
            
            String text = prefix + desc + " " + status;
            
            guiGraphics.drawString(font, Component.literal(text), x, (int)currentY, 0xFFFFFF, true);
            
            targetY += 10;
            count++;
        }
        
        int finalCount = count;
        currentYMap.keySet().removeIf(k -> k >= finalCount);
    }
}
