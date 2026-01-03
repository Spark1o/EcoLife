package com.example.eco.db.manager;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.eco.db.dao.*;
import com.example.eco.db.entity.*;

/**
 * Room 数据库核心管理类
 * 修复点：
 * 1. 移除手动操作 room_master_table 导致的哈希校验失败
 * 2. 升级版本号解决架构变更提示
 * 3. 仅保留必要的内部表创建逻辑
 * 4. 强化异常处理和单例安全性
 * 新增：注册 Dynamic 实体类和 DynamicDao 接口
 */
@Database(
        entities = {
                CarbonFootprint.class,
                Garbage.class,
                Task.class,
                Point.class,
                Achievement.class,
                Dynamic.class // 新增：注册动态实体类
        },
        version = 9, // 关键：升级版本号（从7→8），适配实体类新增
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DB_NAME = "eco_life_db";
    private static volatile AppDatabase INSTANCE;

    // DAO 抽象方法
    public abstract CarbonFootprintDao carbonFootprintDao();
    public abstract GarbageDao garbageDao();
    public abstract TaskDao taskDao();
    public abstract PointDao pointDao();
    public abstract AchievementDao achievementDao();
    public abstract DynamicDao dynamicDao(); // 新增：提供DynamicDao的获取方法

    /**
     * 获取数据库单例实例（线程安全）
     */
    public static AppDatabase getInstance(Context context) {
        // 强制使用Application上下文，避免内存泄漏
        Context appContext = context.getApplicationContext();

        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                        appContext,
                                        AppDatabase.class,
                                        DB_NAME
                                )
                                // 版本升级直接重建，清除旧数据（避免架构冲突）
                                .fallbackToDestructiveMigration()
                                // 仅创建必要的内部表（移除room_master_table操作）
                                .addCallback(new RoomDatabaseCallback())
                                .build();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 异常时置空实例，避免脏数据
                        INSTANCE = null;
                    }
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 检查数据库是否可用（适配 DBUtil 的状态检查）
     */
    public static boolean isDatabaseAvailable(Context context) {
        try {
            AppDatabase db = getInstance(context);
            return db != null && db.isOpen();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 销毁单例实例（应用退出时调用）
     */
    public static void destroyInstance() {
        if (INSTANCE != null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE != null && INSTANCE.isOpen()) {
                    INSTANCE.close();
                }
                INSTANCE = null;
            }
        }
    }

    /**
     * 数据库回调：仅创建 room_table_modification_log 表（Room 失效跟踪器需要）
     */
    private static class RoomDatabaseCallback extends Callback {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // 数据库首次创建时创建内部表
            createRoomModificationLogTable(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // 每次打开数据库时确保内部表存在
            createRoomModificationLogTable(db);
        }
    }

    /**
     * 仅创建 Room 失效跟踪器所需的 room_table_modification_log 表
     * 移除 room_master_table 相关操作（核心修复点）
     */
    private static void createRoomModificationLogTable(SupportSQLiteDatabase db) {
        try {
            // 仅创建 room_table_modification_log 表（Room 必需）
            db.execSQL("CREATE TABLE IF NOT EXISTS room_table_modification_log (" +
                    "table_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "table_name TEXT NOT NULL, " +
                    "invalidated INTEGER NOT NULL DEFAULT 0, " +
                    "last_modified INTEGER NOT NULL DEFAULT 0);");

            // 创建索引，和 Room 原生结构一致
            db.execSQL("CREATE INDEX IF NOT EXISTS index_room_table_modification_log_table_name " +
                    "ON room_table_modification_log(table_name);");

            // 彻底移除 room_master_table 相关操作（核心修复点）
        } catch (Exception e) {
            // 捕获异常但不崩溃，保证应用能正常运行
            e.printStackTrace();
        }
    }
}