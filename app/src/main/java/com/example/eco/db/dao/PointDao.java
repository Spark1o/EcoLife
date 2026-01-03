package com.example.eco.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.eco.db.entity.Point;

import java.util.List;

@Dao
public interface PointDao {
    // 插入积分记录
    @Insert
    long insertPoint(Point point);

    // 查询所有积分变动记录
    @Query("SELECT * FROM point ORDER BY createTime DESC")
    List<Point> getAllPoints();

    // 查询当前总积分（取最新一条记录的totalPoint，无记录则返回0）
    @Query("SELECT IFNULL((SELECT totalPoint FROM point ORDER BY createTime DESC LIMIT 1), 0)")
    int getCurrentTotalPoint();
}