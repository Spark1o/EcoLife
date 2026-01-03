package com.example.eco.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import com.example.eco.db.entity.CarbonFootprint;
import java.util.List;

// Room数据访问接口
@Dao
public interface CarbonFootprintDao {
    // 插入单条数据，返回插入的主键ID（原有）
    @Insert
    long insertCarbonFootprint(CarbonFootprint data);

    // ========== 核心新增：批量插入方法（适配模拟数据） ==========
    /**
     * 批量插入碳足迹数据（用于初始模拟数据一次性插入多条）
     * @param dataList 要插入的CarbonFootprint列表
     */
    @Insert
    void insertAll(List<CarbonFootprint> dataList);

    // 查询所有数据，按创建时间倒序（原有）
    @Query("SELECT * FROM carbon_footprint ORDER BY createTime DESC")
    List<CarbonFootprint> queryAllCarbonFootprint();

    // 查询本周数据（原有）
    @Query("SELECT * FROM carbon_footprint WHERE createTime BETWEEN :startTime AND :endTime")
    List<CarbonFootprint> queryWeekData(long startTime, long endTime);

    // 查询所有数据（原有，和queryAllCarbonFootprint功能一致，保留兼容）
    @Query("SELECT * FROM carbon_footprint ORDER BY createTime DESC")
    List<CarbonFootprint> getAllCarbonFootprints();

    /**
     * 根据ID删除单条碳足迹记录（适配历史记录删除功能）
     * @param id 记录的主键ID
     */
    @Query("DELETE FROM carbon_footprint WHERE id = :id")
    void deleteById(long id);

    /**
     * 清空所有碳足迹记录（适配历史记录清空功能）
     */
    @Query("DELETE FROM carbon_footprint")
    void deleteAll();

    /**
     * 根据ID查询单条记录（适配编辑功能）
     * @param id 记录的主键ID
     * @return 对应的CarbonFootprint对象
     */
    @Query("SELECT * FROM carbon_footprint WHERE id = :id LIMIT 1")
    CarbonFootprint getCarbonFootprintById(long id);

    /**
     * 删除单条记录（可选：基于实体类删除，和deleteById二选一）
     * @param footprint 要删除的CarbonFootprint对象
     */
    @Delete
    void deleteCarbonFootprint(CarbonFootprint footprint);

    /**
     * 更新碳足迹记录（适配编辑功能核心方法）
     * Room会根据实体类的主键ID匹配，更新对应记录的所有字段
     * @param footprint 要更新的CarbonFootprint对象（必须包含主键ID）
     */
    @Update
    void updateCarbonFootprint(CarbonFootprint footprint);
}