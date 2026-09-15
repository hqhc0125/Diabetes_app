package com.example.bysjapp;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private BloodSugarDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new BloodSugarDatabaseHelper(this);
        ListView listView = findViewById(R.id.lv_history);

        // 查询数据库
        ArrayList<HashMap<String, String>> data = getHistoryData();

        // 使用SimpleAdapter填充ListView
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "subtitle"},
                new int[]{android.R.id.text1, android.R.id.text2});

        listView.setAdapter(adapter);

        // 设置列表项点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> item = data.get(position);
                String analysisText = item.get("analysis_text");
                if (analysisText != null && !analysisText.isEmpty()) {
                    showAnalysisDialog(analysisText);
                } else {
                    Toast.makeText(HistoryActivity.this, "暂无分析内容", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private ArrayList<HashMap<String, String>> getHistoryData() {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 查询数据
        Cursor cursor = db.rawQuery("SELECT * FROM BloodSugarRecords ORDER BY timestamp DESC", null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        while (cursor.moveToNext()) {
            @SuppressLint("Range") long timestampMillis = Long.parseLong(cursor.getString(cursor.getColumnIndex("timestamp")));
            String formattedTime = sdf.format(new java.util.Date(timestampMillis));

            @SuppressLint("Range") float avg = cursor.getFloat(cursor.getColumnIndex("average_value"));
            @SuppressLint("Range") float max = cursor.getFloat(cursor.getColumnIndex("max_value"));
            @SuppressLint("Range") float min = cursor.getFloat(cursor.getColumnIndex("min_value"));
            @SuppressLint("Range") String analysisText = cursor.getString(cursor.getColumnIndex("analysis_text"));

            // 显示的内容，保留一位小数
            HashMap<String, String> map = new HashMap<>();
            map.put("title", "检测时间: " + formattedTime);
            map.put("subtitle", String.format(Locale.getDefault(), "平均值: %.1f 最大值: %.1f 最小值: %.1f", avg, max, min));
            map.put("analysis_text", analysisText);
            data.add(map);
        }
        cursor.close();
        db.close();
        return data;
    }

    private void showAnalysisDialog(String analysisText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("检测分析内容")
                .setMessage(analysisText)
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .show();
    }
}