package com.example.eco.db.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 动态数据模型（适配Room数据库 + 兼容模拟数据构造）
 */
@Entity(tableName = "dynamic") // 数据库表名
public class Dynamic {
    @PrimaryKey(autoGenerate = true) // 自增主键
    private long id;                // 动态ID（必须）
    private String username;        // 用户名
    private long publishTime;       // 发布时间（时间戳，数据库存储用）
    private String showTime;        // 展示用时间（如"2小时前"，非数据库存储）
    private String content;         // 动态内容
    private int likeCount;          // 点赞数
    private boolean isLiked;        // 是否已点赞

    // 无参构造（Room必须）
    public Dynamic() {}

    // 构造器1：数据库存储用（核心）
    @Ignore
    public Dynamic(String username, long publishTime, String content) {
        this.username = username;
        this.publishTime = publishTime;
        this.content = content;
        this.likeCount = 0; // 默认点赞数0
        this.isLiked = false; // 默认未点赞
    }

    // 构造器2：兼容原有模拟数据调用（关键修复点）
    @Ignore
    public Dynamic(String username, String showTime, String content, int likeCount, boolean isLiked) {
        this.username = username;
        this.showTime = showTime; // 展示用时间
        this.content = content;
        this.likeCount = likeCount;
        this.isLiked = isLiked;
        this.publishTime = System.currentTimeMillis(); // 补全数据库需要的时间戳
    }

    // Getter和Setter（包含新增的showTime）
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getPublishTime() { return publishTime; }
    public void setPublishTime(long publishTime) { this.publishTime = publishTime; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
}