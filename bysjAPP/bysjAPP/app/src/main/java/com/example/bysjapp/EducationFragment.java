package com.example.bysjapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EducationFragment extends Fragment {

    private RecyclerView knowledgeRecyclerView, guideRecyclerView, newsRecyclerView;
    private EducationAdapter knowledgeAdapter, guideAdapter, newsAdapter;
    private SearchView searchView;
    private FavoriteManager favoriteManager;

    private List<EducationItem> knowledgeList = new ArrayList<>();
    private List<EducationItem> guideList = new ArrayList<>();
    private List<EducationItem> newsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_education, container, false);
        favoriteManager = new FavoriteManager(requireContext());

        // 搜索栏
        searchView = view.findViewById(R.id.search_view);

        // 设置“控糖知识库”分区的更多按钮逻辑
        TextView knowledgeMoreButton = view.findViewById(R.id.knowledge_more_button);
        knowledgeMoreButton.setOnClickListener(v -> {
            if (knowledgeAdapter != null) {
                if (knowledgeAdapter.getItemCount() == 3) {
                    knowledgeAdapter.setExpanded(true);
                    knowledgeMoreButton.setText("收起");
                } else {
                    knowledgeAdapter.setExpanded(false);
                    knowledgeMoreButton.setText("更多");
                }
            }
        });

        // 设置“健康指南文章”分区的更多按钮逻辑
        TextView guideMoreButton = view.findViewById(R.id.guide_more_button);
        guideMoreButton.setOnClickListener(v -> {
            if (guideAdapter != null) {
                if (guideAdapter.getItemCount() == 3) {
                    guideAdapter.setExpanded(true);
                    guideMoreButton.setText("收起");
                } else {
                    guideAdapter.setExpanded(false);
                    guideMoreButton.setText("更多");
                }
            }
        });

        // 设置“最新控糖动态”分区的更多按钮逻辑
        TextView newsMoreButton = view.findViewById(R.id.news_more_button);
        newsMoreButton.setOnClickListener(v -> {
            if (newsAdapter != null) {
                if (newsAdapter.getItemCount() == 3) {
                    newsAdapter.setExpanded(true);
                    newsMoreButton.setText("收起");
                } else {
                    newsAdapter.setExpanded(false);
                    newsMoreButton.setText("更多");
                }
            }
        });

        // 初始化 RecyclerView
        knowledgeRecyclerView = view.findViewById(R.id.knowledge_recycler_view);
        guideRecyclerView = view.findViewById(R.id.guide_recycler_view);
        newsRecyclerView = view.findViewById(R.id.news_recycler_view);

        knowledgeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        guideRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        populateData();

        knowledgeAdapter = new EducationAdapter(knowledgeList, getContext(), favoriteManager);
        guideAdapter = new EducationAdapter(guideList, getContext(), favoriteManager);
        newsAdapter = new EducationAdapter(newsList, getContext(), favoriteManager);

        knowledgeRecyclerView.setAdapter(knowledgeAdapter);
        guideRecyclerView.setAdapter(guideAdapter);
        newsRecyclerView.setAdapter(newsAdapter);

        // 搜索功能监听
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterContent(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterContent(newText);
                return true;
            }
        });

        return view;
    }

    private void populateData() {
        // 增加更多数据以测试显示效果
        knowledgeList.add(new EducationItem("什么是食物升糖指数（GI）和升糖负荷（GL）", "血糖生成指数...", R.drawable.image1, "https://www.gluok.com/article/202304/04115105.html"));
        knowledgeList.add(new EducationItem("研究：调整吃饭顺序可帮助预防糖尿病并控制血糖水平", "根据2023年10月20日发表...", R.drawable.image2, "https://www.gluok.com/article/202310/27223601.html"));
        knowledgeList.add(new EducationItem("糖尿病人吃面食注意这3点, 餐后血糖更平稳", "患有糖尿病的人...", R.drawable.image3, "https://www.gluok.com/article/202309/15164316.html"));
        knowledgeList.add(new EducationItem("糖友饮食指导：全球各糖尿病协会对饮食中主要营养素摄入量的建议", "糖尿病营养治疗是...", R.drawable.image4, "https://www.gluok.com/article/202311/11104835.html"));

        guideList.add(new EducationItem("最新发布2型糖尿病及前期患者心血管疾病预防与管理专家共识", "中国成人2型糖尿病及糖尿病前期患者...", R.drawable.image5, "http://www.cds.org.cn/znygs/4413.html"));
        guideList.add(new EducationItem("老年人多重用药安全管理专家共识", "我国是世界上老年人口最多的...", R.drawable.image6, "http://www.cds.org.cn/znygs/4353.html"));
        guideList.add(new EducationItem("关于2型糖尿病合并慢性肾脏病患者应用胰岛素治疗的专家指导建议", "2015年美国肾脏数据系统显示...", R.drawable.image7, "http://www.cds.org.cn/znygs/4120.html"));
        guideList.add(new EducationItem("中国２型糖尿病患者餐后高血糖管理专家共识", "中国T2DM伴餐后血糖(PPG)升高的患者比例高...", R.drawable.image8, "http://www.cds.org.cn/znygs/3734.html"));

        newsList.add(new EducationItem("糖尿病患者骨折难愈？新技术助力快速恢复！", "糖尿病患者骨折后为何总是难以愈合...", R.drawable.image9, "https://www.sohu.com/a/898446188_121956422"));
        newsList.add(new EducationItem("新技术iCGM助力糖尿病精细化管理", "我国糖尿病患者已超1.4亿，居世界第一...", R.drawable.image10, "https://news.qq.com/rain/a/20250521A06WRY00"));
        newsList.add(new EducationItem("新发1型糖尿病六大治疗误区与循证应对策略", "1型糖尿病（T1DM）是由胰岛β细胞自身免疫性破坏导致的...", R.drawable.image11, "https://news.qq.com/rain/a/20250523A05FXW00"));
        newsList.add(new EducationItem("糖尿病防治呼唤新模式，全院血糖管理获好评！", "近年来,医院糖尿病住院患者呈逐年增加趋势...", R.drawable.image12, "https://www.163.com/news/article/JVHBOF0B00019UD6.html"));
    }

    private void filterContent(String query) {
        // 调用各个分区的过滤方法
        knowledgeAdapter.filter(query);
        guideAdapter.filter(query);
        newsAdapter.filter(query);
    }
}