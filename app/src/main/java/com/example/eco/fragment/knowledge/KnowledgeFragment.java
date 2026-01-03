package com.example.eco.fragment.knowledge;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.eco.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class KnowledgeFragment extends Fragment {

    private static final String[] TITLES = {"低碳生活", "垃圾分类", "节能减排", "自然保护"};
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SharedPreferences sp;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sp = context.getSharedPreferences("eco_data", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        viewPager.setAdapter(new KnowledgeAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(TITLES[position])).attach();

        checkFirstRead();

        return view;
    }

    private void checkFirstRead() {
        boolean hasRead = sp.getBoolean("knowledge_read", false);
        if (!hasRead) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("knowledge_read", true);
            int score = sp.getInt("user_score", 120) + 5;
            editor.putInt("user_score", score);
            editor.apply();
            Toast.makeText(getContext(), "首次阅读科普文章，获得5积分！", Toast.LENGTH_SHORT).show();
        }
    }

    private static class KnowledgeAdapter extends FragmentStateAdapter {
        public KnowledgeAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new ArticleListFragment(); // 无需传递标题，直接返回列表Fragment
        }

        @Override
        public int getItemCount() {
            return TITLES.length;
        }
    }

    // 简化的文章列表Fragment（无标题相关逻辑）
    public static class ArticleListFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_article_list, container, false);
        }
    }
}