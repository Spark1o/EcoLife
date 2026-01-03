package com.example.eco.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.eco.db.entity.Dynamic;

import java.util.List;

/**
 * 动态数据表DAO（数据访问接口）
 */
@Dao
public interface DynamicDao {
    /**
     * 插入单条动态（返回自增ID）
     */
    @Insert
    long insertDynamic(Dynamic dynamic);

    /**
     * 批量插入动态
     */
    @Insert
    void insertAllDynamics(List<Dynamic> dynamics);

    /**
     * 查询所有动态（按发布时间倒序）
     */
    @Query("SELECT * FROM dynamic ORDER BY publishTime DESC")
    List<Dynamic> getAllDynamics();

    /**
     * 查询指定用户的所有动态
     */
    @Query("SELECT * FROM dynamic WHERE username = :username ORDER BY publishTime DESC")
    List<Dynamic> getDynamicsByUsername(String username);

    /**
     * 查询动态总数（用于成就解锁检查）
     */
    @Query("SELECT COUNT(*) FROM dynamic")
    int getDynamicCount();

    /**
     * 更新动态（如点赞数、点赞状态）
     */
    @Update
    void updateDynamic(Dynamic dynamic);

    /**
     * 删除指定ID的动态
     */
    @Query("DELETE FROM dynamic WHERE id = :dynamicId")
    void deleteDynamicById(long dynamicId);

    /**
     * 清空所有动态
     */
    @Query("DELETE FROM dynamic")
    void deleteAllDynamics();
}