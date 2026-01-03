package com.example.eco.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 垃圾分类实体类（对应数据库garbage表）
 */
@Entity(tableName = "garbage")
public class Garbage {
    @PrimaryKey(autoGenerate = true)
    private int id; // 自增主键
    private String name; // 垃圾名称（小写存储）
    private String type; // 分类类型（可回收物/厨余垃圾/有害垃圾/其他垃圾）

    // 构造方法
    public Garbage(String name, String type) {
        this.name = name;
        this.type = type;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}