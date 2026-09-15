package com.example.bysjapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private UserDatabaseHelper dbHelper;
    private EditText etUsername, etAccount, etPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        dbHelper = new UserDatabaseHelper(this);

        etUsername = findViewById(R.id.et_username);
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private boolean isAccountExists(String account) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE account = ?", new String[]{account});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAccountExists(account)) {
            Toast.makeText(this, "注册失败，账号已存在！", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("account", account);
        values.put("password", password);

        long result = db.insert("Users", null, values);
        db.close();

        if (result != -1) {
            Toast.makeText(this, "恭喜您！注册成功！", Toast.LENGTH_SHORT).show();
            finish(); // 注册成功后返回登录界面
        } else {
            Toast.makeText(this, "注册失败，请重试！", Toast.LENGTH_SHORT).show();
        }
    }
}
