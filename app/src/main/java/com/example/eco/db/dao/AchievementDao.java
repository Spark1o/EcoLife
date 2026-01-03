package com.example.eco.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.eco.db.entity.Achievement;

import java.util.List;

@Dao
public interface AchievementDao {
    // 插入单个成就
    @Insert
    long insertAchievement(Achievement achievement);

    // 批量插入成就
    @Insert
    void insertAllAchievements(List<Achievement> achievementList);

    // 更新成就（解锁/奖励）
    @Update
    int updateAchievement(Achievement achievement);

    // 查询所有成就
    @Query("SELECT * FROM achievement ORDER BY id ASC")
    List<Achievement> getAllAchievements();

    // 查询未解锁成就
    @Query("SELECT * FROM achievement WHERE isUnlocked = 0 ORDER BY id ASC")
    List<Achievement> getUnlockedAchievements();

    // 根据条件类型查询成就
    @Query("SELECT * FROM achievement WHERE conditionType = :type AND isUnlocked = 0 LIMIT 1")
    Achievement getAchievementByConditionType(String type);
}