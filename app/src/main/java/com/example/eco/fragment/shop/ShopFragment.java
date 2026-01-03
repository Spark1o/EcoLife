package com.example.eco.fragment.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco.R;
import com.example.eco.db.manager.AppDatabase;
import com.example.eco.db.dao.PointDao;
import com.example.eco.db.entity.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * 环保好物兑换Fragment（对接官方AppDatabase）
 */
public class ShopFragment extends Fragment {

    private RecyclerView rvGoods;
    private GoodsAdapter adapter;
    private List<Goods> goodsList;
    private PointDao pointDao; // 从AppDatabase获取PointDao
    private TextView tvUserScore; // 展示当前积分

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        // 1. 初始化数据库：使用你已有的AppDatabase单例
        AppDatabase db = AppDatabase.getInstance(requireContext());
        pointDao = db.pointDao(); // 获取PointDao

        // 2. 初始化积分展示控件
        tvUserScore = view.findViewById(R.id.tv_user_score);
        updateUserScoreDisplay(); // 实时更新积分显示

        // 3. 初始化商品数据
        initGoodsData();

        // 4. 初始化RecyclerView
        rvGoods = view.findViewById(R.id.rv_goods);
        rvGoods.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GoodsAdapter(goodsList);
        rvGoods.setAdapter(adapter);

        return view;
    }

    /**
     * 初始化商品数据（保留你原有丰富的商品列表）
     */
    private void initGoodsData() {
        goodsList = new ArrayList<>();
        // 日常环保用品
        goodsList.add(new Goods("可降解餐具套装", 50, "玉米淀粉基环保材料制作，耐高温可自然降解，替代一次性塑料餐具"));
        goodsList.add(new Goods("环保帆布袋", 30, "纯棉加厚材质，可重复使用500+次，减少塑料袋使用，印有低碳标语"));
        goodsList.add(new Goods("绿植种子包", 20, "包含薄荷、绿萝、多肉等易活绿植种子，搭配有机营养土，净化室内空气"));
        goodsList.add(new Goods("低碳水杯", 80, "304食品级不锈钢，长效保温保冷，无塑封设计，减少一次性水杯使用"));
        // 清洁类环保品
        goodsList.add(new Goods("天然竹纤维抹布", 25, "无荧光剂无添加，吸水吸油易清洗，可降解不污染环境，10片装"));
        goodsList.add(new Goods("植物酵素清洁剂", 60, "提取天然橙皮、椰子油酵素，无化学残留，可降解，适用于餐具/家居清洁"));
        // 节能类产品
        goodsList.add(new Goods("太阳能便携充电宝", 150, "5000mAh容量，太阳能充电，户外应急使用，减少电网用电消耗"));
        goodsList.add(new Goods("LED节能灯泡", 40, "低功耗高亮度，使用寿命8000小时，比传统灯泡省电80%，E27通用接口"));
        // 户外环保用品
        goodsList.add(new Goods("不锈钢便携餐具", 75, "筷子+勺子+叉子三件套，折叠设计，食品级材质，外出就餐替代一次性餐具"));
        goodsList.add(new Goods("环保分类垃圾袋", 35, "可降解材质，加厚防漏，分厨余/可回收/其他三类，50只装"));
        // 文创类环保品
        goodsList.add(new Goods("再生纸笔记本", 25, "100%再生纸制作，无漂白剂，封面采用环保牛皮纸，内页80张"));
        goodsList.add(new Goods("低碳主题帆布包", 45, "环保印染工艺，印有「低碳出行 绿色生活」标语，大容量可装10kg物品"));
        // 厨房环保用品
        goodsList.add(new Goods("硅胶保鲜盖", 55, "食品级硅胶材质，替代保鲜膜，可重复使用，适配不同尺寸碗盘，6件套"));
        goodsList.add(new Goods("秸秆环保餐盒", 65, "小麦秸秆材质，可微波炉加热，密封防漏，上班族带饭专用，可降解"));
    }

    /**
     * 更新当前积分显示（从Point表获取最新总积分）
     */
    private void updateUserScoreDisplay() {
        // 建议放到子线程执行（避免主线程阻塞，适配你的AppDatabase）
        new Thread(() -> {
            int currentTotal = pointDao.getCurrentTotalPoint();
            // 切回主线程更新UI
            requireActivity().runOnUiThread(() -> {
                tvUserScore.setText("当前积分：" + currentTotal);
            });
        }).start();
    }

    /**
     * 核心：兑换逻辑（基于你的Point表+AppDatabase）
     */
    private void exchangeGoods(Goods goods) {
        // 数据库操作放到子线程（适配你的AppDatabase线程安全要求）
        new Thread(() -> {
            // 1. 获取当前总积分
            int currentTotal = pointDao.getCurrentTotalPoint();
            int needScore = goods.getScore();

            // 2. 校验积分是否足够
            if (currentTotal >= needScore) {
                // 3. 计算扣减后的总积分
                int newTotal = currentTotal - needScore;

                // 4. 插入积分变动记录（负数=扣减，reason记录兑换商品）
                Point pointRecord = new Point(
                        -needScore,          // 变动值：扣减为负
                        newTotal,            // 变动后的总积分
                        "兑换商品：" + goods.getName() // 变动原因
                );
                pointDao.insertPoint(pointRecord); // 插入到数据库

                // 5. 主线程反馈成功+更新UI
                requireActivity().runOnUiThread(() -> {
                    updateUserScoreDisplay();
                    Toast.makeText(getContext(),
                            "兑换成功！「" + goods.getName() + "」已扣减" + needScore + "积分，剩余：" + newTotal,
                            Toast.LENGTH_SHORT).show();
                });
            } else {
                // 积分不足，主线程提示
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                            "积分不足！兑换「" + goods.getName() + "」需要" + needScore + "积分，当前仅有" + currentTotal,
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 商品实体类
     */
    public static class Goods {
        private String name;
        private int score;
        private String desc;

        public Goods(String name, int score, String desc) {
            this.name = name;
            this.score = score;
            this.desc = desc;
        }

        public String getName() { return name; }
        public int getScore() { return score; }
        public String getDesc() { return desc; }
    }

    /**
     * 商品适配器
     */
    private class GoodsAdapter extends RecyclerView.Adapter<GoodsAdapter.GoodsViewHolder> {
        private List<Goods> list;

        public GoodsAdapter(List<Goods> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public GoodsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goods, parent, false);
            return new GoodsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GoodsViewHolder holder, int position) {
            Goods goods = list.get(position);
            holder.tvName.setText(goods.getName());
            holder.tvScore.setText(goods.getScore() + "积分");
            holder.tvDesc.setText(goods.getDesc());

            // 兑换按钮点击事件
            holder.btnExchange.setOnClickListener(v -> {
                exchangeGoods(goods); // 调用核心兑换逻辑
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class GoodsViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvScore, tvDesc;
            Button btnExchange;

            public GoodsViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_goods_name);
                tvScore = itemView.findViewById(R.id.tv_goods_score);
                tvDesc = itemView.findViewById(R.id.tv_goods_desc);
                btnExchange = itemView.findViewById(R.id.btn_exchange);
            }
        }
    }

    /**
     * 页面销毁时清空引用，避免内存泄漏
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        goodsList = null;
        pointDao = null;
    }
}