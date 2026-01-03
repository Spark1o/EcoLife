package com.example.eco.fragment.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eco.R;
import com.example.eco.db.DBUtil;
import com.example.eco.db.entity.Achievement;
import com.example.eco.db.entity.CarbonFootprint;
import com.example.eco.db.entity.Task;

import java.util.Calendar;
import java.util.List;

/**
 * 任务+成就一体化Fragment
 * 修复版：解决碳足迹任务不加积分、社区任务控件不更新/不加积分问题
 */
public class TaskFragment extends Fragment {

    // ========== 任务配置常量 ==========
    private static final String TASK1_TITLE = "记录1次碳足迹数据";
    private static final String TASK2_TITLE = "查询1次垃圾分类";
    private static final String TASK3_TITLE = "发布1条环保动态";
    private static final String WEEK_TASK_TITLE = "本周减少5kgCO₂排放";
    private static final int TASK1_SCORE = 20;
    private static final int TASK2_SCORE = 20;
    private static final int TASK3_SCORE = 30;
    private static final int WEEK_TASK_SCORE = 50;

    // 单位转换：数据库中碳足迹值为g → 转换为kg
    private static final float CARBON_UNIT_CONVERT = 1000.0f;
    private static final float WEEK_CARBON_TARGET = 5.0f;

    // ========== 成就配置常量 ==========
    private static final String ACHIEVEMENT1_TYPE = "carbon_first";
    private static final String ACHIEVEMENT2_TYPE = "garbage_10";
    private static final String ACHIEVEMENT3_TYPE = "point_100";
    private static final String ACHIEVEMENT4_TYPE = "week_task";
    private static final String ACHIEVEMENT5_TYPE = "dynamic_first";

    // ========== 控件 ==========
    private TextView tvScore;
    private Button btnTask1, btnTask2, btnTask3, btnWeekTask;
    private TextView tvWeekTaskProgress, tvWeekCarbonReduce;
    private ProgressBar pbWeekTask;
    private ImageView ivAchievement1, ivAchievement2, ivAchievement3, ivAchievement4, ivAchievement5;
    private TextView tvAchievement1, tvAchievement2, tvAchievement3, tvAchievement4, tvAchievement5, tvAchievementTips;

    // ========== 数据 ==========
    private DBUtil dbUtil;
    private Handler mainHandler;
    private Context mContext;
    private List<Task> taskList;
    private List<Achievement> achievementList;
    private int currentScore = 0;
    private int garbageQueryCount = 0;
    private int dynamicPublishCount = 0;
    private SharedPreferences weekTaskSp;
    private static final String SP_WEEK_TASK = "week_task_sp";
    private static final String KEY_WEEK_TASK_COMPLETED = "week_task_completed";
    private static final String KEY_CURRENT_WEEK = "current_week";
    private float weekCarbonReduce = 0.0f;

    // 核心标记
    private boolean isTask1Done = false;
    private boolean isWeekTaskRewardGiven = false;

    // 新增：SP常量（统一管理）
    private static final String SP_ECO_DATA = "eco_data";
    private static final String KEY_DYNAMIC_PUBLISH_COUNT = "dynamic_publish_count";
    private static final String KEY_GARBAGE_QUERY_COUNT = "garbage_query_count";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        dbUtil = DBUtil.getInstance(mContext);
        mainHandler = new Handler(Looper.getMainLooper());

        // 修复：强制读取最新的SP数据（避免缓存问题）
        SharedPreferences sp = mContext.getSharedPreferences(SP_ECO_DATA, Context.MODE_PRIVATE);
        garbageQueryCount = sp.getInt(KEY_GARBAGE_QUERY_COUNT, 0);
        dynamicPublishCount = sp.getInt(KEY_DYNAMIC_PUBLISH_COUNT, 0);
        Log.d("TaskFragment", "读取SP：垃圾分类次数=" + garbageQueryCount + "，动态发布次数=" + dynamicPublishCount);

