package com.example.eco.fragment.community;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.eco.R;
import com.example.eco.fragment.publish.PublishFragment;
import com.example.eco.db.entity.Dynamic;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 社区Fragment（支持加载发布的新动态 + 保留初始默认动态）
 */
public class CommunityFragment extends Fragment {

    private Context mContext;
    private LinearLayout llPublishEntry;
    private LinearLayout llDynamicList; // 动态列表容器
    private View emptyView;             // 空状态视图

    // SP相关
    private static final String SP_NAME = "dynamic_data";
    private static final String KEY_DYNAMIC_LIST = "dynamic_list";

    // 模拟初始数据（固定显示的默认动态，不会被覆盖）
    private List<Dynamic> defaultDynamicList = new ArrayList<Dynamic>() {{
        add(new Dynamic("环保小达人", "2小时前", "今天用洗菜水浇花，不仅节约了1桶水，花草也长得更茂盛了～🌿", 28, false));
        add(new Dynamic("低碳生活家", "昨天 18:30", "本周累计减少10kgCO₂排放，通勤全部选择骑行+公交，为地球减负，也收获了健康～🚲", 45, true));
        // 可选：添加更多默认动态
        // add(new Dynamic("绿色守护者", "3天前", "周末捡垃圾，小小的行动也能让环境变更好～♻️", 66, false));
    }};

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        // 初始化控件
        llPublishEntry = view.findViewById(R.id.ll_publish_entry);
        llDynamicList = view.findViewById(R.id.ll_dynamic_list);
        emptyView = view.findViewById(R.id.empty_view);

        // 加载动态列表（核心：默认动态 + 发布的动态）
        loadDynamicList();

        // 初始化监听
        initListener(view);

        return view;
    }

    /**
     * 核心优化：加载动态列表（发布的动态在顶部，默认动态在下方）
     */
    private void loadDynamicList() {
        // 1. 清空原有列表
        llDynamicList.removeAllViews();

        // 2. 最终显示的列表 = 发布的动态 + 默认动态
        List<Dynamic> finalDynamicList = new ArrayList<>();

        // 3. 读取本地存储的发布动态
        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sp.getString(KEY_DYNAMIC_LIST, "");
        Type type = new TypeToken<List<Dynamic>>() {}.getType();
        List<Dynamic> publishDynamicList = gson.fromJson(json, type);

        // 4. 如果有发布的动态，先添加到最终列表（新发布的在顶部）
        if (publishDynamicList != null && !publishDynamicList.isEmpty()) {
            finalDynamicList.addAll(publishDynamicList);
        }

        // 5. 再添加默认动态（确保始终显示初始的几条）
        finalDynamicList.addAll(defaultDynamicList);

        // 6. 显示空状态/加载动态
        if (finalDynamicList.isEmpty()) {
            llDynamicList.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            llDynamicList.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            // 逐个添加动态项到列表
            for (int i = 0; i < finalDynamicList.size(); i++) {
                addDynamicItem(finalDynamicList.get(i), i);
            }
        }
    }

    /**
     * 添加单个动态项到列表（优化点赞状态保存逻辑）
     */
    private void addDynamicItem(Dynamic dynamic, int position) {
        // 加载动态项布局（必须确保item_dynamic.xml存在且ID匹配）
        View dynamicView = LayoutInflater.from(mContext).inflate(R.layout.item_dynamic, llDynamicList, false);

        // 赋值用户信息
        TextView tvUsername = dynamicView.findViewById(R.id.tv_username);
        TextView tvTime = dynamicView.findViewById(R.id.tv_time);
        TextView tvContent = dynamicView.findViewById(R.id.tv_content);
        tvUsername.setText(dynamic.getUsername());
        tvTime.setText(dynamic.getShowTime());
        tvContent.setText(dynamic.getContent());

        // 赋值点赞区域
        LinearLayout llLike = dynamicView.findViewById(R.id.ll_like);
        ImageView ivLikeIcon = dynamicView.findViewById(R.id.iv_like_icon);
        TextView tvLikeCount = dynamicView.findViewById(R.id.tv_like_count);
        tvLikeCount.setText(" " + dynamic.getLikeCount());

        // 设置点赞初始状态
        if (dynamic.isLiked()) {
            ivLikeIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.primary_green));
        } else {
            ivLikeIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.gray_medium));
        }

        // 点赞点击事件（优化：实时更新并保存）
        llLike.setOnClickListener(v -> {
            boolean isLiked = dynamic.isLiked();
            if (isLiked) {
                // 取消点赞
                dynamic.setLikeCount(dynamic.getLikeCount() - 1);
                dynamic.setLiked(false);
                ivLikeIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.gray_medium));
                Toast.makeText(mContext, "已取消点赞", Toast.LENGTH_SHORT).show();
            } else {
                // 点赞
                dynamic.setLikeCount(dynamic.getLikeCount() + 1);
                dynamic.setLiked(true);
                ivLikeIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.primary_green));
                Toast.makeText(mContext, "点赞成功", Toast.LENGTH_SHORT).show();
            }
            tvLikeCount.setText(" " + dynamic.getLikeCount());

            // 修复：保存更新后的完整列表（包括默认+发布）
            saveFinalDynamicListToSP();
        });

        // 添加到列表
        llDynamicList.addView(dynamicView);
    }

    /**
     * 新增：保存最终的完整列表（发布+默认）到SP
     */
    private void saveFinalDynamicListToSP() {
        // 重新构建最终列表（发布+默认）
        List<Dynamic> finalDynamicList = new ArrayList<>();

        // 读取发布的动态
        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sp.getString(KEY_DYNAMIC_LIST, "");
        Type type = new TypeToken<List<Dynamic>>() {}.getType();
        List<Dynamic> publishDynamicList = gson.fromJson(json, type);

        if (publishDynamicList != null && !publishDynamicList.isEmpty()) {
            finalDynamicList.addAll(publishDynamicList);
        }
        finalDynamicList.addAll(defaultDynamicList);

        // 保存到SP
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_DYNAMIC_LIST, gson.toJson(finalDynamicList));
        editor.apply();
    }


    private void initListener(View view) {
        // 发布卡片跳转
        if (llPublishEntry != null) {
            llPublishEntry.setOnClickListener(v -> {
                if (getActivity() == null) return;
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_container, new PublishFragment());
                transaction.addToBackStack("CommunityFragment");
                transaction.commit();
            });
        }

        // 移除原有的initDynamicItemListener调用（无需重复处理点赞）
        // initDynamicItemListener(view);
    }

    // 页面恢复时刷新列表（发布返回后自动刷新）
    @Override
    public void onResume() {
        super.onResume();
        loadDynamicList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        llPublishEntry = null;
        llDynamicList = null;
        emptyView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }
}