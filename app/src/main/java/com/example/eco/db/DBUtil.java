package com.example.eco.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.eco.db.dao.*;
import com.example.eco.db.entity.*;
import com.example.eco.db.manager.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DBUtil {

    public interface Callback<T> {
        void onResult(T data);
    }
    // 单例实例（核心：避免重复创建数据库）
    private static volatile DBUtil instance;

    private CarbonFootprintDao carbonDao;
    private GarbageDao garbageDao;
    private TaskDao taskDao;
    private PointDao pointDao;
    private AchievementDao achievementDao;
    // 新增：动态DAO
    private DynamicDao dynamicDao;
    private AppDatabase db;
    private Context mContext;
    private Handler mainHandler;

    // 新增：线程池（统一管理异步任务）- 永不手动关闭
    private ExecutorService executorService;
    // 新增：数据库可用状态标记
    private boolean isDbAvailable = true;

    // 私有化构造方法（单例模式）
    private DBUtil(Context context) {
        this.mContext = context.getApplicationContext();
        mainHandler = new Handler(Looper.getMainLooper());
        // 初始化线程池（使用CachedThreadPool，自动管理线程，无需关闭）
        executorService = Executors.newCachedThreadPool();


        // 初始化数据库
        try {
            db = AppDatabase.getInstance(mContext);
            carbonDao = db.carbonFootprintDao();
            garbageDao = db.garbageDao();
            taskDao = db.taskDao();
            pointDao = db.pointDao();
            achievementDao = db.achievementDao();
            // 新增：初始化动态DAO
            dynamicDao = db.dynamicDao();
            isDbAvailable = true;
        } catch (Exception e) {
            e.printStackTrace();
            isDbAvailable = false;
        }

        // 初始化基础数据
        initGarbageDataAsync();
        initDefaultTasksAsync();
        initDefaultAchievementsAsync();
    }

    // 获取单例实例（核心：全局唯一）
    public static DBUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (DBUtil.class) {
                if (instance == null) {
                    instance = new DBUtil(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ====================== 核心：补全 executeAsync 方法 ======================
    /**
     * 统一异步执行方法（带数据库状态检查）
     */
    private void executeAsync(Runnable task) {
        if (!isDbAvailable) {
            mainHandler.post(() ->
                    Toast.makeText(mContext, "数据库不可用", Toast.LENGTH_SHORT).show()
            );
            return;
        }
        // 捕获线程池可能的异常，避免崩溃
        try {
            executorService.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() ->
                            Toast.makeText(mContext, "操作失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() ->
                    Toast.makeText(mContext, "任务提交失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    // ====================== 新增：addTaskAsync 相关方法 ======================
    /**
     * 异步添加单个任务（带去重逻辑）
     * @param task 要添加的任务对象
     * @param callback 操作结果回调
     */
    public void addTaskAsync(Task task, TaskAddCallback callback) {
        executeAsync(() -> {
            try {
                // 1. 检查任务是否已存在（按标题去重）
                Task existingTask = taskDao.getTaskByTitle(task.getTitle());
                if (existingTask != null) {
                    // 任务已存在，直接回调成功
                    mainHandler.post(() -> callback.onAddSuccess("任务已存在，无需重复添加"));
                    return;
                }
                // 2. 插入新任务
                taskDao.insertTask(task);
                // 3. 主线程回调成功
                mainHandler.post(() -> callback.onAddSuccess("任务添加成功"));
            } catch (Exception e) {
                e.printStackTrace();
                // 4. 主线程回调失败
                mainHandler.post(() -> callback.onAddFailed(e));
            }
        });
    }

    /**
     * 任务添加回调接口
     */
    public interface TaskAddCallback {
        void onAddSuccess(String message);
        void onAddFailed(Exception e);
    }

    // ====================== 成就相关异步方法 ======================
    /**
     * 初始化默认成就（首次启动插入）
     */
    private void initDefaultAchievementsAsync() {
        executeAsync(() -> {
            if (achievementDao.getAllAchievements().isEmpty()) {
                List<Achievement> achievementList = new ArrayList<>();
                achievementList.add(new Achievement("环保新手", "首次记录碳足迹", "carbon_first", 1, 10));
                achievementList.add(new Achievement("分类达人", "累计查询10次垃圾分类", "garbage_10", 10, 20));
                achievementList.add(new Achievement("积分小富翁", "总积分达到100分", "point_100", 100, 30));
                achievementList.add(new Achievement("任务全能王", "完成所有挑战任务", "task_all", 0, 50));
                achievementList.add(new Achievement("周任务达人", "完成首次每周任务", "week_task", 0, 20));
                // 新增：动态发布者成就
                achievementList.add(new Achievement("动态发布者", "首次发布环保动态", "dynamic_first", 1, 15));
                achievementDao.insertAllAchievements(achievementList);
            }
        });
    }

    /**
     * 异步获取所有成就
     */
    public void getAllAchievementsAsync(AchievementQueryCallback callback) {
        executeAsync(() -> {
            List<Achievement> achievements = achievementDao.getAllAchievements();
            mainHandler.post(() -> callback.onQuerySuccess(achievements));
        });
    }

    /**
     * 解锁成就（核心方法）
     */
    private void unlockAchievementAsync(Achievement achievement, AchievementUnlockCallback callback) {
        executeAsync(() -> {
            // 更新成就状态
            achievement.setUnlocked(true);
            achievement.setUnlockTime(System.currentTimeMillis());
            achievementDao.updateAchievement(achievement);

            // 增加奖励积分
            int currentTotal = pointDao.getCurrentTotalPoint();
            int newTotal = currentTotal + achievement.getRewardPoint();
            Point point = new Point(achievement.getRewardPoint(), newTotal, "解锁成就：" + achievement.getTitle());
            pointDao.insertPoint(point);

            // 回调成功
            mainHandler.post(() -> callback.onUnlockSuccess(achievement.getTitle(), achievement.getRewardPoint(), newTotal));
        });
    }

    /**
     * 检测并解锁「首次记录碳足迹」成就
     */
    public void checkCarbonFirstAchievement(AchievementUnlockCallback callback) {
        executeAsync(() -> {
            List<CarbonFootprint> carbonList = carbonDao.getAllCarbonFootprints();
            if (carbonList.size() >= 1) {
                Achievement achievement = achievementDao.getAchievementByConditionType("carbon_first");
                if (achievement != null && !achievement.isUnlocked()) {
                    unlockAchievementAsync(achievement, callback);
                } else {
                    mainHandler.post(() -> callback.onUnlockFailed(new Exception("成就已解锁")));
                }
            } else {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("未满足解锁条件")));
            }
        });
    }

    /**
     * 检测并解锁「累计查询10次垃圾分类」成就
     */
    public void checkGarbage10Achievement(int queryCount, AchievementUnlockCallback callback) {
        executeAsync(() -> {
            if (queryCount >= 10) {
                Achievement achievement = achievementDao.getAchievementByConditionType("garbage_10");
                if (achievement != null && !achievement.isUnlocked()) {
                    unlockAchievementAsync(achievement, callback);
                } else {
                    mainHandler.post(() -> callback.onUnlockFailed(new Exception("成就已解锁")));
                }
            } else {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("未满足解锁条件：需查询10次")));
            }
        });
    }

    /**
     * 检测并解锁「总积分达100分」成就
     */
    public void checkPoint100Achievement(AchievementUnlockCallback callback) {
        executeAsync(() -> {
            int totalPoint = pointDao.getCurrentTotalPoint();
            if (totalPoint >= 100) {
                Achievement achievement = achievementDao.getAchievementByConditionType("point_100");
                if (achievement != null && !achievement.isUnlocked()) {
                    unlockAchievementAsync(achievement, callback);
                } else {
                    mainHandler.post(() -> callback.onUnlockFailed(new Exception("成就已解锁")));
                }
            } else {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("未满足解锁条件：需100积分")));
            }
        });
    }

    /**
     * 检测并解锁「完成所有任务」成就
     */
    public void checkTaskAllAchievement(AchievementUnlockCallback callback) {
        executeAsync(() -> {
            List<Task> allTasks = taskDao.getAllTasks();
            boolean allCompleted = true;
            for (Task task : allTasks) {
                if (!task.isCompleted()) {
                    allCompleted = false;
                    break;
                }
            }
            if (allCompleted) {
                Achievement achievement = achievementDao.getAchievementByConditionType("task_all");
                if (achievement != null && !achievement.isUnlocked()) {
                    unlockAchievementAsync(achievement, callback);
                } else {
                    mainHandler.post(() -> callback.onUnlockFailed(new Exception("成就已解锁")));
                }
            } else {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("未满足解锁条件：需完成所有任务")));
            }
        });
    }

    /**
     * 检测并解锁「周任务达人」成就
     */
    public void checkWeekTaskAchievement(AchievementUnlockCallback callback) {
        executeAsync(() -> {
            Achievement achievement = achievementDao.getAchievementByConditionType("week_task");

            if (achievement == null) {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("周任务达人成就不存在")));
                return;
            }

            if (achievement.isUnlocked()) {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("成就已解锁")));
                return;
            }

            unlockAchievementAsync(achievement, callback);
        });
    }

    // 新增：检测并解锁「动态发布者」成就
    /**
     * 检测并解锁「首次发布环保动态」成就
     */
    public void checkDynamicFirstAchievement(AchievementUnlockCallback callback) {
        executeAsync(() -> {
            // 1. 查询用户发布的动态数量
            List<Dynamic> dynamicList = dynamicDao.getAllDynamics();
            if (dynamicList.size() >= 1) {
                // 2. 查询动态发布者成就
                Achievement achievement = achievementDao.getAchievementByConditionType("dynamic_first");
                if (achievement == null) {
                    mainHandler.post(() -> callback.onUnlockFailed(new Exception("动态发布者成就不存在")));
                    return;
                }
                // 3. 检查是否已解锁
                if (!achievement.isUnlocked()) {
                    unlockAchievementAsync(achievement, callback);
                } else {
                    mainHandler.post(() -> callback.onUnlockFailed(new Exception("成就已解锁")));
                }
            } else {
                mainHandler.post(() -> callback.onUnlockFailed(new Exception("未满足解锁条件：需发布至少1条动态")));
            }
        });
    }

    /**
     * 通用添加积分方法（用于每周任务奖励）
     */
    public void addPointsAsync(int points, PointQueryCallback callback) {
        executeAsync(() -> {
            int currentTotal = pointDao.getCurrentTotalPoint();
            int newTotal = currentTotal + points;
            Point point = new Point(points, newTotal, "每周任务奖励");
            pointDao.insertPoint(point);
            mainHandler.post(() -> callback.onQuerySuccess(newTotal));
        });
    }

    // ====================== 任务相关异步方法 ======================
    /**
     * 初始化默认任务（首次启动插入）
     */
    private void initDefaultTasksAsync() {
        executeAsync(() -> {
            if (taskDao.getAllTasks().isEmpty()) {
                List<Task> taskList = new ArrayList<>();
                taskList.add(new Task("记录1次碳足迹", 10));
                taskList.add(new Task("查询1次垃圾分类", 5));
                taskList.add(new Task("连续3天记录碳足迹", 30));
                // 新增：发布1条动态任务
                taskList.add(new Task("发布1条环保动态", 15));
                taskDao.insertAllTasks(taskList);
            }
        });
    }

    /**
     * 异步获取所有任务
     */
    public void getAllTasksAsync(TaskQueryCallback callback) {
        executeAsync(() -> {
            List<Task> tasks = taskDao.getAllTasks();
            mainHandler.post(() -> callback.onQuerySuccess(tasks));
        });
    }

    /**
     * 异步标记任务完成，并自动增加积分
     */
    public void completeTaskAsync(Task task, TaskCompleteCallback callback) {
        executeAsync(() -> {
            task.setCompleted(true);
            task.setCompleteTime(System.currentTimeMillis());
            taskDao.updateTask(task);

            int currentTotal = pointDao.getCurrentTotalPoint();
            int newTotal = currentTotal + task.getPoint();

            Point point = new Point(task.getPoint(), newTotal, "完成任务：" + task.getTitle());
            pointDao.insertPoint(point);

            mainHandler.post(() -> callback.onCompleteSuccess(newTotal));
        });
    }

    // ====================== 积分相关异步方法 ======================
    /**
     * 异步获取当前总积分
     */
    public void getCurrentTotalPointAsync(PointQueryCallback callback) {
        executeAsync(() -> {
            int total = pointDao.getCurrentTotalPoint();
            mainHandler.post(() -> callback.onQuerySuccess(total));
        });
    }

    /**
     * 异步获取积分变动记录
     */
    public void getAllPointsAsync(PointListCallback callback) {
        executeAsync(() -> {
            List<Point> points = pointDao.getAllPoints();
            mainHandler.post(() -> callback.onQuerySuccess(points));
        });
    }

    // ====================== 联动方法 ======================
    /**
     * 记录碳足迹后，自动完成对应任务
     */
    public void completeCarbonTaskAfterInsertAsync(TaskCompleteCallback callback) {
        executeAsync(() -> {
            Task task = taskDao.getTaskByTitle("记录1次碳足迹");
            if (task != null && !task.isCompleted()) {
                completeTaskAsync(task, callback);
            } else {
                int currentTotal = pointDao.getCurrentTotalPoint();
                mainHandler.post(() -> callback.onCompleteSuccess(currentTotal));
            }
        });
    }

    /**
     * 查询垃圾分类后，自动完成对应任务
     */
    public void completeGarbageTaskAfterQueryAsync(TaskCompleteCallback callback) {
        executeAsync(() -> {
            Task task = taskDao.getTaskByTitle("查询1次垃圾分类");
            if (task != null && !task.isCompleted()) {
                completeTaskAsync(task, callback);
            } else {
                int currentTotal = pointDao.getCurrentTotalPoint();
                mainHandler.post(() -> callback.onCompleteSuccess(currentTotal));
            }
        });
    }

    // 新增：发布动态后自动完成对应任务
    /**
     * 发布动态后，自动完成「发布1条环保动态」任务
     */
    public void completeDynamicTaskAfterPublishAsync(TaskCompleteCallback callback) {
        executeAsync(() -> {
            Task task = taskDao.getTaskByTitle("发布1条环保动态");
            if (task != null && !task.isCompleted()) {
                completeTaskAsync(task, callback);
            } else {
                int currentTotal = pointDao.getCurrentTotalPoint();
                mainHandler.post(() -> callback.onCompleteSuccess(currentTotal));
            }
        });
    }

    // ====================== 碳足迹核心方法 ======================
    /**
     * 插入碳足迹数据（异步）
     */
    public void insertCarbonDataAsync(String type, String subType, float value, CarbonInsertCallback callback) {
        executeAsync(() -> {
            CarbonFootprint data = new CarbonFootprint();
            data.setType(type);
            data.setSubType(subType);
            data.setValue(value);
            data.setCarbonValue(calculateCarbon(type, subType, value));
            data.setCreateTime(System.currentTimeMillis());

            long result = carbonDao.insertCarbonFootprint(data);

            // 插入成功回调
            mainHandler.post(() -> callback.onInsertSuccess(result));

            // 自动完成任务
            completeCarbonTaskAfterInsertAsync(new TaskCompleteCallback() {
                @Override
                public void onCompleteSuccess(int newTotalPoint) {}

                @Override
                public void onCompleteFailed(Exception e) {
                    mainHandler.post(() ->
                            Toast.makeText(mContext, "任务完成失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
    }
    public void getAllCarbonFootprintAsync(CarbonListCallback callback) { // 改用和你一致的回调风格
        executeAsync(() -> {
            List<CarbonFootprint> list = carbonDao.queryAllCarbonFootprint();
            // 切回主线程回调结果（适配你的回调接口风格）
            mainHandler.post(() -> callback.onQuerySuccess(list));
        });
    }

    /**
     * 异步删除单条碳足迹记录
     * 适配历史记录页面删除功能
     */
    public void deleteCarbonFootprintByIdAsync(long id, CarbonDeleteCallback callback) { // 改用自定义回调（更规范）
        executeAsync(() -> {
            try {
                // 子线程删除：调用DAO中新增的deleteById方法
                carbonDao.deleteById(id);
                // 删除成功回调
                mainHandler.post(() -> callback.onDeleteSuccess());
            } catch (Exception e) {
                // 删除失败回调
                mainHandler.post(() -> callback.onDeleteFailed(e));
            }
        });
    }

    /**
     * 异步清空所有碳足迹记录
     * 适配历史记录页面清空功能
     */
    public void clearAllCarbonFootprintAsync(CarbonClearCallback callback) { // 改用自定义回调（更规范）
        executeAsync(() -> {
            try {
                // 子线程清空：调用DAO中新增的deleteAll方法
                carbonDao.deleteAll();
                // 清空成功回调
                mainHandler.post(() -> callback.onClearSuccess());
            } catch (Exception e) {
                // 清空失败回调
                mainHandler.post(() -> callback.onClearFailed(e));
            }
        });
    }

    /**
     * 异步查询本周碳足迹数据
     */
    public void getWeekCarbonDataAsync(long startTime, long endTime, CarbonWeekQueryCallback callback) {
        executeAsync(() -> {
            List<CarbonFootprint> data = carbonDao.queryWeekData(startTime, endTime);
            mainHandler.post(() -> callback.onQuerySuccess(data));
        });
    }

    /**
     * 计算碳足迹数值（纯计算）
     */
    public double calculateCarbon(String type, String subType, double value) {
        switch (type) {
            case "出行":
                switch (subType) {
                    case "自驾": return value * 0.18;
                    case "公交": return value * 0.03;
                    case "地铁": return value * 0.01;
                    default: return value * 0.1;
                }
            case "饮食":
                switch (subType) {
                    case "肉食": return value * 1.8;
                    case "素食": return value * 0.3;
                    case "外卖": return value * 0.5;
                    default: return value * 1.0;
                }
            case "消费":
                switch (subType) {
                    case "服装": return (value / 100) * 5;
                    case "电子产品": return (value / 100) * 10;
                    default: return (value / 100) * 2;
                }
            default: return 0.0;
        }
    }

    // ====================== 垃圾分类异步方法 ======================
    /**
     * 异步初始化垃圾分类数据
     */
    private void initGarbageDataAsync() {
        executeAsync(() -> {
            List<Garbage> existingData = garbageDao.getAllGarbage();
            if (existingData.isEmpty()) {
                List<Garbage> garbageList = new ArrayList<>();
                // 可回收物
                garbageList.add(new Garbage("塑料瓶".toLowerCase(Locale.ROOT), "可回收物"));
                garbageList.add(new Garbage("纸箱子".toLowerCase(Locale.ROOT), "可回收物"));
                garbageList.add(new Garbage("易拉罐".toLowerCase(Locale.ROOT), "可回收物"));
                garbageList.add(new Garbage("玻璃罐".toLowerCase(Locale.ROOT), "可回收物"));
                garbageList.add(new Garbage("旧衣服".toLowerCase(Locale.ROOT), "可回收物"));
                // 厨余垃圾
                garbageList.add(new Garbage("剩米饭".toLowerCase(Locale.ROOT), "厨余垃圾"));
                garbageList.add(new Garbage("苹果核".toLowerCase(Locale.ROOT), "厨余垃圾"));
                garbageList.add(new Garbage("菜叶".toLowerCase(Locale.ROOT), "厨余垃圾"));
                garbageList.add(new Garbage("骨头".toLowerCase(Locale.ROOT), "厨余垃圾"));
                garbageList.add(new Garbage("果皮".toLowerCase(Locale.ROOT), "厨余垃圾"));
                // 有害垃圾
                garbageList.add(new Garbage("电池".toLowerCase(Locale.ROOT), "有害垃圾"));
                garbageList.add(new Garbage("灯管".toLowerCase(Locale.ROOT), "有害垃圾"));
                garbageList.add(new Garbage("过期药品".toLowerCase(Locale.ROOT), "有害垃圾"));
                garbageList.add(new Garbage("指甲油".toLowerCase(Locale.ROOT), "有害垃圾"));
                garbageList.add(new Garbage("油漆桶".toLowerCase(Locale.ROOT), "有害垃圾"));
                // 其他垃圾
                garbageList.add(new Garbage("卫生纸".toLowerCase(Locale.ROOT), "其他垃圾"));
                garbageList.add(new Garbage("烟头".toLowerCase(Locale.ROOT), "其他垃圾"));
                garbageList.add(new Garbage("塑料袋".toLowerCase(Locale.ROOT), "其他垃圾"));
                garbageList.add(new Garbage("一次性筷子".toLowerCase(Locale.ROOT), "其他垃圾"));
                garbageList.add(new Garbage("陶瓷碗".toLowerCase(Locale.ROOT), "其他垃圾"));

                garbageDao.insertAll(garbageList);
            }
        });
    }

    public void getCarbonFootprintCountAsync(CarbonCountCallback callback) {
        executeAsync(() -> {
            List<CarbonFootprint> allData = carbonDao.queryAllCarbonFootprint();
            int count = allData != null ? allData.size() : 0;
            mainHandler.post(() -> callback.onQuerySuccess(count));
        });
    }

    // 异步更新记录
    public void updateCarbonFootprintAsync(CarbonFootprint item, CarbonUpdateCallback callback) {
        executeAsync(() -> {
            try {
                carbonDao.updateCarbonFootprint(item); // DAO中的@Update方法
                mainHandler.post(() -> callback.onUpdateSuccess());
            } catch (Exception e) {
                mainHandler.post(() -> callback.onUpdateFailed(e));
            }
        });
    }

    /**
     * 异步批量插入碳足迹数据（用于初始模拟数据）
     */
    public void insertCarbonDataListAsync(List<CarbonFootprint> dataList, CarbonListInsertCallback callback) {
        executorService.execute(() -> {
            try {
                carbonDao.insertAll(dataList);
                callback.onInsertSuccess();
            } catch (Exception e) {
                callback.onInsertFailed(e);
            }
        });
    }

    // ====================== 动态相关核心方法（新增） ======================
    /**
     * 插入动态数据（异步）
     * @param content 动态内容
     * @param callback 插入结果回调
     */
    public void insertDynamicDataAsync(String content, Callback<Long> callback) {
        executeAsync(() -> {
            Dynamic dynamic = new Dynamic();
            dynamic.setContent(content);
            dynamic.setPublishTime(System.currentTimeMillis());
            // 如需用户ID，补充：dynamic.setUserId(当前用户ID);

            long rowId = dynamicDao.insertDynamic(dynamic);
            mainHandler.post(() -> callback.onResult(rowId));

            // 自动完成「发布1条动态」任务
            completeDynamicTaskAfterPublishAsync(new TaskCompleteCallback() {
                @Override
                public void onCompleteSuccess(int newTotalPoint) {
                    // 任务完成后，检查动态发布者成就
                    checkDynamicFirstAchievement(new AchievementUnlockCallback() {
                        @Override
                        public void onUnlockSuccess(String achievementTitle, int rewardPoint, int newTotalPoint) {
                            mainHandler.post(() ->
                                    Toast.makeText(mContext, "解锁成就：" + achievementTitle + "，奖励" + rewardPoint + "积分", Toast.LENGTH_SHORT).show()
                            );
                        }

                        @Override
                        public void onUnlockFailed(Exception e) {
                            // 成就已解锁/未满足条件，无需提示
                        }
                    });
                }

                @Override
                public void onCompleteFailed(Exception e) {
                    mainHandler.post(() ->
                            Toast.makeText(mContext, "任务完成失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
    }

    /**
     * 异步获取用户发布的所有动态
     */
    public void getAllDynamicsAsync(Callback<List<Dynamic>> callback) {
        executeAsync(() -> {
            List<Dynamic> dynamics = dynamicDao.getAllDynamics();
            mainHandler.post(() -> callback.onResult(dynamics));
        });
    }

    // ====================== 回调接口定义 ======================
    // 成就相关回调
    public interface AchievementQueryCallback {
        void onQuerySuccess(List<Achievement> achievements);
        void onQueryFailed(Exception e);
    }

    public interface AchievementUnlockCallback {
        void onUnlockSuccess(String achievementTitle, int rewardPoint, int newTotalPoint);
        void onUnlockFailed(Exception e);
    }

    // 任务相关回调
    public interface TaskQueryCallback {
        void onQuerySuccess(List<Task> tasks);
        void onQueryFailed(Exception e);
    }

    public interface TaskCompleteCallback {
        void onCompleteSuccess(int newTotalPoint);
        void onCompleteFailed(Exception e);
    }

    // 积分相关回调
    public interface PointQueryCallback {
        void onQuerySuccess(int totalPoint);
        void onQueryFailed(Exception e);
    }

    public interface PointListCallback {
        void onQuerySuccess(List<Point> points);
        void onQueryFailed(Exception e);
    }

    // 碳足迹相关回调
    public interface CarbonInsertCallback {
        void onInsertSuccess(long rowId);
        void onInsertFailed(Exception e);
    }

    public interface CarbonQueryCallback {
        void onQuerySuccess(List<CarbonFootprint> data);
        void onQueryFailed(Exception e);
    }

    public interface CarbonWeekQueryCallback {
        void onQuerySuccess(List<CarbonFootprint> data);
        void onQueryFailed(Exception e);
    }

    public interface CarbonCountCallback {
        void onQuerySuccess(int count);
        void onQueryFailed(Exception e);
    }

    /**
     * 碳足迹列表查询回调
     */
    public interface CarbonListCallback {
        void onQuerySuccess(List<CarbonFootprint> data);
        // 可选：添加失败回调
        void onQueryFailed(Exception e);
    }

    /**
     * 碳足迹删除回调
     */
    public interface CarbonDeleteCallback {
        void onDeleteSuccess();
        void onDeleteFailed(Exception e);
    }

    /**
     * 碳足迹清空回调
     */
    public interface CarbonClearCallback {
        void onClearSuccess();
        void onClearFailed(Exception e);
    }
    public interface CarbonUpdateCallback {
        void onUpdateSuccess();
        void onUpdateFailed(Exception e);
    }
    public interface CarbonListInsertCallback {
        void onInsertSuccess();
        void onInsertFailed(Exception e);
    }
}