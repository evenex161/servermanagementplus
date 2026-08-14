package com.servermanagement.features.economy;

public class TaskComponent {
    private TaskType type;
    private int targetAmount;
    private String customDescription;

    public TaskComponent(TaskType type, int targetAmount, String customDescription) {
        this.type = type;
        this.targetAmount = targetAmount;
        this.customDescription = customDescription;
    }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }
    
    public int getTargetAmount() { return targetAmount; }
    public void setTargetAmount(int targetAmount) { this.targetAmount = targetAmount; }
    
    public String getCustomDescription() { return customDescription; }
    public void setCustomDescription(String customDescription) { this.customDescription = customDescription; }
}
