package com.example.eco.db.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "task")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private long id;             // 主键自增
    private String title;        // 任务标题
    private int point;           // 任务积分
    private boolean completed;   // 是否完成
    private long completeTime;   // 完成时间

    // 无参构造（Room默认使用，必须保留）
    public Task() {
    }

    // ===== 关键：添加@Ignore注解消除警告 =====
    @Ignore
    public Task(String title, int point) {
        this.title = title;
        this.point = point;
        this.completed = false; // 默认未完成
        this.completeTime = 0;  // 默认无完成时间
    }

    // 所有Getter & Setter方法（保持不变）
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPoint() { return point; }
    public void setPoint(int point) { this.point = point; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public long getCompleteTime() { return completeTime; }
    public void setCompleteTime(long completeTime) { this.completeTime = completeTime; }
}