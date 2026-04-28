package com.servermanagement.gui.economy;

import com.servermanagement.client.ClientDailyTasksData;
import com.servermanagement.features.economy.DailyTask;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for daily tasks GUI
 */
public class DailyTasksMenu extends AbstractContainerMenu {
    private List<DailyTask> tasks;
    private long resetTime;

    public DailyTasksMenu(int containerId, Inventory playerInventory) {
        super(com.servermanagement.gui.ModMenuTypes.DAILY_TASKS_MENU, containerId);
        reloadFromClientCache();
    }

    /** Re-pull task list and reset time from the client cache. Called from the
     *  screen's init() so a late SyncDailyTasksPacket triggering refreshOpenScreen()
     *  picks up freshly synced data instead of the constructor snapshot. */
    public void reloadFromClientCache() {
        this.tasks = new ArrayList<>(ClientDailyTasksData.getTasks());
        this.resetTime = ClientDailyTasksData.getResetTime();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public List<DailyTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<DailyTask> tasks) {
        this.tasks = tasks;
    }

    public long getResetTime() {
        return resetTime;
    }

    public void setResetTime(long resetTime) {
        this.resetTime = resetTime;
    }
}
