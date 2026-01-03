package com.example.eco.fragment.garbage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eco.R;
import com.example.eco.db.DBUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 垃圾分类Fragment优化版（联动任务+积分版）
 * 优化点：视觉样式、交互体验、功能增强、性能优化 + 任务积分联动
 */
public class GarbageClassifyFragment extends Fragment {

    // 模拟垃圾分类的本地数据
    private Map<String, String> garbageMap;
    private Map<String, Integer> typeColorMap; // 分类颜色映射

    private EditText etGarbageName;
    private Button btnSearch;
    private LinearLayout llResult;
    private TextView tvResult;

    // 全局上下文（避免getActivity()为空）
    private Context mContext;
    // 数据库工具类（新增：任务积分联动）
    private DBUtil dbUtil;
    // 主线程Handler（新增：异步回调更新UI）
    private Handler mainHandler;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        // 新增：初始化数据库工具类
        dbUtil = DBUtil.getInstance(mContext);
        // 新增：初始化主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_garbage_classify, container, false);

        // 初始化数据（优化：提前初始化，避免重复创建）
        initGarbageData();
        initTypeColorMap();

        // 初始化控件
        initView(view);

        // 初始化监听（含输入框监听）
        initListener();

        // 设置输入框焦点
        setInputFocus();

        return view;
    }

    /**
     * 初始化垃圾分类数据（优化：数据小写存储，支持模糊查询）
     */
    private void initGarbageData() {
        garbageMap = new HashMap<>();
        // 可回收物
        garbageMap.put("塑料瓶".toLowerCase(Locale.ROOT), "可回收物");
        garbageMap.put("纸箱子".toLowerCase(Locale.ROOT), "可回收物");
        garbageMap.put("易拉罐".toLowerCase(Locale.ROOT), "可回收物");
        garbageMap.put("玻璃罐".toLowerCase(Locale.ROOT), "可回收物");
        garbageMap.put("旧衣服".toLowerCase(Locale.ROOT), "可回收物");
        // 厨余垃圾
        garbageMap.put("剩米饭".toLowerCase(Locale.ROOT), "厨余垃圾");
        garbageMap.put("苹果核".toLowerCase(Locale.ROOT), "厨余垃圾");
        garbageMap.put("菜叶".toLowerCase(Locale.ROOT), "厨余垃圾");
        garbageMap.put("骨头".toLowerCase(Locale.ROOT), "厨余垃圾");
        garbageMap.put("果皮".toLowerCase(Locale.ROOT), "厨余垃圾");
        // 有害垃圾
        garbageMap.put("电池".toLowerCase(Locale.ROOT), "有害垃圾");
        garbageMap.put("灯管".toLowerCase(Locale.ROOT), "有害垃圾");
        garbageMap.put("过期药品".toLowerCase(Locale.ROOT), "有害垃圾");
        garbageMap.put("指甲油".toLowerCase(Locale.ROOT), "有害垃圾");
        garbageMap.put("油漆桶".toLowerCase(Locale.ROOT), "有害垃圾");
        // 其他垃圾
        garbageMap.put("卫生纸".toLowerCase(Locale.ROOT), "其他垃圾");
        garbageMap.put("烟头".toLowerCase(Locale.ROOT), "其他垃圾");
        garbageMap.put("塑料袋".toLowerCase(Locale.ROOT), "其他垃圾");
        garbageMap.put("一次性筷子".toLowerCase(Locale.ROOT), "其他垃圾");
        garbageMap.put("陶瓷碗".toLowerCase(Locale.ROOT), "其他垃圾");
    }

    /**
     * 初始化分类颜色映射（不同分类不同颜色，增强视觉区分）
     */
    private void initTypeColorMap() {
        typeColorMap = new HashMap<>();
        typeColorMap.put("可回收物", R.color.recyclable);
        typeColorMap.put("厨余垃圾", R.color.kitchen_waste);
        typeColorMap.put("有害垃圾", R.color.hazardous_waste);
        typeColorMap.put("其他垃圾", R.color.other_waste);
    }

    /**
     * 初始化控件（核心：视觉样式优化）
     */
    private void initView(View view) {
        etGarbageName = view.findViewById(R.id.et_garbage_name);
        btnSearch = view.findViewById(R.id.btn_search);
        llResult = view.findViewById(R.id.ll_result);
        tvResult = view.findViewById(R.id.tv_result);

        // ========== 输入框优化 ==========
        // 1. 提示文字和样式
        etGarbageName.setHint("请输入垃圾名称（如：塑料瓶）");
        etGarbageName.setHintTextColor(ContextCompat.getColor(mContext, R.color.gray_medium));
        etGarbageName.setTextColor(ContextCompat.getColor(mContext, R.color.gray_dark));
        etGarbageName.setTextSize(16f);

        // 2. 自定义背景（圆角边框）
        etGarbageName.setBackground(ContextCompat.getDrawable(mContext, R.drawable.et_bg));
        etGarbageName.setPadding(dp2px(12), dp2px(12), dp2px(12), dp2px(12));

        // 3. 输入类型优化
        etGarbageName.setInputType(EditorInfo.TYPE_CLASS_TEXT);

        // ========== 按钮优化 ==========
        btnSearch.setBackground(ContextCompat.getDrawable(mContext, R.drawable.btn_bg));
        btnSearch.setTextColor(ContextCompat.getColor(mContext, R.color.white));
        btnSearch.setTextSize(16f);
        btnSearch.setPadding(dp2px(20), dp2px(12), dp2px(20), dp2px(12));

        // ========== 结果区域优化 ==========
        llResult.setVisibility(View.GONE); // 初始隐藏
        llResult.setPadding(dp2px(12), dp2px(12), dp2px(12), dp2px(12));
        llResult.setBackground(ContextCompat.getDrawable(mContext, R.drawable.result_bg));
        tvResult.setTextSize(16f);
    }

    /**
     * 初始化监听（增强交互体验）
     */
    private void initListener() {
        // 输入框文字变化监听（实时恢复样式）
        etGarbageName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入非空时恢复输入框默认样式
                if (s.length() > 0) {
                    etGarbageName.setBackground(ContextCompat.getDrawable(mContext, R.drawable.et_bg));
                }
                // 输入时隐藏结果区域
                llResult.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 回车搜索（优化交互）
        etGarbageName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                btnSearch.performClick();
                return true;
            }
            return false;
        });

        // 搜索按钮点击监听
        btnSearch.setOnClickListener(v -> {
            // 收起软键盘
            hideSoftKeyboard(v);

            String garbageName = etGarbageName.getText().toString().trim();

            // 校验输入
            if (!validateInput(garbageName)) {
                return;
            }

            // 查询分类（优化：忽略大小写）
            queryGarbageType(garbageName);
        });
    }

    /**
     * 输入校验（增强错误提示）
     */
    private boolean validateInput(String garbageName) {
        if (garbageName.isEmpty()) {
            Toast.makeText(mContext, "请输入垃圾名称", Toast.LENGTH_SHORT).show();
            etGarbageName.setBackground(ContextCompat.getDrawable(mContext, R.drawable.et_bg_error));
            etGarbageName.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * 查询垃圾分类（核心业务逻辑 + 新增任务积分联动）
     */
    private void queryGarbageType(String garbageName) {
        // 转换为小写，支持大小写不敏感查询
        String key = garbageName.toLowerCase(Locale.ROOT);
        String type = garbageMap.get(key);

        llResult.setVisibility(View.VISIBLE);
        if (type != null) {
            // 找到结果：显示分类并设置对应颜色
            tvResult.setText(garbageName + " → " + type);
            int colorRes = typeColorMap.get(type);
            tvResult.setTextColor(ContextCompat.getColor(mContext, colorRes));

            // ========== 新增：查询成功后联动任务+积分 ==========
            completeGarbageTaskAndAddScore();
        } else {
            // 未找到结果：友好提示
            tvResult.setText("未查询到「" + garbageName + "」的分类信息");
            tvResult.setTextColor(ContextCompat.getColor(mContext, R.color.gray_medium));
        }
    }

    /**
     * 新增：完成垃圾分类查询任务并增加积分
     */
    private void completeGarbageTaskAndAddScore() {
        // 异步完成"查询1次垃圾分类"任务
        dbUtil.completeGarbageTaskAfterQueryAsync(new DBUtil.TaskCompleteCallback() {
            @Override
            public void onCompleteSuccess(int newTotalPoint) {
                // 主线程更新UI，提示积分增加
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "完成任务！积分+5，当前总积分：" + newTotalPoint, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCompleteFailed(Exception e) {
                // 任务完成失败（如任务已完成、数据库异常），轻量提示
                mainHandler.post(() -> {
                    // 仅在日志打印异常，用户侧只提示"任务已完成"（避免干扰核心功能）
                    if (e.getMessage().contains("已完成")) {
                        Toast.makeText(mContext, "该任务已完成，无需重复领取积分", Toast.LENGTH_SHORT).show();
                    } else {
                        e.printStackTrace();
                    }
                });
            }
        });
        // 1. 记录查询次数（SP临时存储）
        SharedPreferences sp = mContext.getSharedPreferences("eco_data", Context.MODE_PRIVATE);
        int queryCount = sp.getInt("garbage_query_count", 0) + 1;
        sp.edit().putInt("garbage_query_count", queryCount).apply();

// 2. 检测「分类达人」成就
        dbUtil.checkGarbage10Achievement(queryCount, new DBUtil.AchievementUnlockCallback() {
            @Override
            public void onUnlockSuccess(String achievementTitle, int rewardPoint, int newTotalPoint) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "解锁成就：" + achievementTitle + "！积分+" + rewardPoint + "，总积分：" + newTotalPoint, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onUnlockFailed(Exception e) {
                e.printStackTrace();
            }
        });

// 3. 检测「积分小富翁」成就
        dbUtil.checkPoint100Achievement(new DBUtil.AchievementUnlockCallback() {
            @Override
            public void onUnlockSuccess(String achievementTitle, int rewardPoint, int newTotalPoint) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "解锁成就：" + achievementTitle + "！积分+" + rewardPoint + "，总积分：" + newTotalPoint, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onUnlockFailed(Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 设置输入框焦点
     */
    private void setInputFocus() {
        etGarbageName.requestFocus();
    }

    /**
     * 隐藏软键盘
     */
    private void hideSoftKeyboard(View view) {
        if (mContext == null) return;
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 工具方法：dp转px
     */
    private int dp2px(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 防止内存泄漏（新增：释放数据库和Handler）
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        etGarbageName = null;
        btnSearch = null;
        llResult = null;
        tvResult = null;
        garbageMap.clear();
        typeColorMap.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 新增：移除Handler所有回调，防止内存泄漏
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        // 新增：释放数据库和Handler引用
        dbUtil = null;
        mainHandler = null;
    }
}