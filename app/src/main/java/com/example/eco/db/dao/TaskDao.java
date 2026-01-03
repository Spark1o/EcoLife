// TaskDao.java 正确代码
package com.example.eco.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.eco.db.entity.Task;

import java.util.List;

@Dao
public interface TaskDao {
    // 1. 修复getAllTasks查询（删除createTime相关）
    @Query("SELECT * FROM task") // 简单查询所有任务，无多余列
    List<Task> getAllTasks();

    // 2. 按标题查询任务（之前addTaskAsync需要的方法）
    @Query("SELECT * FROM task WHERE title = :title LIMIT 1")
    Task getTaskByTitle(String title);

    // 3. 插入单个任务
    @Insert
    long insertTask(Task task);

    // 4. 批量插入任务
    @Insert
    void insertAllTasks(List<Task> tasks);

    // 5. 更新任务
    @Update
    void updateTask(Task task);

    // 其他方法（如需要排序，用存在的字段，比如completeTime）
    // @Query("SELECT * FROM task ORDER BY completeTime DESC")
    // List<Task> getAllTasksSorted();
}