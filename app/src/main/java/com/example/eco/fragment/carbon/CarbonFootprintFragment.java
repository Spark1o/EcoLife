package com.example.eco.fragment.carbon;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.example.eco.R;
import com.example.eco.activity.MainActivity;
import com.example.eco.db.DBUtil;
import com.example.eco.db.entity.CarbonFootprint;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 碳足迹Fragment（全异步版 + 初始模拟数据）
 * 核心新增：首次无数据时自动插入模拟数据，让图表有初始内容
 */
public class CarbonFootprintFragment extends Fragment {
    public static CarbonFootprintFragment newInstance(CarbonFootprint editItem) {
        CarbonFootprintFragment fragment = new CarbonFootprintFragment();
        if (editItem != null) {
            Bundle args = new Bundle();
            args.putSerializable("EDIT_ITEM", (Serializable) editItem);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public static CarbonFootprintFragment newInstance() {
        return new CarbonFootprintFragment();
    }

    // 控件声明
    private LineChart lineChart;
    private PieChart pieChart;
    private EditText etType, etSubType, etValue;
    private Button btnSubmit;
    private TextView tvWeekTotalCarbon;

    // 一周的日期标签
    private final String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    // 全局上下文
    private Context mContext;
    // 数据库工具类
    private DBUtil dbUtil;
    // 主线程Handler
    private Handler mainHandler;
    // 标记：是否已插入初始模拟数据（避免重复插入）
    private boolean isInitDataInserted = false;

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
        View view = inflater.inflate(R.layout.fragment_carbon_footprint, container, false);

        // 初始化控件
        initView(view);

        // 激活历史记录图标点击事件
        ImageView ivViewHistory = view.findViewById(R.id.iv_view_history);
        ivViewHistory.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCarbonHistoryFragment();
            } else {
                Toast.makeText(mContext, "跳转失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });

        // 初始化图表
        try {
            initCharts();
            // 异步加载图表数据（新增：无数据时插入初始模拟数据）
            loadChartDataFromDBAsync();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mContext, "图表初始化失败", Toast.LENGTH_SHORT).show();
        }

        // 初始化事件监听
        initListener();

        // 设置输入框焦点
        setInputFocus();

        return view;
    }

    /**
     * 初始化控件
     */
    private void initView(View view) {
        lineChart = view.findViewById(R.id.line_chart);
        pieChart = view.findViewById(R.id.pie_chart);
        etType = view.findViewById(R.id.et_type);
        etSubType = view.findViewById(R.id.et_sub_type);
        etValue = view.findViewById(R.id.et_value);
        btnSubmit = view.findViewById(R.id.btn_submit);
        tvWeekTotalCarbon = view.findViewById(R.id.tv_week_total_carbon);

        // 输入框提示
        etType.setHint("请输入碳排放类型（如：出行）");
        etSubType.setHint("请输入子类型（如：自驾）");
        etValue.setHint("请输入数值（如：10）");
    }

    /**
     * 初始化图表基础样式
     */
    private void initCharts() {
        initLineChartStyle();
        initPieChartStyle();
    }

    /**
     * 初始化折线图样式
     */
    private void initLineChartStyle() {
        int primaryGreen = ContextCompat.getColor(mContext, R.color.primary_green);
        int primaryGreenLight = ContextCompat.getColor(mContext, R.color.primary_green_light);
        int grayDark = ContextCompat.getColor(mContext, R.color.gray_dark);
        int grayLight = ContextCompat.getColor(mContext, R.color.gray_light);

        // X轴配置
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(7);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(6f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) Math.round(value);
                if (index >= 0 && index < weekDays.length) {
                    return weekDays[index];
                }
                return "";
            }
        });

        // Y轴配置
        YAxis leftYAxis = lineChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(grayLight);
        lineChart.getAxisRight().setEnabled(false);

        // 图例配置
        Legend legend = lineChart.getLegend();
        if (legend != null) {
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
        }

        // 隐藏描述
        Description lineDesc = new Description();
        lineDesc.setEnabled(false);
        lineChart.setDescription(lineDesc);

        // 交互配置
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDoubleTapToZoomEnabled(true);
    }

    /**
     * 初始化饼图样式
     */
    private void initPieChartStyle() {
        int grayDark = ContextCompat.getColor(mContext, R.color.gray_dark);
        int white = ContextCompat.getColor(mContext, R.color.white);

        // 中心文字
        pieChart.setCenterText("品类占比");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(grayDark);

        // 隐藏描述
        Description pieDesc = new Description();
        pieDesc.setEnabled(false);
        pieChart.setDescription(pieDesc);

        // 交互配置
        pieChart.setTouchEnabled(true);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setHighlightPerTapEnabled(true);

        // 图例配置
        Legend pieLegend = pieChart.getLegend();
        if (pieLegend != null) {
            pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        }
    }

    /**
     * 异步加载图表数据（核心新增：无数据时插入初始模拟数据）
     */
    private void loadChartDataFromDBAsync() {
        // 1. 计算本周时间范围
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToMonday = (dayOfWeek == 1 ? 6 : dayOfWeek - 2);
        calendar.add(Calendar.DAY_OF_YEAR, -daysToMonday);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long weekStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_YEAR, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long weekEnd = calendar.getTimeInMillis();

        // 打印时间范围
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        android.util.Log.d("图表调试", "本周开始：" + sdf.format(new Date(weekStart)));
        android.util.Log.d("图表调试", "本周结束：" + sdf.format(new Date(weekEnd)));

        // 2. 异步查询本周数据
        dbUtil.getWeekCarbonDataAsync(weekStart, weekEnd, new DBUtil.CarbonWeekQueryCallback() {
            @Override
            public void onQuerySuccess(List<CarbonFootprint> weekData) {
                // 查询成功，处理数据
                android.util.Log.d("图表调试", "查询到本周数据条数：" + weekData.size());

                // ========== 核心新增：无数据时插入初始模拟数据 ==========
                if (weekData.isEmpty() && !isInitDataInserted) {
                    insertInitMockData(weekStart, weekEnd);
                    return; // 插入后重新加载数据
                }
                // =====================================================

                // 打印每条数据
                for (int i = 0; i < weekData.size(); i++) {
                    CarbonFootprint data = weekData.get(i);
                    android.util.Log.d("图表调试", "数据" + i + "：" +
                            "时间戳=" + data.getCreateTime() +
                            "（日期=" + sdf.format(new Date(data.getCreateTime())) + "）" +
                            "，类型=" + data.getType() +
                            "，数值=" + data.getValue());
                }

                // 统计饼图数据 + 计算本周总碳排放量
                Map<String, Double> typeSumMap = new HashMap<>();
                double weekTotalCarbon = 0.0;
                for (CarbonFootprint data : weekData) {
                    double carbonValue = dbUtil.calculateCarbon(data.getType(), data.getSubType(), data.getValue());
                    weekTotalCarbon += carbonValue;
                    typeSumMap.put(data.getType(), typeSumMap.getOrDefault(data.getType(), 0.0) + carbonValue);
                }
                android.util.Log.d("图表调试", "本周总碳排放量：" + weekTotalCarbon + " kgCO₂");

                // 主线程更新UI
                updateLineChart(weekData);
                updatePieChart(typeSumMap);
                tvWeekTotalCarbon.setText(String.format("%.2f", weekTotalCarbon));
            }

            @Override
            public void onQueryFailed(Exception e) {
                // 查询失败，提示用户
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "数据查询失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // 显示空数据
                    updateLineChart(new ArrayList<>());
                    updatePieChart(new HashMap<>());
                    tvWeekTotalCarbon.setText("0.00");
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 核心新增：插入初始模拟数据（首次无数据时调用）
     */
    private void insertInitMockData(long weekStart, long weekEnd) {
        android.util.Log.d("图表调试", "无本周数据，插入初始模拟数据");
        isInitDataInserted = true; // 标记已插入，避免重复

        // 1. 构建模拟数据（覆盖本周不同日期、不同类型）
        List<CarbonFootprint> mockDataList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // 周一：出行-自驾，数值20km
        calendar.setTimeInMillis(weekStart);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        mockDataList.add(new CarbonFootprint(
                "出行",
                "自驾",
                20.0f,
                calendar.getTimeInMillis()
        ));
        android.util.Log.d("模拟数据", "周一模拟数据：" + sdf.format(new Date(calendar.getTimeInMillis())));

        // 周三：饮食-外卖，数值3份
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        mockDataList.add(new CarbonFootprint(
                "饮食",
                "外卖",
                3.0f,
                calendar.getTimeInMillis()
        ));
        android.util.Log.d("模拟数据", "周三模拟数据：" + sdf.format(new Date(calendar.getTimeInMillis())));

        // 周五：居家-用电，数值5度
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        mockDataList.add(new CarbonFootprint(
                "居家",
                "用电",
                5.0f,
                calendar.getTimeInMillis()
        ));
        android.util.Log.d("模拟数据", "周五模拟数据：" + sdf.format(new Date(calendar.getTimeInMillis())));

        // 周日：出行-公交，数值10km
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        mockDataList.add(new CarbonFootprint(
                "出行",
                "公交",
                10.0f,
                calendar.getTimeInMillis()
        ));
        android.util.Log.d("模拟数据", "周日模拟数据：" + sdf.format(new Date(calendar.getTimeInMillis())));

        // 2. 异步插入模拟数据
        dbUtil.insertCarbonDataListAsync(mockDataList, new DBUtil.CarbonListInsertCallback() {
            @Override
            public void onInsertSuccess() {
                android.util.Log.d("模拟数据", "初始模拟数据插入成功");
                // 插入成功后重新加载数据
                loadChartDataFromDBAsync();
            }

            @Override
            public void onInsertFailed(Exception e) {
                android.util.Log.e("模拟数据", "初始模拟数据插入失败：" + e.getMessage());
                e.printStackTrace();
                // 插入失败仍显示空数据
                mainHandler.post(() -> {
                    updateLineChart(new ArrayList<>());
                    updatePieChart(new HashMap<>());
                    tvWeekTotalCarbon.setText("0.00");
                });
            }
        });
    }

    /**
     * 更新折线图
     */
    private void updateLineChart(List<CarbonFootprint> weekData) {
        List<Entry> lineEntries = new ArrayList<>();
        float[] weekCarbonValues = new float[7];
        for (int i = 0; i < 7; i++) {
            weekCarbonValues[i] = 0f;
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (CarbonFootprint data : weekData) {
            // 计算碳足迹数值
            double carbonValue = dbUtil.calculateCarbon(data.getType(), data.getSubType(), data.getValue());
            // 解析日期
            calendar.setTimeInMillis(data.getCreateTime());
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int index = dayOfWeek == 1 ? 6 : dayOfWeek - 2;

            // 打印转换过程
            android.util.Log.d("图表调试", "数据日期：" + sdf.format(new Date(data.getCreateTime())) +
                    " → 系统星期值=" + dayOfWeek +
                    " → 数组索引=" + index +
                    " → 对应星期=" + (index >=0 && index <7 ? weekDays[index] : "异常") +
                    " → 碳排放量=" + carbonValue);

            // 累加数值
            if (index >= 0 && index < 7) {
                weekCarbonValues[index] += (float) carbonValue;
            }
        }

        // 打印最终数值
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            sb.append(weekDays[i]).append("=").append(weekCarbonValues[i]).append("，");
        }
        android.util.Log.d("图表调试", "最终星期数值：" + sb.toString());

        // 填充Entry
        for (int i = 0; i < 7; i++) {
            lineEntries.add(new Entry(i, weekCarbonValues[i]));
        }

        // 折线图样式
        int primaryGreen = ContextCompat.getColor(mContext, R.color.primary_green);
        int primaryGreenLight = ContextCompat.getColor(mContext, R.color.primary_green_light);
        int grayDark = ContextCompat.getColor(mContext, R.color.gray_dark);

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "本周碳排放量（kgCO₂）");
        lineDataSet.setColor(primaryGreen);
        lineDataSet.setCircleColor(primaryGreen);
        lineDataSet.setCircleRadius(4f);
        lineDataSet.setCircleHoleRadius(2f);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillColor(primaryGreenLight);
        lineDataSet.setFillAlpha(80);

        LineData lineData = new LineData(lineDataSet);
        lineData.setDrawValues(true);
        lineData.setValueTextSize(10f);
        lineData.setValueTextColor(grayDark);

        // 设置数据并刷新
        lineChart.setData(lineData);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    /**
     * 更新饼图
     */
    private void updatePieChart(Map<String, Double> typeSumMap) {
        List<PieEntry> pieEntries = new ArrayList<>();

        // 填充统计数据
        for (Map.Entry<String, Double> entry : typeSumMap.entrySet()) {
            if (entry.getValue() > 0) {
                pieEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        }

        // 若无数据，显示默认值
        if (pieEntries.isEmpty()) {
            pieEntries.add(new PieEntry(100f, "暂无数据"));
        }

        // 饼图样式
        int[] colors = {
                ContextCompat.getColor(mContext, R.color.primary_green),
                ContextCompat.getColor(mContext, android.R.color.holo_orange_dark),
                ContextCompat.getColor(mContext, android.R.color.holo_blue_light),
                ContextCompat.getColor(mContext, R.color.gray_medium)
        };

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "碳排放品类占比");
        pieDataSet.setColors(colors);
        pieDataSet.setSliceSpace(2f);
        pieDataSet.setSelectionShift(5f);

        int white = ContextCompat.getColor(mContext, R.color.white);
        PieData pieData = new PieData(pieDataSet);
        pieData.setDrawValues(true);
        pieData.setValueTextSize(12f);
        pieData.setValueTextColor(white);

        // 设置数据并刷新
        pieChart.setData(pieData);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    /**
     * 初始化事件监听（异步提交数据）
     */
    private void initListener() {
        btnSubmit.setOnClickListener(v -> {
            // 收起软键盘
            hideSoftKeyboard(v);

            // 获取输入
            String type = etType.getText().toString().trim();
            String subType = etSubType.getText().toString().trim();
            String valueStr = etValue.getText().toString().trim();

            // 校验
            if (!validateInput(type, subType, valueStr)) {
                return;
            }

            float value = Float.parseFloat(valueStr);
            // 异步提交数据
            submitDataToDBAsync(type, subType, value);
        });
    }

    /**
     * 异步提交数据到数据库
     */
    private void submitDataToDBAsync(String type, String subType, float value) {
        dbUtil.insertCarbonDataAsync(type, subType, value, new DBUtil.CarbonInsertCallback() {
            @Override
            public void onInsertSuccess(long rowId) {
                // 1. 提示插入成功
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "数据提交成功！", Toast.LENGTH_SHORT).show();
                    clearInput();
                    setInputFocus();
                    loadChartDataFromDBAsync();
                });

                // 2. 自动完成"记录1次碳足迹"任务，增加积分
                dbUtil.completeCarbonTaskAfterInsertAsync(new DBUtil.TaskCompleteCallback() {
                    @Override
                    public void onCompleteSuccess(int newTotalPoint) {
                        mainHandler.post(() -> {
                            Toast.makeText(mContext, "完成任务！积分+10，当前总积分：" + newTotalPoint, Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onCompleteFailed(Exception e) {
                        e.printStackTrace();
                    }
                });
                // 检测「首次记录碳足迹」成就
                dbUtil.checkCarbonFirstAchievement(new DBUtil.AchievementUnlockCallback() {
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

                // 检测「积分小富翁」成就
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

            @Override
            public void onInsertFailed(Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "数据提交失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 输入校验
     */
    private boolean validateInput(String type, String subType, String valueStr) {
        if (type.isEmpty()) {
            Toast.makeText(mContext, "请输入碳排放类型（如：出行）", Toast.LENGTH_SHORT).show();
            etType.requestFocus();
            return false;
        }

        if (subType.isEmpty()) {
            Toast.makeText(mContext, "请输入子类型（如：自驾）", Toast.LENGTH_SHORT).show();
            etSubType.requestFocus();
            return false;
        }

        if (valueStr.isEmpty()) {
            Toast.makeText(mContext, "请输入数值（如：10）", Toast.LENGTH_SHORT).show();
            etValue.requestFocus();
            return false;
        }

        float value;
        try {
            value = Float.parseFloat(valueStr);
        } catch (NumberFormatException e) {
            Toast.makeText(mContext, "请输入有效数字（如：10、5.5）", Toast.LENGTH_SHORT).show();
            etValue.requestFocus();
            return false;
        }

        if (value <= 0) {
            Toast.makeText(mContext, "请输入正数数值", Toast.LENGTH_SHORT).show();
            etValue.requestFocus();
            return false;
        }

        return true;
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
     * 清空输入框
     */
    private void clearInput() {
        etType.setText("");
        etSubType.setText("");
        etValue.setText("");
    }

    /**
     * 设置输入框焦点
     */
    private void setInputFocus() {
        etType.requestFocus();
    }

    /**
     * 防止内存泄漏
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lineChart = null;
        pieChart = null;
        etType = null;
        etSubType = null;
        etValue = null;
        btnSubmit = null;
        tvWeekTotalCarbon = null;
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