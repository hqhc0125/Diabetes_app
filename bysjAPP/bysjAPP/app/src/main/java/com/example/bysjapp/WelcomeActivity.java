package com.example.bysjapp;  // 请替换为你的包名

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 延时2秒后跳转到 MainActivity（默认显示“检测”界面）
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 启动主界面
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                // 结束当前欢迎界面
                finish();
            }
        }, 1000);  // 延时2000毫秒，即2秒
    }
}
