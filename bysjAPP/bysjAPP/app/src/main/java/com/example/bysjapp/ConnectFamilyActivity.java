package com.example.bysjapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ConnectFamilyActivity extends AppCompatActivity {

    private UserDatabaseHelper dbHelper;
    private String role;  // "家人" or "患者"
    private boolean boundAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_family);

        dbHelper = new UserDatabaseHelper(this);
        role = getIntent().getStringExtra("role");

        EditText etAccount = findViewById(R.id.et_account);
        Button btnConnect = findViewById(R.id.btn_connect);

        btnConnect.setOnClickListener(v -> {
            String inputAccount = etAccount.getText().toString().trim();
            if (inputAccount.isEmpty()) {
                Toast.makeText(this, "请输入账号", Toast.LENGTH_SHORT).show();
                return;
            }

            // 查询账号是否存在
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE username = ?", new String[]{inputAccount});
            if (cursor.moveToFirst()) {
                if (role.equals("家人")) {
                    // 家人角色：提示“等待对方同意”
                    Toast.makeText(this, "账号连接中，等待对方同意", Toast.LENGTH_SHORT).show();
                } else {
                    // 患者角色：直接绑定
                    ContentValues values = new ContentValues();
                    values.put("bound_account", inputAccount);
                    db.update("Users", values, "username = ?", new String[]{inputAccount});
                    Toast.makeText(this, "账号绑定成功", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 账号不存在
                Toast.makeText(this, "账号不存在，请重新输入", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
            db.close();
        });

        Button btnViewHistory = findViewById(R.id.btn_view_history);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.putExtra("bound_account", boundAccount);  // 传递绑定的患者账号
            startActivity(intent);
        });

    }
}
