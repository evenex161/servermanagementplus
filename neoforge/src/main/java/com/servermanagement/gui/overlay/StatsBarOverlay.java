package com.servermanagement.gui.overlay;

import com.servermanagement.client.ClientConfig;
import com.servermanagement.client.ClientDailyTasksData;
import com.servermanagement.features.economy.DailyTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StatsBarOverlay {

    public static final int WIDTH = 150;

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        if (!ClientConfig.isShowStatsBar()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;

        int x = ClientConfig.getStatsBarX();
        int y = ClientConfig.getStatsBarY();
        Font font = mc.font;

        List<DailyTask> tasks = ClientDailyTasksData.getTasks();
        if (tasks == null || tasks.isEmpty()) return;

        // Render Title
        guiGraphics.drawString(font, Component.literal("§6=== Daily Tasks ==="), x, y, 0xFFFFFF, true);
        y += 12;

        int count = 1;
        for (DailyTask task : tasks) {
            String status = task.isClaimed() ? "§a[Claimed]" : (task.isCompleted() ? "§a[Completed]" : "§e[" + task.getProgress() + "/" + task.getGoal() + "]");
            String text = "§7[" + count + "] §f" + task.getDescription() + " " + status;
            
            // Truncate if too long (optional)
            if (font.width(text) > WIDTH) {
                text = font.plainSubstrByWidth(text, WIDTH - 10) + "...";
            }
            
            guiGraphics.drawString(font, Component.literal(text), x, y, 0xFFFFFF, true);
            y += 10;
            count++;
        }
    }
}
