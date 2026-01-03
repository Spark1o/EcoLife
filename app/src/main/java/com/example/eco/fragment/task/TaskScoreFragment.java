package com.example.eco.fragment.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.eco.R;
import com.example.eco.fragment.shop.ShopFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


public class TaskScoreFragment extends Fragment {
    private static final String[] TITLES = {"挑战任务", "积分兑换"};

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_score, container, false);

        initView(view);
        initViewPager();

        return view;
    }

    private void initView(View view) {
        if (view == null) return;
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
    }

    private void initViewPager() {
        if (viewPager == null || getActivity() == null) return;

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new TaskFragment(); // 挑战任务（含内置成就徽章）
                    case 1:
                        return new ShopFragment(); // 积分兑换（保持不变）
                    default:
                        return new TaskFragment();
                }
            }

            @Override
            public int getItemCount() {
                return TITLES.length; // 仅返回2个Tab
            }
        });

        // TabLayout与ViewPager绑定
        if (tabLayout != null) {
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                if (position >= 0 && position < TITLES.length) {
                    tab.setText(TITLES[position]);
                }
            }).attach();
        }

        // 设置预加载页数（优化切换体验）
        viewPager.setOffscreenPageLimit(TITLES.length);
    }

    /**
     * 优化内存管理
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tabLayout = null;
        if (viewPager != null) {
            viewPager.setAdapter(null);
            viewPager = null;
        }
    }
}