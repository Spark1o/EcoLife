package com.example.eco.fragment.publish;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 发布动态Fragment
 * 功能：输入动态内容、字数统计、发布动态（本地存储）、返回社区页面
 * 修复：添加动态发布次数统计，同步到TaskFragment
 */
public class PublishFragment extends Fragment {

    // 上下文
    private Context mContext;

    // 控件
    private ImageView ivBack;
    private EditText etDynamic;
    private TextView tvWordCount;
    private Button btnPublish;

    // 最大输入长度
    private static final int MAX_CONTENT_LENGTH = 200;

    // ===== 新增：本地存储相关配置 =====
    // SharedPreferences 文件名（动态列表）
    private static final String SP_NAME = "dynamic_data";
    // 存储动态列表的key
    private static final String KEY_DYNAMIC_LIST = "dynamic_list";

    // ===== 新增：任务统计相关SP配置（和TaskFragment保持一致）=====
    private static final String SP_ECO_DATA = "eco_data";
    private static final String KEY_DYNAMIC_PUBLISH_COUNT = "dynamic_publish_count";

    // 动态数据模型（内部类）
    public static class Dynamic {
        private String username;    // 用户名
        private String time;        // 发布时间
        private String content;     // 动态内容
        private int likeCount;      // 点赞数
        private boolean isLiked;    // 是否已点赞

        public Dynamic(String username, String time, String content, int likeCount, boolean isLiked) {
            this.username = username;
            this.time = time;
            this.content = content;
            this.likeCount = likeCount;
            this.isLiked = isLiked;
        }

        // Getter & Setter
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
        public boolean isLiked() { return isLiked; }
        public void setLiked(boolean liked) { isLiked = liked; }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish, container, false);

        // 初始化控件
        initViews(view);

        // 初始化监听
        initListeners();

        // 自动弹出软键盘
        showSoftKeyboard();

        return view;
    }

    /**
     * 初始化所有控件
     */
    private void initViews(View view) {
        // 导航栏
        ivBack = view.findViewById(R.id.iv_back);

        // 输入区域
        etDynamic = view.findViewById(R.id.et_dynamic);
        tvWordCount = view.findViewById(R.id.tv_word_count);

        // 发布按钮
        btnPublish = view.findViewById(R.id.btn_publish);

        // 初始化输入框
        initEditText();
    }

    /**
     * 初始化输入框配置
     */
    private void initEditText() {
        // 设置最大输入长度
        etDynamic.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(MAX_CONTENT_LENGTH)
        });

        // 设置输入类型（多行）
        etDynamic.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        etDynamic.setSingleLine(false);

        // 初始化字数统计
        updateWordCount(0);
    }

    /**
     * 初始化所有监听事件
     */
    private void initListeners() {
        // 1. 返回按钮：返回社区页面
        ivBack.setOnClickListener(v -> {
            hideSoftKeyboard();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // 2. 输入框文字变化监听
        etDynamic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                // 更新字数统计
                updateWordCount(length);
                // 更新发布按钮状态
                updatePublishButtonState(length > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. 发布按钮点击事件
        btnPublish.setOnClickListener(v -> {
            publishDynamic();
        });

        // 4. 回车发布（Ctrl+回车）
        etDynamic.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && etDynamic.getText().length() > 0) {
                publishDynamic();
                return true;
            }
            return false;
        });
    }

    /**
     * 更新字数统计
     */
    private void updateWordCount(int length) {
        tvWordCount.setText(length + "/" + MAX_CONTENT_LENGTH);
    }

    /**
     * 更新发布按钮状态（可点击/不可点击）
     */
    private void updatePublishButtonState(boolean enable) {
        btnPublish.setEnabled(enable);
        if (enable) {
            btnPublish.setBackground(ContextCompat.getDrawable(mContext, R.drawable.btn_bg));
            btnPublish.setTextColor(ContextCompat.getColor(mContext, R.color.white));
        } else {
            btnPublish.setBackground(ContextCompat.getDrawable(mContext, R.drawable.btn_bg_disable));
            btnPublish.setTextColor(ContextCompat.getColor(mContext, R.color.gray_medium));
        }
    }

    /**
     * 发布动态核心逻辑（修复：添加任务计数统计）
     */
    private void publishDynamic() {
        // 1. 收起软键盘
        hideSoftKeyboard();

        // 2. 获取并校验输入内容
        String content = etDynamic.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(mContext, "请输入动态内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== 原有：本地存储动态列表逻辑 =====
        // 2.1 构造新动态（模拟用户名、发布时间）
        Dynamic newDynamic = new Dynamic(
                "环保爱好者",  // 可替换为真实用户名
                "刚刚",        // 发布时间
                content,       // 输入的动态内容
                0,             // 初始点赞数0
                false          // 初始未点赞
        );

        // 2.2 读取本地已保存的动态列表
        SharedPreferences spDynamic = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = spDynamic.getString(KEY_DYNAMIC_LIST, "");
        Type type = new TypeToken<List<Dynamic>>() {}.getType();
        List<Dynamic> dynamicList = gson.fromJson(json, type);
        if (dynamicList == null) {
            dynamicList = new ArrayList<>(); // 首次发布初始化列表
        }

        // 2.3 把新动态添加到列表头部（最新的在最前面）
        dynamicList.add(0, newDynamic);

        // 2.4 保存更新后的列表到本地
        SharedPreferences.Editor editorDynamic = spDynamic.edit();
        editorDynamic.putString(KEY_DYNAMIC_LIST, gson.toJson(dynamicList));
        editorDynamic.apply();

        // ===== 新增：更新动态发布次数统计（核心修复）=====
        SharedPreferences spEco = mContext.getSharedPreferences(SP_ECO_DATA, Context.MODE_PRIVATE);
        // 读取当前发布次数
        int currentCount = spEco.getInt(KEY_DYNAMIC_PUBLISH_COUNT, 0);
        // 次数+1并保存
        SharedPreferences.Editor editorEco = spEco.edit();
        editorEco.putInt(KEY_DYNAMIC_PUBLISH_COUNT, currentCount + 1);
        editorEco.apply();

        // 打印日志，便于调试
        android.util.Log.d("PublishFragment", "动态发布次数更新：" + (currentCount + 1));

        // 3. 提示发布成功（更新提示文案）
        Toast.makeText(mContext, "动态发布成功！已完成「发布环保动态」任务，积分+30", Toast.LENGTH_SHORT).show();

        // 4. 发布成功后返回社区页面
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();

            // ===== 可选：立即刷新TaskFragment（无需重启页面）=====
            try {
                // 找到TaskFragment并重新加载数据
                Fragment taskFragment = getActivity().getSupportFragmentManager().findFragmentByTag("TaskFragment");
                if (taskFragment instanceof com.example.eco.fragment.task.TaskFragment) {
                    ((com.example.eco.fragment.task.TaskFragment) taskFragment).loadAllData();
                }
            } catch (Exception e) {
                // 若找不到Fragment，不影响核心功能
                android.util.Log.d("PublishFragment", "刷新TaskFragment失败：" + e.getMessage());
            }
        }
    }

    /**
     * 显示软键盘
     */
    private void showSoftKeyboard() {
        etDynamic.requestFocus();
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etDynamic, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * 隐藏软键盘
     */
    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etDynamic.getWindowToken(), 0);
    }

    /**
     * 防止内存泄漏
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ivBack = null;
        etDynamic = null;
        tvWordCount = null;
        btnPublish = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    /**
     * dp转px工具方法
     */
    private int dp2px(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}