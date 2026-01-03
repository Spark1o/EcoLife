package com.example.eco.fragment.mine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.eco.R;
import com.example.eco.db.DBUtil;
import com.example.eco.db.entity.Achievement;
import com.example.eco.db.entity.Task;
import com.example.eco.fragment.community.CommunityFragment;

import java.util.List;

public class MyFragment extends Fragment {

    // 控件
    private TextView tvTotalScore;
    private TextView tvCompletedTasks;
    private TextView tvUnlockedAchievements;
    private TextView tvGarbageQueryCount;
    private TextView tvCarbonFootprintCount;
    private ImageView ivRefresh;
    private Button btnRefreshData;
    private Button btnEnterCommunity;

    // 数据
    private DBUtil dbUtil;
    private Handler mainHandler;
    private Context mContext;

    // Fragment容器ID（请修改为你项目中实际的容器ID）
    private static final int FRAGMENT_CONTAINER_ID = R.id.content_container;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        dbUtil = DBUtil.getInstance(mContext);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my, container, false);

        initViews(view);
        setClickListeners();
        loadUserData();

        return view;
    }

    private void initViews(View view) {
        // 绑定原有控件
        tvTotalScore = view.findViewById(R.id.tv_my_total_score);
        tvCompletedTasks = view.findViewById(R.id.tv_my_completed_tasks);
        tvUnlockedAchievements = view.findViewById(R.id.tv_my_unlocked_achievements);
        tvGarbageQueryCount = view.findViewById(R.id.tv_my_garbage_query_count);
        tvCarbonFootprintCount = view.findViewById(R.id.tv_my_carbon_count);
        ivRefresh = view.findViewById(R.id.iv_refresh);
        btnRefreshData = view.findViewById(R.id.btn_refresh_data);

        // 绑定进入社区按钮
        btnEnterCommunity = view.findViewById(R.id.btn_enter_community);

        // 初始占位符
        tvTotalScore.setText("--");
        tvCompletedTasks.setText("--");
        tvUnlockedAchievements.setText("--");
        tvGarbageQueryCount.setText("--");
        tvCarbonFootprintCount.setText("--");
    }

    private void setClickListeners() {
        // 圆形刷新按钮
        ivRefresh.setOnClickListener(v -> refreshData());

        // 底部刷新按钮
        btnRefreshData.setOnClickListener(v -> refreshData());

        // 进入社区按钮点击事件（无动画）
        btnEnterCommunity.setOnClickListener(v -> enterCommunity());
    }

    // 进入社区逻辑（移除所有动画相关代码）
    private void enterCommunity() {
        // 1. 校验Activity是否存在
        if (getActivity() == null) {
            Toast.makeText(mContext, "当前页面状态异常，无法进入社区", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 校验Fragment容器ID是否有效
        View containerView = getActivity().findViewById(FRAGMENT_CONTAINER_ID);
        if (containerView == null) {
            Toast.makeText(mContext, "未找到Fragment容器，请检查布局ID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 3. 创建CommunityFragment实例
            CommunityFragment communityFragment = new CommunityFragment();

            // 4. 执行Fragment切换（无动画）
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            // 仅保留核心替换逻辑，移除动画
            transaction.replace(FRAGMENT_CONTAINER_ID, communityFragment);
            // 添加到返回栈
            transaction.addToBackStack("CommunityFragment");
            // 容错提交
            transaction.commitAllowingStateLoss();

            Toast.makeText(mContext, "进入环保社区", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(mContext, "进入社区失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        loadTotalScore();
        loadCompletedTasksCount();
        loadUnlockedAchievementsCount();
        loadGarbageQueryCount();
        loadCarbonFootprintCount();
    }

    private void loadTotalScore() {
        dbUtil.getCurrentTotalPointAsync(new DBUtil.PointQueryCallback() {
            @Override
            public void onQuerySuccess(int totalPoint) {
                final int score = totalPoint;
                mainHandler.post(() -> tvTotalScore.setText(String.valueOf(score)));
            }

            @Override
            public void onQueryFailed(Exception e) {
                mainHandler.post(() -> {
                    tvTotalScore.setText("0");
                    Toast.makeText(mContext, "加载积分失败", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        });
    }

    private void loadCompletedTasksCount() {
        dbUtil.getAllTasksAsync(new DBUtil.TaskQueryCallback() {
            @Override
            public void onQuerySuccess(List<Task> tasks) {
                int completedCount = 0;
                for (Task task : tasks) {
                    if (task.isCompleted()) completedCount++;
                }

                final int finalCompletedCount = completedCount;
                final int totalTaskCount = tasks.size();

                mainHandler.post(() ->
                        tvCompletedTasks.setText(finalCompletedCount + "/" + totalTaskCount)
                );
            }

            @Override
            public void onQueryFailed(Exception e) {
                mainHandler.post(() -> tvCompletedTasks.setText("0/0"));
                e.printStackTrace();
            }
        });
    }

    private void loadUnlockedAchievementsCount() {
        dbUtil.getAllAchievementsAsync(new DBUtil.AchievementQueryCallback() {
            @Override
            public void onQuerySuccess(List<Achievement> achievements) {
                int unlockedCount = 0;
                for (Achievement achievement : achievements) {
                    if (achievement.isUnlocked()) unlockedCount++;
                }

                final int finalUnlockedCount = unlockedCount;
                final int totalAchievementCount = achievements.size();

                mainHandler.post(() ->
                        tvUnlockedAchievements.setText(finalUnlockedCount + "/" + totalAchievementCount)
                );
            }

            @Override
            public void onQueryFailed(Exception e) {
                mainHandler.post(() -> tvUnlockedAchievements.setText("0/0"));
                e.printStackTrace();
            }
        });
    }

    private void loadGarbageQueryCount() {
        new Thread(() -> {
            final int count = mContext.getSharedPreferences("eco_data", Context.MODE_PRIVATE)
                    .getInt("garbage_query_count", 0);
            mainHandler.post(() -> tvGarbageQueryCount.setText(String.valueOf(count)));
        }).start();
    }

    private void loadCarbonFootprintCount() {
        dbUtil.getCarbonFootprintCountAsync(new DBUtil.CarbonCountCallback() {
            @Override
            public void onQuerySuccess(int count) {
                final int finalCount = count;
                mainHandler.post(() -> tvCarbonFootprintCount.setText(String.valueOf(finalCount)));
            }

            @Override
            public void onQueryFailed(Exception e) {
                mainHandler.post(() -> tvCarbonFootprintCount.setText("0"));
                e.printStackTrace();
            }
        });
    }

    public void refreshData() {
        // 刷新动画
        ivRefresh.animate().rotation(360).setDuration(500).start();

        // 重置占位符
        tvTotalScore.setText("--");
        tvCompletedTasks.setText("--");
        tvUnlockedAchievements.setText("--");
        tvGarbageQueryCount.setText("--");
        tvCarbonFootprintCount.setText("--");

        // 重新加载
        loadUserData();

        Toast.makeText(mContext, "正在刷新数据...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvTotalScore = null;
        tvCompletedTasks = null;
        tvUnlockedAchievements = null;
        tvGarbageQueryCount = null;
        tvCarbonFootprintCount = null;
        ivRefresh = null;
        btnRefreshData = null;
        btnEnterCommunity = null;
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