package com.servermanagement.features.economy;

import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class DailyTaskTemplate {
    private String id;
    private List<TaskComponent> components = new ArrayList<>();
    
    // Legacy fields for migration
    private TaskType type;
    private int targetAmount;
    private String customDescription;
    
    private double rewardAmount;
    private ItemStack rewardItem;
    private List<ItemStack> rewardItems = new ArrayList<>();
    private boolean enabled;
    
    public DailyTaskTemplate() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.components = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
    }
    
    public DailyTaskTemplate(List<TaskComponent> components, double rewardAmount, List<ItemStack> rewardItems) {
        this.id = UUID.randomUUID().toString();
        this.components = components != null ? new ArrayList<>(components) : new ArrayList<>();
        this.rewardAmount = rewardAmount;
        this.rewardItems = rewardItems != null ? new ArrayList<>(rewardItems) : new ArrayList<>();
        this.enabled = true;
    }

    public void migrateLegacyData() {
        if (components == null) {
            components = new ArrayList<>();
        }
        if (components.isEmpty() && type != null) {
            components.add(new TaskComponent(type, targetAmount, customDescription));
            type = null;
            customDescription = null;
        }
        
        if (rewardItems == null) {
            rewardItems = new ArrayList<>();
        }
        if (rewardItem != null && !rewardItem.isEmpty()) {
            rewardItems.add(rewardItem.copy());
            rewardItem = null;
        }
    }

    public String getId() { return id; }
    
    public List<TaskComponent> getComponents() { return components != null ? components : new ArrayList<>(); }
    public void setComponents(List<TaskComponent> components) { this.components = components != null ? components : new ArrayList<>(); }

    public double getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(double rewardAmount) { this.rewardAmount = rewardAmount; }
    
    public List<ItemStack> getRewardItems() { return rewardItems != null ? rewardItems : new ArrayList<>(); }
    public void setRewardItems(List<ItemStack> rewardItems) { this.rewardItems = rewardItems != null ? rewardItems : new ArrayList<>(); }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public DailyTask createTask() {
        return new DailyTask(this.id, new ArrayList<>(this.components), this.rewardAmount, new ArrayList<>(this.rewardItems));
    }
    
    // Fallback getters for legacy network packets or UI that expects a single type
    public TaskType getType() { return components != null && !components.isEmpty() ? components.get(0).getType() : TaskType.BREAK_BLOCKS; }
    public int getTargetAmount() { return components != null && !components.isEmpty() ? components.get(0).getTargetAmount() : 0; }
    public String getCustomDescription() { return components != null && !components.isEmpty() ? components.get(0).getCustomDescription() : ""; }
}
