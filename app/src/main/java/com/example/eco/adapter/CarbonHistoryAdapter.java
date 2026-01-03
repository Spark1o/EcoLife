package com.example.eco.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eco.R;
import com.example.eco.db.entity.CarbonFootprint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 碳足迹历史记录适配器
 * 适配 item_carbon_history.xml 布局
 */
public class CarbonHistoryAdapter extends RecyclerView.Adapter<CarbonHistoryAdapter.ViewHolder> {

    private Context mContext;
    private List<CarbonFootprint> mDataList;
    private OnItemClickListener mListener;

    // 点击事件接口（编辑/删除）
    public interface OnItemClickListener {
        void onEditClick(CarbonFootprint item); // 编辑点击
        void onDeleteClick(CarbonFootprint item); // 删除点击
    }

    // 构造方法
    public CarbonHistoryAdapter(Context context, List<CarbonFootprint> dataList, OnItemClickListener listener) {
        this.mContext = context;
        this.mDataList = dataList;
        this.mListener = listener;
    }

    // 更新列表数据
    public void updateData(List<CarbonFootprint> newData) {
        this.mDataList = newData;
        notifyDataSetChanged(); // 刷新列表
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载自定义列表项布局
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_carbon_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarbonFootprint item = mDataList.get(position);
        if (item == null) return;

        // 1. 绑定类型（类型-子类型）
        holder.tvType.setText(item.getType() + "-" + item.getSubType());

        // 2. 绑定时间（格式化时间戳）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(item.getCreateTime())));

        // 3. 绑定数值（根据类型补充单位）
        String valueUnit = getValueUnit(item.getType());
        holder.tvValue.setText(String.format(Locale.getDefault(),
                "数值：%.2f %s", item.getValue(), valueUnit));

        // 4. 绑定碳排放量
        holder.tvCarbon.setText(String.format(Locale.getDefault(),
                "碳排放：%.2f kgCO₂", item.getCarbonValue()));

        // 5. 绑定编辑/删除点击事件
        holder.tvEdit.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onEditClick(item);
            }
        });

        holder.tvDelete.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onDeleteClick(item);
            }
        });
    }

    // 根据类型获取数值单位（优化显示）
// 根据类型获取数值单位（优化显示）- 适配Java 8
    private String getValueUnit(String type) {
        // 替换Java 14的switch表达式为Java 8兼容的switch语句
        if (type == null) {
            return "单位";
        }
        switch (type) {
            case "出行":
                return "公里";
            case "饮食":
                return "餐";
            case "消费":
                return "元";
            default:
                return "单位";
        }
    }

    @Override
    public int getItemCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    // ViewHolder：绑定列表项所有控件
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTime, tvValue, tvCarbon, tvEdit, tvDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定item_carbon_history.xml的所有控件ID
            tvType = itemView.findViewById(R.id.tv_item_type);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvValue = itemView.findViewById(R.id.tv_item_value);
            tvCarbon = itemView.findViewById(R.id.tv_item_carbon);
            tvEdit = itemView.findViewById(R.id.tv_item_edit);
            tvDelete = itemView.findViewById(R.id.tv_item_delete);
        }
    }
    public List<CarbonFootprint> getDataList() {
        return mDataList;
    }
}