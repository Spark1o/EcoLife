package com.example.eco.fragment.history;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eco.R;
import com.example.eco.adapter.CarbonHistoryAdapter;
import com.example.eco.db.DBUtil;
import com.example.eco.db.entity.CarbonFootprint;
import java.util.List;

public class CarbonHistoryFragment extends Fragment {

    private RecyclerView rvCarbonHistory;
    private TextView tvEmptyHistory;
    private Button btnClearAll;
    private ImageView ivBack;

    private DBUtil dbUtil;
    private Handler mainHandler;
    private CarbonHistoryAdapter historyAdapter;

    // 静态创建方法（供MainActivity调用）
    public static CarbonHistoryFragment newInstance() {
        return new CarbonHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carbon_history, container, false);

        // 初始化控件
        initViews(view);
        // 初始化数据工具和Handler
        initData();
        // 绑定点击事件
        setClickListeners();
        // 加载历史记录数据
        loadCarbonHistory();

        return view;
    }

    // 初始化控件
    private void initViews(View view) {
        rvCarbonHistory = view.findViewById(R.id.rv_carbon_history);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        btnClearAll = view.findViewById(R.id.btn_clear_all);
        ivBack = view.findViewById(R.id.iv_back);

        // 配置RecyclerView
        rvCarbonHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCarbonHistory.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        // ========== 修改适配器初始化 ==========
        historyAdapter = new CarbonHistoryAdapter(getContext(), null, new CarbonHistoryAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(CarbonFootprint item) {
                onItemClick(item, false);
            }

            @Override
            public void onDeleteClick(CarbonFootprint item) {
                onItemClick(item, true);
            }
        });
        rvCarbonHistory.setAdapter(historyAdapter);
    }

    // 初始化数据工具
    private void initData() {
        dbUtil = DBUtil.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // 绑定点击事件
    private void setClickListeners() {
        // 返回按钮：回到碳足迹主页面
        ivBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // 清空所有按钮
        btnClearAll.setOnClickListener(v -> {
            // 弹出清空确认对话框
            new AlertDialog.Builder(requireContext())
                    .setTitle("清空确认")
                    .setMessage("确定要清空所有历史记录吗？\n此操作不可恢复！")
                    .setPositiveButton("清空", (dialog, which) -> {
                        // 调用DBUtil清空记录
                        dbUtil.clearAllCarbonFootprintAsync(new DBUtil.CarbonClearCallback() {
                            @Override
                            public void onClearSuccess() {
                                Toast.makeText(requireContext(), "所有记录已清空", Toast.LENGTH_SHORT).show();
                                loadCarbonHistory(); // 重新加载数据
                            }

                            @Override
                            public void onClearFailed(Exception e) {
                                Toast.makeText(requireContext(), "清空失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // 加载历史记录数据（后续对接数据库）
// CarbonHistoryFragment.java 中修改 loadCarbonHistory 方法
    private void loadCarbonHistory() {
        // 加载中提示
        tvEmptyHistory.setText("加载中...");
        tvEmptyHistory.setVisibility(View.VISIBLE);
        rvCarbonHistory.setVisibility(View.GONE);

        // 异步查询数据库（改用匿名内部类，支持多个抽象方法）
        dbUtil.getAllCarbonFootprintAsync(new DBUtil.CarbonListCallback() {
            @Override
            public void onQuerySuccess(List<CarbonFootprint> data) {
                mainHandler.post(() -> {
                    if (data == null || data.isEmpty()) {
                        // 无数据：显示空提示
                        tvEmptyHistory.setText("暂无碳足迹记录\n快去添加吧～");
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                        rvCarbonHistory.setVisibility(View.GONE);
                    } else {
                        // 有数据：更新适配器并显示列表
                        historyAdapter.updateData(data);
                        tvEmptyHistory.setVisibility(View.GONE);
                        rvCarbonHistory.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onQueryFailed(Exception e) {
                mainHandler.post(() -> {
                    tvEmptyHistory.setText("查询失败：" + e.getMessage());
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    rvCarbonHistory.setVisibility(View.GONE);
                    // 提示用户
                    Toast.makeText(requireContext(), "查询历史记录失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onItemClick(CarbonFootprint item, boolean isDelete) {
        if (isDelete) {
            // 删除逻辑：弹出确认框
            new AlertDialog.Builder(requireContext())
                    .setTitle("删除确认")
                    .setMessage("确定要删除这条记录吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 调用DBUtil删除单条记录
                        dbUtil.deleteCarbonFootprintByIdAsync(item.getId(), new DBUtil.CarbonDeleteCallback() {
                            @Override
                            public void onDeleteSuccess() {
                                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                                loadCarbonHistory(); // 重新加载数据
                            }

                            @Override
                            public void onDeleteFailed(Exception e) {
                                Toast.makeText(requireContext(), "删除失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            showEditDialog(item);
        }
    }
    private void showEditDialog(CarbonFootprint editItem) {
        // 1. 创建弹窗
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("编辑碳足迹记录");
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_carbon, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 2. 获取弹窗控件
        Spinner spType = dialogView.findViewById(R.id.sp_edit_type);
        Spinner spSubtype = dialogView.findViewById(R.id.sp_edit_subtype);
        EditText etValue = dialogView.findViewById(R.id.et_edit_value);
        Button btnCancel = dialogView.findViewById(R.id.btn_edit_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_edit_save);

        // 3. 初始化Spinner数据（适配你的类型/子类型）
        // 类型列表
        String[] types = {"出行", "饮食", "消费"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(typeAdapter);

        // 子类型列表（根据初始类型加载）
        updateSubtypeSpinner(spType, spSubtype, editItem.getType());

        // 4. 回显数据到弹窗
        // 回显类型
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(editItem.getType())) {
                spType.setSelection(i);
                break;
            }
        }
        // 回显子类型（需提前准备子类型映射）
        String[] subTypes = getSubtypesByType(editItem.getType());
        for (int i = 0; i < subTypes.length; i++) {
            if (subTypes[i].equals(editItem.getSubType())) {
                spSubtype.setSelection(i);
                break;
            }
        }
        // 回显数值（保留1位小数，避免科学计数法）
        etValue.setText(String.format("%.1f", editItem.getValue()));

        // 5. 类型选择变化时，更新子类型列表
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = (String) parent.getItemAtPosition(position);
                updateSubtypeSpinner(spType, spSubtype, selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 6. 取消按钮
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 7. 保存按钮（核心：更新数据+计算碳排放量+局部刷新）
        btnSave.setOnClickListener(v -> {
            // 获取修改后的数据
            String newType = (String) spType.getSelectedItem();
            String newSubtype = (String) spSubtype.getSelectedItem();
            String valueStr = etValue.getText().toString().trim();

            // 校验：数值不能为空
            if (valueStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入数值", Toast.LENGTH_SHORT).show();
                return;
            }

            // 校验：数值格式（避免非数字输入）
            float newValue;
            try {
                newValue = Float.parseFloat(valueStr);
                // 校验：数值必须大于0
                if (newValue <= 0) {
                    Toast.makeText(requireContext(), "数值必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "请输入有效的数值", Toast.LENGTH_SHORT).show();
                return;
            }

            // ========== 核心修复1：计算碳排放量 ==========
            double newCarbonValue = dbUtil.calculateCarbon(newType, newSubtype, newValue);

            // 更新实体类数据（包含碳排放量）
            editItem.setType(newType);
            editItem.setSubType(newSubtype);
            editItem.setValue(newValue);
            editItem.setCarbonValue(newCarbonValue); // 关键：赋值碳排放量
            // 可选：更新修改时间（如果实体类有modifyTime字段）
            // editItem.setCreateTime(System.currentTimeMillis());

            // ========== 核心修复2：异步更新数据库 ==========
            dbUtil.updateCarbonFootprintAsync(editItem, new DBUtil.CarbonUpdateCallback() {
                @Override
                public void onUpdateSuccess() {
                    Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // 局部刷新列表（无需重新查询所有数据，性能更好）
                    int position = getPositionById(editItem.getId());
                    if (position != -1) {
                        historyAdapter.notifyItemChanged(position);
                    }
                    // 备选：如果局部刷新有问题，再用全局刷新
                    // loadCarbonHistory();
                }

                @Override
                public void onUpdateFailed(Exception e) {
                    Toast.makeText(requireContext(), "修改失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 显示弹窗
        dialog.show();
    }

    // 辅助方法：根据类型获取子类型列表
    private String[] getSubtypesByType(String type) {
        switch (type) {
            case "出行":
                return new String[]{"自驾", "公交", "地铁", "步行"};
            case "饮食":
                return new String[]{"外卖", "堂食", "自制"};
            case "消费":
                return new String[]{"购物", "缴费", "其他"};
            default:
                return new String[]{};
        }
    }

    // 辅助方法：更新子类型Spinner
    private void updateSubtypeSpinner(Spinner spType, Spinner spSubtype, String type) {
        String[] subtypes = getSubtypesByType(type);
        ArrayAdapter<String> subtypeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, subtypes);
        spSubtype.setAdapter(subtypeAdapter);
    }

    // 辅助方法：根据ID获取记录在列表中的位置（用于局部刷新）
    private int getPositionById(long id) {
        List<CarbonFootprint> dataList = historyAdapter.getDataList(); // 需给Adapter新增getDataList方法
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }
    // 适配器点击接口（兼容原有定义，可删除，因为Adapter已定义自己的接口）
    public interface OnItemClickListener {
        void onItemClick(CarbonFootprint item, boolean isDelete);
    }
}