        // 初始化每周任务SP
        weekTaskSp = mContext.getSharedPreferences(SP_WEEK_TASK, Context.MODE_PRIVATE);
        isWeekTaskRewardGiven = weekTaskSp.getBoolean(KEY_WEEK_TASK_COMPLETED, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);
        initViews(view);
        loadAllData();
        return view;
    }

    /**
     * 初始化控件（修复：提前设置任务3默认UI）
     */
    private void initViews(View view) {
        // 积分显示
        tvScore = view.findViewById(R.id.tv_score);

        // 任务按钮（全部设为不可点击）
        btnTask1 = view.findViewById(R.id.btn_task1);
        btnTask2 = view.findViewById(R.id.btn_task2);
        btnTask3 = view.findViewById(R.id.btn_task3);
        btnWeekTask = view.findViewById(R.id.btn_week_task);

        btnTask1.setEnabled(false);
        btnTask2.setEnabled(false);
        btnTask3.setEnabled(false);
        btnWeekTask.setEnabled(false);

        // 修复：初始化任务按钮默认状态（避免空指针）
        updateTaskButtonStyle(btnTask1, false);
        updateTaskButtonStyle(btnTask2, false);
        updateTaskButtonStyle(btnTask3, false);
        updateTaskButtonStyle(btnWeekTask, false);

        // 每周任务控件
        tvWeekTaskProgress = view.findViewById(R.id.tv_week_task_progress);
        tvWeekCarbonReduce = view.findViewById(R.id.tv_week_carbon_reduce);
        pbWeekTask = view.findViewById(R.id.pb_week_task);
        pbWeekTask.setMax((int) (WEEK_CARBON_TARGET * 100));

        // 成就控件
        ivAchievement1 = view.findViewById(R.id.iv_achievement1);
        ivAchievement2 = view.findViewById(R.id.iv_achievement2);
        ivAchievement3 = view.findViewById(R.id.iv_achievement3);
        ivAchievement4 = view.findViewById(R.id.iv_achievement4);
        ivAchievement5 = view.findViewById(R.id.iv_achievement5);
        tvAchievement1 = view.findViewById(R.id.tv_achievement1);
        tvAchievement2 = view.findViewById(R.id.tv_achievement2);
        tvAchievement3 = view.findViewById(R.id.tv_achievement3);
        tvAchievement4 = view.findViewById(R.id.tv_achievement4);
        tvAchievement5 = view.findViewById(R.id.tv_achievement5);
        tvAchievementTips = view.findViewById(R.id.tv_achievement_tips);

        // 初始化文本
        tvAchievement1.setText("环保新手");
        tvAchievement2.setText("分类达人");
        tvAchievement3.setText("积分能手");
        tvAchievement4.setText("任务标兵");
        tvAchievement5.setText("动态作者");
        tvAchievementTips.setText("完成任务解锁更多成就徽章");
        tvWeekTaskProgress.setText(String.format("当前进度：%.1f/%.1fkg CO₂", 0.0f, WEEK_CARBON_TARGET));
        tvWeekCarbonReduce.setText(String.format("本周已减少：%.1fkg CO₂", 0.0f));
    }

    /**
     * 修复：调整加载顺序，先加载任务列表→再检查碳足迹→最后检查任务完成状态
     */
    public void loadAllData() {
        // 第一步：先加载任务列表（确保任务存在）
        loadTasks();

        // 第二步：加载积分和成就
        loadScore();
        loadAchievements();

        // 第三步：检查碳足迹并标记任务1
        checkCarbonFootprintAndMarkTask1();

        // 第四步：初始化每周任务
        initWeekTaskButton();

        // 第五步：强制检查任务2/3完成状态（核心修复）
        mainHandler.postDelayed(() -> {
            autoCheckTask2();
            autoCheckTask3();
            updateTask2And3Buttons();
        }, 500); // 延迟500ms，确保任务列表已加载完成
    }

    private void checkCarbonFootprintAndMarkTask1() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long weekStart = cal.getTimeInMillis();
        long weekEnd = System.currentTimeMillis();

        dbUtil.getWeekCarbonDataAsync(weekStart, weekEnd, new DBUtil.CarbonWeekQueryCallback() {
            @Override
            public void onQuerySuccess(List<CarbonFootprint> data) {
                Log.d("TaskFragment", "碳足迹数据条数：" + (data == null ? 0 : data.size()));

                // 1. 计算本周碳减排量
                weekCarbonReduce = 0.0f;
                if (data != null && !data.isEmpty()) {
                    for (CarbonFootprint cf : data) {
                        double carbonValue = dbUtil.calculateCarbon(cf.getType(), cf.getSubType(), cf.getValue());
                        weekCarbonReduce += (float) carbonValue;
                        Log.d("TaskFragment", "单条碳足迹计算（kg）：" + carbonValue + "kg");
                    }
                    isTask1Done = true;
                    updateTask1ButtonUI(true);

                    // 修复：立即触发任务1完成逻辑（不管数据库是否标记）
                    completeTask1Force();
                } else {
                    isTask1Done = false;
                    updateTask1ButtonUI(false);
                }

                // 2. 更新每周任务UI
                updateWeekCarbonProgress();

                // 3. 检测每周任务奖励
                autoCheckWeekTaskAndGiveReward();

                // 4. 检查周任务完成状态
                checkWeekTaskCompletion();

                // 5. 刷新周任务按钮
                initWeekTaskButton();
            }

            @Override
            public void onQueryFailed(Exception e) {
                weekCarbonReduce = 0.0f;
                isTask1Done = false;
                updateTask1ButtonUI(false);
                updateWeekCarbonProgress();
                e.printStackTrace();
            }
        });
    }

    private void completeTask1Force() {
        if (taskList == null) return;

        boolean task1Exists = false;
        Task task1 = null;

        for (Task task : taskList) {
            if (TASK1_TITLE.equals(task.getTitle())) {
                task1Exists = true;
                task1 = task;
                break;
            }
        }

        // 如果任务1不存在，先创建任务1
        if (!task1Exists) {
            // 关键：创建final变量供内部类引用
            final Task newTask1 = new Task();
            newTask1.setTitle(TASK1_TITLE);
            newTask1.setCompleted(false);
            newTask1.setPoint(TASK1_SCORE);
            dbUtil.addTaskAsync(newTask1, new DBUtil.TaskAddCallback() {
                @Override
                public void onAddSuccess(String message) {
                    Log.d("TaskFragment", "创建任务1成功：" + message);
                    completeTask1Internal(newTask1);
                }

                @Override
                public void onAddFailed(Exception e) {
                    Log.e("TaskFragment", "创建任务1失败：", e);
                }
            });
        } else {
            // 关键：如果是已有任务，创建final副本
            final Task finalTask1 = task1;
            completeTask1Internal(finalTask1);
        }
    }

    private void completeTask1Internal(Task task1) {
        if (task1.isCompleted()) {
            Log.d("TaskFragment", "任务1已完成，无需重复发放积分");
            return;
        }

        Log.d("TaskFragment", "强制完成任务1并发放积分");
        dbUtil.completeTaskAsync(task1, new DBUtil.TaskCompleteCallback() {
            @Override
            public void onCompleteSuccess(int newTotalPoint) {
                mainHandler.post(() -> {
                    tvScore.setText(String.valueOf(newTotalPoint));
                    currentScore = newTotalPoint;
                    Toast.makeText(mContext, "完成任务：记录碳足迹，积分+20", Toast.LENGTH_SHORT).show();
                    checkCarbonFirstAchievement();
                });
            }

            @Override
            public void onCompleteFailed(Exception e) {
                Log.e("TaskFragment", "完成任务1失败：", e);
                // 降级处理：直接加积分（绕过任务完成逻辑）
                dbUtil.addPointsAsync(TASK1_SCORE, new DBUtil.PointQueryCallback() {
                    @Override
                    public void onQuerySuccess(int newTotalPoint) {
                        mainHandler.post(() -> {
                            tvScore.setText(String.valueOf(newTotalPoint));
                            currentScore = newTotalPoint;
                            Toast.makeText(mContext, "碳足迹任务积分+20（降级发放）", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onQueryFailed(Exception e) {
                        Log.e("TaskFragment", "降级发放积分失败：", e);
                    }
                });
            }
        });
    }


    private void updateTask1ButtonUI(boolean isDone) {
        if (btnTask1 == null) return;

        mainHandler.post(() -> {
            if (isDone) {
                btnTask1.setText("已完成");
                btnTask1.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.primary_green));
                btnTask1.setTextColor(ContextCompat.getColorStateList(mContext, R.color.white));
            } else {
                btnTask1.setText("未完成");
                btnTask1.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.gray_light));
                btnTask1.setTextColor(ContextCompat.getColorStateList(mContext, R.color.gray_dark));
            }
            // 强制刷新UI
            btnTask1.invalidate();
            btnTask1.requestLayout();
            Log.d("TaskFragment", "任务1UI已强制更新：" + (isDone ? "已完成" : "未完成"));
        });
    }

    private void loadTasks() {
        dbUtil.getAllTasksAsync(new DBUtil.TaskQueryCallback() {
            @Override
            public void onQuerySuccess(List<Task> tasks) {
                taskList = tasks;
                Log.d("TaskFragment", "任务列表条数：" + tasks.size());

                // 检查并创建缺失的任务（核心修复）
                checkAndCreateMissingTasks();

                // 1. 处理任务1状态
                for (Task task : tasks) {
                    if (TASK1_TITLE.equals(task.getTitle())) {
                        isTask1Done = task.isCompleted();
                        updateTask1ButtonUI(isTask1Done);
                        break;
                    }
                }

                // 2. 更新任务2/3按钮
                updateTask2And3Buttons();

                Log.d("TaskFragment", "任务列表加载完成，任务1状态：" + isTask1Done);
            }

            @Override
            public void onQueryFailed(Exception e) {
                Toast.makeText(mContext, "加载任务失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }


    private void checkAndCreateMissingTasks() {
        boolean hasTask1 = false, hasTask2 = false, hasTask3 = false;

        for (Task task : taskList) {
            if (TASK1_TITLE.equals(task.getTitle())) hasTask1 = true;
            if (TASK2_TITLE.equals(task.getTitle())) hasTask2 = true;
            if (TASK3_TITLE.equals(task.getTitle())) hasTask3 = true;
        }

        // 创建任务1
        if (!hasTask1) {
            Task task1 = new Task();
            task1.setTitle(TASK1_TITLE);
            task1.setCompleted(false);
            task1.setPoint(TASK1_SCORE);
            dbUtil.addTaskAsync(task1, new DBUtil.TaskAddCallback() {
                @Override
                public void onAddSuccess(String message) {
                    Log.d("TaskFragment", "创建任务1成功：" + message);
                }

                @Override
                public void onAddFailed(Exception e) {
                    Log.e("TaskFragment", "创建任务1失败：", e);
                }
            });
        }

        // 创建任务2
        if (!hasTask2) {
            Task task2 = new Task();
            task2.setTitle(TASK2_TITLE);
            task2.setCompleted(false);
            task2.setPoint(TASK2_SCORE);
            dbUtil.addTaskAsync(task2, new DBUtil.TaskAddCallback() {
                @Override
                public void onAddSuccess(String message) {
                    Log.d("TaskFragment", "创建任务2成功：" + message);
                }

                @Override
                public void onAddFailed(Exception e) {
                    Log.e("TaskFragment", "创建任务2失败：", e);
                }
            });
        }

        // 创建任务3（核心）
        if (!hasTask3) {
            Task task3 = new Task();
            task3.setTitle(TASK3_TITLE);
            task3.setCompleted(false);
            task3.setPoint(TASK3_SCORE);
            dbUtil.addTaskAsync(task3, new DBUtil.TaskAddCallback() {
                @Override
                public void onAddSuccess(String message) {
                    Log.d("TaskFragment", "创建任务3成功：" + message);
                    // 创建成功后立即检查任务3完成状态
                    autoCheckTask3();
                }

                @Override
                public void onAddFailed(Exception e) {
                    Log.e("TaskFragment", "创建任务3失败：", e);
                }
            });
        }
    }

    private void autoCheckTask2() {
        if (garbageQueryCount < 1 || taskList == null) return;

        for (Task task : taskList) {
            if (TASK2_TITLE.equals(task.getTitle()) && !task.isCompleted()) {
                Log.d("TaskFragment", "自动完成任务2");

                dbUtil.completeTaskAsync(task, new DBUtil.TaskCompleteCallback() {
                    @Override
                    public void onCompleteSuccess(int newTotalPoint) {
                        mainHandler.post(() -> {
                            updateTaskButtonStyle(btnTask2, true);
                            tvScore.setText(String.valueOf(newTotalPoint));
                            currentScore = newTotalPoint;
                            Toast.makeText(mContext, "完成任务：查询垃圾分类，积分+20", Toast.LENGTH_SHORT).show();
                            checkGarbage10Achievement();
                        });
                    }

                    @Override
                    public void onCompleteFailed(Exception e) {
                        Log.e("TaskFragment", "完成任务2失败：", e);
                        // 降级处理
                        dbUtil.addPointsAsync(TASK2_SCORE, new DBUtil.PointQueryCallback() {
                            @Override
                            public void onQuerySuccess(int newTotalPoint) {
                                mainHandler.post(() -> {
                                    tvScore.setText(String.valueOf(newTotalPoint));
                                    currentScore = newTotalPoint;
                                    updateTaskButtonStyle(btnTask2, true);
                                });
                            }

                            @Override
                            public void onQueryFailed(Exception e) {}
                        });
                    }
                });
                break;
            }
        }
    }


    private void autoCheckTask3() {
        if (dynamicPublishCount < 1 || taskList == null) {
            Log.d("TaskFragment", "动态发布次数不足：" + dynamicPublishCount);
            return;
        }

        for (Task task : taskList) {
            if (TASK3_TITLE.equals(task.getTitle()) && !task.isCompleted()) {
                Log.d("TaskFragment", "自动完成任务3");

                dbUtil.completeTaskAsync(task, new DBUtil.TaskCompleteCallback() {
                    @Override
                    public void onCompleteSuccess(int newTotalPoint) {
                        mainHandler.post(() -> {
                            // 强制更新控件（核心修复）
                            updateTaskButtonStyle(btnTask3, true);
                            tvScore.setText(String.valueOf(newTotalPoint));
                            currentScore = newTotalPoint;
                            Toast.makeText(mContext, "完成任务：发布环保动态，积分+15", Toast.LENGTH_SHORT).show();
                            checkDynamicFirstAchievement();
                            // 刷新成就徽章
                            updateAchievementBadges();
                        });
                    }

                    @Override
                    public void onCompleteFailed(Exception e) {
                        Log.e("TaskFragment", "完成任务3失败：", e);
                        // 降级处理：直接加积分+更新UI
                        dbUtil.addPointsAsync(TASK3_SCORE, new DBUtil.PointQueryCallback() {
                            @Override
                            public void onQuerySuccess(int newTotalPoint) {
                                mainHandler.post(() -> {
                                    tvScore.setText(String.valueOf(newTotalPoint));
                                    currentScore = newTotalPoint;
                                    updateTaskButtonStyle(btnTask3, true);
                                    Toast.makeText(mContext, "发布动态积分+30（降级发放）", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onQueryFailed(Exception e) {
                                Log.e("TaskFragment", "降级发放任务3积分失败：", e);
                            }
                        });
                    }
                });
                break;
            }
        }
    }


    private void updateTask2And3Buttons() {
        if (taskList == null) return;

        mainHandler.post(() -> {
            for (Task task : taskList) {
                switch (task.getTitle()) {
                    case TASK2_TITLE:
                        updateTaskButtonStyle(btnTask2, task.isCompleted());
                        break;
                    case TASK3_TITLE:
                        updateTaskButtonStyle(btnTask3, task.isCompleted());
                        break;
                }
            }
            // 强制刷新布局
            if (btnTask2 != null) btnTask2.requestLayout();
            if (btnTask3 != null) btnTask3.requestLayout();
        });
    }

    // ========== 以下为保留的核心逻辑（已修复） ==========
    private void autoCheckWeekTaskAndGiveReward() {
        if (isWeekTaskRewardGiven) return;

        if (weekCarbonReduce >= WEEK_CARBON_TARGET) {
            dbUtil.addPointsAsync(WEEK_TASK_SCORE, new DBUtil.PointQueryCallback() {
                @Override
                public void onQuerySuccess(int newTotalPoint) {
                    isWeekTaskRewardGiven = true;
                    weekTaskSp.edit().putBoolean(KEY_WEEK_TASK_COMPLETED, true).apply();

                    mainHandler.post(() -> {
                        currentScore = newTotalPoint;
                        tvScore.setText(String.valueOf(newTotalPoint));
                        initWeekTaskButton();
                        updateWeekCarbonProgress();
                        Toast.makeText(mContext, "本周碳减排达标！积分+50", Toast.LENGTH_LONG).show();
                        checkWeekTaskAchievement();
                    });
                }

                @Override
                public void onQueryFailed(Exception e) {
                    Toast.makeText(mContext, "发放每周奖励失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateWeekCarbonProgress() {
        if (pbWeekTask == null || tvWeekTaskProgress == null || tvWeekCarbonReduce == null) return;

        mainHandler.post(() -> {
            String progressText = String.format("当前进度：%.1f/%.1fkg CO₂", weekCarbonReduce, WEEK_CARBON_TARGET);
            String reduceText = String.format("本周已减少：%.1fkg CO₂", weekCarbonReduce);
            tvWeekTaskProgress.setText(progressText);
            tvWeekCarbonReduce.setText(reduceText);

            int progress = (int) Math.min(weekCarbonReduce * 100, WEEK_CARBON_TARGET * 100);
            pbWeekTask.setProgress(progress);

            pbWeekTask.invalidate();
            tvWeekTaskProgress.invalidate();
            tvWeekCarbonReduce.invalidate();

            Log.d("TaskFragment", "周进度已更新：" + progress + "/500，减排量=" + weekCarbonReduce + "kg");
        });
    }

    private void checkWeekTaskCompletion() {
        boolean isCompleted = weekCarbonReduce >= WEEK_CARBON_TARGET;
        if (isCompleted && !weekTaskSp.getBoolean(KEY_WEEK_TASK_COMPLETED, false)) {
            weekTaskSp.edit().putBoolean(KEY_WEEK_TASK_COMPLETED, true).apply();
            initWeekTaskButton();
        }
    }

    private void initWeekTaskButton() {
        checkWeekReset();
        boolean isWeekTaskCompleted = weekTaskSp.getBoolean(KEY_WEEK_TASK_COMPLETED, false);

        mainHandler.post(() -> {
            if (btnWeekTask == null) return;

            if (isWeekTaskCompleted) {
                btnWeekTask.setText("本周任务已完成");
                btnWeekTask.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.primary_green));
                btnWeekTask.setTextColor(ContextCompat.getColorStateList(mContext, R.color.white));
            } else if (weekCarbonReduce >= WEEK_CARBON_TARGET) {
                btnWeekTask.setText("本周任务达标");
                btnWeekTask.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.orange));
                btnWeekTask.setTextColor(ContextCompat.getColorStateList(mContext, R.color.white));
            } else {
                btnWeekTask.setText("进行中");
                btnWeekTask.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.primary_green_light));
                btnWeekTask.setTextColor(ContextCompat.getColorStateList(mContext, R.color.primary_green));
            }
            btnWeekTask.invalidate();
            btnWeekTask.requestLayout();
            btnWeekTask.setEnabled(false);
        });
    }

    private void checkWeekReset() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int savedWeek = weekTaskSp.getInt(KEY_CURRENT_WEEK, -1);

        if (currentWeek != savedWeek) {
            weekCarbonReduce = 0.0f;
            isWeekTaskRewardGiven = false;
            isTask1Done = false;
            weekTaskSp.edit()
                    .putBoolean(KEY_WEEK_TASK_COMPLETED, false)
                    .putInt(KEY_CURRENT_WEEK, currentWeek)
                    .apply();

            mainHandler.post(() -> {
                updateWeekCarbonProgress();
                updateTask1ButtonUI(false);
                initWeekTaskButton();
                Toast.makeText(mContext, "新的一周开始啦，任务已重置！", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadScore() {
        dbUtil.getCurrentTotalPointAsync(new DBUtil.PointQueryCallback() {
            @Override
            public void onQuerySuccess(int totalPoint) {
                currentScore = totalPoint;
                mainHandler.post(() -> tvScore.setText(String.valueOf(totalPoint)));
            }

            @Override
            public void onQueryFailed(Exception e) {
                mainHandler.post(() -> tvScore.setText("0"));
                e.printStackTrace();
            }
        });
    }

    private void loadAchievements() {
        dbUtil.getAllAchievementsAsync(new DBUtil.AchievementQueryCallback() {
            @Override
            public void onQuerySuccess(List<Achievement> achievements) {
                achievementList = achievements;
                mainHandler.post(() -> updateAchievementBadges());
            }

            @Override
            public void onQueryFailed(Exception e) {
                Toast.makeText(mContext, "加载成就失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void updateTaskButtonStyle(Button button, boolean isDone) {
        if (button == null) return;

        mainHandler.post(() -> {
            if (isDone) {
                button.setText("已完成");
                button.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.primary_green));
                button.setTextColor(ContextCompat.getColorStateList(mContext, R.color.white));
            } else {
                button.setText("未完成");
                button.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.gray_light));
                button.setTextColor(ContextCompat.getColorStateList(mContext, R.color.gray_dark));
            }
            button.invalidate();
            button.requestLayout();
        });
    }

    private void updateAchievementBadges() {
        if (achievementList == null) return;

        mainHandler.post(() -> {
            for (Achievement achievement : achievementList) {
                switch (achievement.getConditionType()) {
                    case ACHIEVEMENT1_TYPE:
                        updateAchievementBadge(ivAchievement1, tvAchievement1, achievement.isUnlocked());
                        break;
                    case ACHIEVEMENT2_TYPE:
                        updateAchievementBadge(ivAchievement2, tvAchievement2, achievement.isUnlocked());
                        if (!achievement.isUnlocked()) {
                            Toast.makeText(mContext, "分类达人进度：" + garbageQueryCount + "/10次", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case ACHIEVEMENT3_TYPE:
                        updateAchievementBadge(ivAchievement3, tvAchievement3, achievement.isUnlocked());
                        if (!achievement.isUnlocked() && currentScore > 0) {
                            Toast.makeText(mContext, "积分小富翁进度：" + currentScore + "/100分", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case ACHIEVEMENT4_TYPE:
                        updateAchievementBadge(ivAchievement4, tvAchievement4, achievement.isUnlocked());
                        break;
                    case ACHIEVEMENT5_TYPE:
                        updateAchievementBadge(ivAchievement5, tvAchievement5, achievement.isUnlocked());
                        if (!achievement.isUnlocked() && dynamicPublishCount > 0) {
                            Toast.makeText(mContext, "动态发布者进度：" + dynamicPublishCount + "/1次", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        });
    }

    private void updateAchievementBadge(ImageView iv, TextView tv, boolean isUnlocked) {
        if (iv == null || tv == null) return;

        if (isUnlocked) {
            iv.setColorFilter(ContextCompat.getColor(mContext, R.color.primary_green));
            tv.setTextColor(ContextCompat.getColor(mContext, R.color.gray_dark));
        } else {
            iv.setColorFilter(ContextCompat.getColor(mContext, R.color.gray_light));
            tv.setTextColor(ContextCompat.getColor(mContext, R.color.gray_medium));
        }
        iv.invalidate();
        tv.invalidate();
    }

    private void checkCarbonFirstAchievement() {
        dbUtil.checkCarbonFirstAchievement(new DBUtil.AchievementUnlockCallback() {
            @Override
            public void onUnlockSuccess(String title, int reward, int newScore) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "解锁成就：" + title + "！积分+" + reward, Toast.LENGTH_LONG).show();
                    tvScore.setText(String.valueOf(newScore));
                    updateAchievementBadges();
                });
            }

            @Override
            public void onUnlockFailed(Exception e) {}
        });
    }

    private void checkGarbage10Achievement() {
        dbUtil.checkGarbage10Achievement(garbageQueryCount + 1, new DBUtil.AchievementUnlockCallback() {
            @Override
            public void onUnlockSuccess(String title, int reward, int newScore) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "解锁成就：" + title + "！积分+" + reward, Toast.LENGTH_LONG).show();
                    tvScore.setText(String.valueOf(newScore));
                    updateAchievementBadges();
                });
            }

            @Override
            public void onUnlockFailed(Exception e) {}
        });
    }

    private void checkDynamicFirstAchievement() {
        dbUtil.checkDynamicFirstAchievement(new DBUtil.AchievementUnlockCallback() {
            @Override
            public void onUnlockSuccess(String title, int reward, int newScore) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "解锁成就：" + title + "！积分+" + reward, Toast.LENGTH_LONG).show();
                    tvScore.setText(String.valueOf(newScore));
                    updateAchievementBadges();
                });
            }

            @Override
            public void onUnlockFailed(Exception e) {}
        });
    }

    private void checkWeekTaskAchievement() {
        dbUtil.checkWeekTaskAchievement(new DBUtil.AchievementUnlockCallback() {
            @Override
            public void onUnlockSuccess(String title, int reward, int newScore) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "解锁成就：" + title + "！积分+" + reward, Toast.LENGTH_LONG).show();
                    tvScore.setText(String.valueOf(newScore));
                    updateAchievementBadges();
                });
            }

            @Override
            public void onUnlockFailed(Exception e) {}
        });
    }

    // ========== 内存管理 ==========
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvScore = null;
        btnTask1 = btnTask2 = btnTask3 = btnWeekTask = null;
        tvWeekTaskProgress = tvWeekCarbonReduce = null;
        pbWeekTask = null;
        ivAchievement1 = ivAchievement2 = ivAchievement3 = ivAchievement4 = ivAchievement5 = null;
        tvAchievement1 = tvAchievement2 = tvAchievement3 = tvAchievement4 = tvAchievement5 = tvAchievementTips = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        dbUtil = null;
        mainHandler = null;
    }
}