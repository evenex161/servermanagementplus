package com.servermanagement.gui.economy;

import com.servermanagement.client.ClientAchievementsData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Container menu for achievements GUI
 */
public class AchievementsMenu extends AbstractContainerMenu {
    private Set<String> earnedAchievements;
    private int totalRewards;

    public AchievementsMenu(int containerId, Inventory playerInventory) {
        super(com.servermanagement.gui.ModMenuTypes.ACHIEVEMENTS_MENU.get(), containerId);
        this.earnedAchievements = new HashSet<>(ClientAchievementsData.getEarnedAchievements());
        this.totalRewards = ClientAchievementsData.getTotalRewardsEarned();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Set<String> getEarnedAchievements() {
        return earnedAchievements;
    }

    public void setEarnedAchievements(Set<String> achievements) {
        this.earnedAchievements = achievements;
    }

    public int getTotalRewards() {
        return totalRewards;
    }

    public void setTotalRewards(int totalRewards) {
        this.totalRewards = totalRewards;
    }
}
