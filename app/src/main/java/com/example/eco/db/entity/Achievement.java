package com.example.eco.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 成就表：记录成就信息、解锁状态、解锁时间
 */
@Entity(tableName = "achievement")
public class Achievement {
    @PrimaryKey(autoGenerate = true)
    private int id;                // 自增主键
    private String title;          // 成就标题（如：环保新手）
    private String description;    // 成就描述（如：首次记录碳足迹）
    private String conditionType;  // 解锁条件类型（carbon_first/garbage_10/point_100/task_all）
    private int conditionValue;    // 解锁条件数值（如10次/100分）
    private boolean isUnlocked;    // 是否解锁
    private int rewardPoint;       // 解锁奖励积分
    private long unlockTime;       // 解锁时间（未解锁则为0）

    // 构造方法
    public Achievement(String title, String description, String conditionType, int conditionValue, int rewardPoint) {
        this.title = title;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.isUnlocked = false;
        this.rewardPoint = rewardPoint;
        this.unlockTime = 0;
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }
    public int getConditionValue() { return conditionValue; }
    public void setConditionValue(int conditionValue) { this.conditionValue = conditionValue; }
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    public int getRewardPoint() { return rewardPoint; }
    public void setRewardPoint(int rewardPoint) { this.rewardPoint = rewardPoint; }
    public long getUnlockTime() { return unlockTime; }
    public void setUnlockTime(long unlockTime) { this.unlockTime = unlockTime; }
}