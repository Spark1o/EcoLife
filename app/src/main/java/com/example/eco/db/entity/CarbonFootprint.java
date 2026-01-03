package com.example.eco.db.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// Room实体类，对应数据库表
@Entity(tableName = "carbon_footprint")
public class CarbonFootprint {
    @PrimaryKey(autoGenerate = true) // 主键自增
    private long id;
    private String type; // 出行/饮食/消费
    private String subType; // 自驾/公交/肉食等
    private float value; // 原始数值（公里数/金额/餐数）
    private double carbonValue; // 计算出的碳排放量（kgCO₂）
    private long createTime; // 记录时间（时间戳）

    // 空构造方法（Room必需，不可删除）
    public CarbonFootprint() {}

    // ========== 核心新增：适配模拟数据插入的构造方法 ==========
    // 用于模拟数据插入（仅传type/subType/value/createTime，carbonValue后续计算）
    @Ignore // 标记为Room忽略的构造方法
    public CarbonFootprint(String type, String subType, float value, long createTime) {
        this.type = type;
        this.subType = subType;
        this.value = value;
        this.createTime = createTime;
        this.carbonValue = 0.0; // 初始值，后续由DBUtil计算填充
    }

    // 原有全参构造方法（保留，适配已有逻辑）
    @Ignore
    public CarbonFootprint(String type, String subType, float value, float carbonValue, long createTime) {
        this.type = type;
        this.subType = subType;
        this.value = value;
        this.carbonValue = carbonValue;
        this.createTime = createTime;
    }

    // Getter & Setter 方法（完全保留，无修改）
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public float getValue() { return value; }
    public void setValue(float value) { this.value = value; }

    public double getCarbonValue() { return carbonValue; }
    public void setCarbonValue(double carbonValue) { this.carbonValue = carbonValue; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    // toString方法（完全保留，无修改）
    @Override
    public String toString() {
        return "CarbonFootprint{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", subType='" + subType + '\'' +
                ", value=" + value +
                ", carbonValue=" + carbonValue +
                ", createTime=" + createTime +
                '}';
    }
}