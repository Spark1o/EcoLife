package com.example.eco.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.eco.db.entity.Garbage;

import java.util.List;

/**
 * 垃圾分类数据库操作接口
 */
@Dao
public interface GarbageDao {
    // 插入单条垃圾数据
    @Insert
    void insert(Garbage garbage);

    // 批量插入垃圾数据
    @Insert
    void insertAll(List<Garbage> garbageList);

    // 根据名称精确查询分类
    @Query("SELECT type FROM garbage WHERE name = :name")
    String getGarbageType(String name);

    // 模糊查询（根据名称关键词匹配）
    @Query("SELECT type FROM garbage WHERE name LIKE '%' || :keyword || '%'")
    List<String> searchGarbageType(String keyword);

    // 查询所有垃圾数据（用于初始化检查）
    @Query("SELECT * FROM garbage")
    List<Garbage> getAllGarbage();
}