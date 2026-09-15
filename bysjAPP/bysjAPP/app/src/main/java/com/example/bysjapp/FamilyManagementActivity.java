package com.example.bysjapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FamilyManagementActivity extends AppCompatActivity {

    private UserDatabaseHelper dbHelper;
    private EditText etFamilyAccount;
    private Button btnConnectFamily;
    private Button btnViewFamilyHistory;
    private String connectedAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_management);

        dbHelper = new UserDatabaseHelper(this);
        etFamilyAccount = findViewById(R.id.et_family_account);
        btnConnectFamily = findViewById(R.id.btn_connect_family);
        btnViewFamilyHistory = findViewById(R.id.btn_view_family_history);

        btnConnectFamily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = etFamilyAccount.getText().toString().trim();
                if (account.isEmpty()) {
                    Toast.makeText(FamilyManagementActivity.this, "请输入家人账号", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE account = ?", new String[]{account});
                if (cursor.moveToFirst()) {
                    Toast.makeText(FamilyManagementActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    connectedAccount = account;
                    btnViewFamilyHistory.setEnabled(true);
                    btnViewFamilyHistory.setBackgroundTintList(getColorStateList(R.color.blue));
                } else {
                    Toast.makeText(FamilyManagementActivity.this, "账号不存在，请重新输入", Toast.LENGTH_SHORT).show();
                    btnViewFamilyHistory.setEnabled(false);
                    btnViewFamilyHistory.setBackgroundTintList(getColorStateList(R.color.gray));
                }
                cursor.close();
                db.close();
            }
        });

        btnViewFamilyHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectedAccount != null) {
                    Intent intent = new Intent(FamilyManagementActivity.this, HistoryActivity.class);
                    intent.putExtra("bound_account", connectedAccount);
                    startActivity(intent);
                }
            }
        });
    }
}