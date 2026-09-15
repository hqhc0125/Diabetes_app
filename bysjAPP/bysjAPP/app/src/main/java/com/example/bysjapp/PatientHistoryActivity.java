package com.example.bysjapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class PatientHistoryActivity extends AppCompatActivity {

    private BloodSugarDatabaseHelper bloodSugarDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_history);

        bloodSugarDbHelper = new BloodSugarDatabaseHelper(this);
        String boundAccount = getIntent().getStringExtra("bound_account");

        ListView listView = findViewById(R.id.lv_patient_history);
        ArrayList<HashMap<String, String>> data = getPatientHistory(boundAccount);

        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "subtitle"},
                new int[]{android.R.id.text1, android.R.id.text2});
        listView.setAdapter(adapter);
    }

    private ArrayList<HashMap<String, String>> getPatientHistory(String username) {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        SQLiteDatabase db = bloodSugarDbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM BloodSugarRecords WHERE UserID = ?", new String[]{username});
        while (cursor.moveToNext()) {
            String timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
            String averageValue = cursor.getString(cursor.getColumnIndex("average_value"));

            HashMap<String, String> map = new HashMap<>();
            map.put("title", "检测时间: " + timestamp);
            map.put("subtitle", "平均血糖值: " + averageValue);
            data.add(map);
        }
        cursor.close();
        db.close();
        return data;
    }
}
