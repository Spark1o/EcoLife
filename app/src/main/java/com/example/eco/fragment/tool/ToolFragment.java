package com.example.eco.fragment.tool;

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
import com.example.eco.fragment.garbage.GarbageClassifyFragment;
import com.example.eco.fragment.knowledge.KnowledgeFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * 环保工具Fragment：整合垃圾分类+科普知识
 */
public class ToolFragment extends Fragment {
    private static final String[] TITLES = {"垃圾分类", "环保科普"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tool, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);

        // 设置ViewPager适配器，关联垃圾分类和科普Fragment
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new GarbageClassifyFragment(); // 垃圾分类
                } else {
                    return new KnowledgeFragment(); // 环保科普
                }
            }

            @Override
            public int getItemCount() {
                return TITLES.length;
            }
        });

        // 关联TabLayout和ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(TITLES[position])).attach();

        return view;
    }
}