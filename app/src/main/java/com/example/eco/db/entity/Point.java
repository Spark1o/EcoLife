package com.example.eco.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 积分表：记录积分变动、总积分、变动原因
 */
@Entity(tableName = "point")
public class Point {
    @PrimaryKey(autoGenerate = true)
    private int id;                // 自增主键
    private int changeValue;       // 积分变动值（正数=增加，负数=减少）
    private int totalPoint;        // 变动后的总积分
    private String reason;         // 变动原因（如：完成任务"记录碳足迹"、兑换奖品）
    private long createTime;       // 变动时间

    // 构造方法
    public Point(int changeValue, int totalPoint, String reason) {
        this.changeValue = changeValue;
        this.totalPoint = totalPoint;
        this.reason = reason;
        this.createTime = System.currentTimeMillis();
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getChangeValue() { return changeValue; }
    public void setChangeValue(int changeValue) { this.changeValue = changeValue; }
    public int getTotalPoint() { return totalPoint; }
    public void setTotalPoint(int totalPoint) { this.totalPoint = totalPoint; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}