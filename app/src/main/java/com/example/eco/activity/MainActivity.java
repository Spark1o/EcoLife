package com.example.eco.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.eco.R;
import com.example.eco.fragment.carbon.CarbonFootprintFragment;
import com.example.eco.fragment.history.CarbonHistoryFragment; // 新增：导入历史记录Fragment
import com.example.eco.fragment.task.TaskScoreFragment;
import com.example.eco.fragment.tool.ToolFragment;
import com.example.eco.fragment.mine.MyFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// 移除了无用的CarbonFootprint导入，仅保留必要依赖
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private Fragment currentFragment;

    // 导航项ID常量
    private int NAV_CARBON = R.id.nav_carbon;
    private int NAV_TOOL = R.id.nav_tool;
    private int NAV_TASK = R.id.nav_task;
    private int NAV_MINE = R.id.nav_mine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();
        bottomNavigationView = findViewById(R.id.bottom_nav);

        // 初始化默认Fragment（碳足迹）
        if (savedInstanceState == null) {
            replaceFragment(new CarbonFootprintFragment(), false);
            bottomNavigationView.setSelectedItemId(R.id.nav_carbon);
        }

        // 底部导航监听
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment = null;

            if (itemId == NAV_CARBON) {
                targetFragment = new CarbonFootprintFragment();
            } else if (itemId == NAV_TOOL) {
                targetFragment = new ToolFragment();
            } else if (itemId == NAV_TASK) {
                targetFragment = new TaskScoreFragment();
            } else if (itemId == NAV_MINE) {
                targetFragment = new MyFragment();
            }

            // 切换主Fragment时清空返回栈
            if (targetFragment != null && currentFragment != targetFragment) {
                fragmentManager.popBackStackImmediate();
                replaceFragment(targetFragment, false);
                return true;
            }
            return false;
        });
    }

    // 重载replaceFragment方法：支持是否加入返回栈
    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out);
        transaction.replace(R.id.content_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        if (!fragmentManager.isStateSaved()) {
            transaction.commit();
        } else {
            transaction.commitAllowingStateLoss();
        }

        currentFragment = fragment;
    }

    // ========== 核心新增：打开历史记录Fragment的方法 ==========
    public void openCarbonHistoryFragment() {
        CarbonHistoryFragment historyFragment = CarbonHistoryFragment.newInstance();
        replaceFragment(historyFragment, true); // 加入返回栈，支持返回
    }

    // ========== 修复返回键逻辑（修正方法名拼写） ==========
    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    // ========== 删除无效代码：未实现接口的onEditCarbon方法 ==========
    // （编辑功能后续需要时，再配合接口实现，现在先删除避免冗余）
}