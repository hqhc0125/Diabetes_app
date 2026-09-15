package com.example.bysjapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ConsultHistoryActivity extends AppCompatActivity {

    private ConsultHistoryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consult_history);

        dbHelper = new ConsultHistoryDatabaseHelper(this);
        ListView listView = findViewById(R.id.lv_consult_history);

        // 查询数据库
        ArrayList<HashMap<String, String>> data = getHistoryData();

        // 使用SimpleAdapter填充ListView
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "timestamp"},
                new int[]{android.R.id.text1, android.R.id.text2});

        listView.setAdapter(adapter);

        // 设置列表项点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> item = data.get(position);
                String content = item.get("content");
                // 跳转到详情页显示完整对话内容
                Intent intent = new Intent(ConsultHistoryActivity.this, ConsultDetailActivity.class);
                intent.putExtra("content", content);
                startActivity(intent);
            }
        });
    }

    private ArrayList<HashMap<String, String>> getHistoryData() {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 查询数据
        Cursor cursor = db.rawQuery("SELECT * FROM ConsultHistory ORDER BY timestamp DESC", null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        while (cursor.moveToNext()) {
            @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("title"));
            @SuppressLint("Range") String timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
            @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex("content"));

            // 显示的内容
            HashMap<String, String> map = new HashMap<>();
            map.put("title", title);
            map.put("timestamp", timestamp);
            map.put("content", content);
            data.add(map);
        }
        cursor.close();
        db.close();
        return data;
    }
}