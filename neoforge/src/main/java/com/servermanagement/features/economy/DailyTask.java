package com.servermanagement.features.economy;

import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class DailyTask {
    private String id;
    private List<TaskComponent> components = new ArrayList<>();
    private int currentStep = 0;
    private int progress = 0;
    
    // Legacy fields for migration
    private TaskType type;
    private int goal;
    private String customDescription;
    
    private double rewardAmount;
    private List<ItemStack> rewardItems = new ArrayList<>();
    private boolean isClaimed;
    private long completedTime;
    
    public DailyTask() {}
    
    public DailyTask(String templateId, List<TaskComponent> components, double rewardAmount, List<ItemStack> rewardItems) {
        this.id = UUID.randomUUID().toString() + "-" + templateId;
        this.components = components != null ? components : new ArrayList<>();
        this.rewardAmount = rewardAmount;
        this.rewardItems = rewardItems != null ? rewardItems : new ArrayList<>();
        this.currentStep = 0;
        this.progress = 0;
        this.isClaimed = false;
        this.completedTime = 0;
    }
    
    public DailyTask(TaskType type, int goal) {
        this.id = UUID.randomUUID().toString();
        this.components = new ArrayList<>();
        this.components.add(new TaskComponent(type, goal, ""));
        this.rewardAmount = goal * 10.0;
        this.rewardItems = new ArrayList<>();
        this.currentStep = 0;
        this.progress = 0;
        this.isClaimed = false;
        this.completedTime = 0;
    }

    public void migrateLegacyData() {
        if (components == null) {
            components = new ArrayList<>();
        }
        if (components.isEmpty() && type != null) {
            components.add(new TaskComponent(type, goal, customDescription));
            type = null;
            customDescription = null;
        }
        if (rewardItems == null) {
            rewardItems = new ArrayList<>();
        }
    }

    public String getId() { return id; }
    
    public List<TaskComponent> getComponents() { return components != null ? components : new ArrayList<>(); }
    
    public TaskComponent getCurrentComponent() {
        if (components != null && currentStep >= 0 && currentStep < components.size()) {
            return components.get(currentStep);
        }
        return null; // All done or empty
    }
    
    public int getCurrentStep() { return currentStep; }
    
    // For StatsBarOverlay UI progress bounds
    public int getProgress() { return progress; }
    public int getGoal() {
        TaskComponent curr = getCurrentComponent();
        return curr != null ? curr.getTargetAmount() : 1;
    }
    
    // Legacy mapping
    public TaskType getType() { 
        TaskComponent curr = getCurrentComponent();
        return curr != null ? curr.getType() : TaskType.BREAK_BLOCKS;
    }
    
    public String getDescription() {
        TaskComponent curr = getCurrentComponent();
        if (curr == null) return "Completed";
        if (curr.getCustomDescription() != null && !curr.getCustomDescription().isEmpty()) {
            return curr.getCustomDescription();
        }
        return curr.getType().getDisplayName();
    }
    
    public String getFullDescription() {
        if (components == null || components.isEmpty()) return "Unknown Task";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            TaskComponent c = components.get(i);
            String text = c.getCustomDescription() != null && !c.getCustomDescription().isEmpty() ? c.getCustomDescription() : c.getType().getDisplayName();
            if (i < currentStep) sb.append("§m").append(text).append("§r, ");
            else if (i == currentStep) sb.append("§e").append(text).append("§r, ");
            else sb.append("§7").append(text).append("§r, ");
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    public double getRewardAmount() { return rewardAmount; }
    public List<ItemStack> getRewardItems() { return rewardItems; }
    public void setRewardItems(List<ItemStack> rewardItems) { this.rewardItems = rewardItems != null ? rewardItems : new ArrayList<>(); }
    public void setRewardAmount(double rewardAmount) { this.rewardAmount = rewardAmount; }

    public boolean isCompleted() {
        return currentStep >= components.size();
    }

    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { this.isClaimed = claimed; }

    public void addProgress(int amount) {
        if (isCompleted() || isClaimed()) return;
        
        TaskComponent current = getCurrentComponent();
        if (current == null) return;
        
        this.progress += amount;
        if (this.progress >= current.getTargetAmount()) {
            // Step finished
            this.currentStep++;
            this.progress = 0; // Reset for next step
            
            if (isCompleted()) {
                this.completedTime = System.currentTimeMillis();
            }
        }
    }
    
    // Used by networking
    public void setCurrentStep(int step) { this.currentStep = step; }
    public void setProgress(int progress) { this.progress = progress; }
    public void forceSetCompleted() { this.currentStep = this.components.size(); }
}
