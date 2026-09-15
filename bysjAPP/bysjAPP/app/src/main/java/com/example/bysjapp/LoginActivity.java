package com.example.bysjapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private UserDatabaseHelper dbHelper;
    private EditText etAccount, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ImageView ivPasswordToggle;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        dbHelper = new UserDatabaseHelper(this);

        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);

        // 登录按钮监听
        btnLogin.setOnClickListener(v -> login());

        // 注册按钮监听
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 密码可见切换功能
        ivPasswordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                ivPasswordToggle.setImageResource(R.drawable.ic_hide_password);
                isPasswordVisible = false;
            } else {
                etPassword.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                ivPasswordToggle.setImageResource(R.drawable.ic_show_password);
                isPasswordVisible = true;
            }
        });
    }

    private void login() {
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 检查是否输入为空
        if (account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE account = ?", new String[]{account});

            if (cursor.moveToFirst()) {
                @SuppressLint("Range") String storedPassword = cursor.getString(cursor.getColumnIndex("password"));
                if (storedPassword.equals(password)) {
                    Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // 关闭登录页面
                } else {
                    Toast.makeText(this, "密码错误，请重新输入", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "账号不存在，请重新输入", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常堆栈信息
            Toast.makeText(this, "发生异常：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
