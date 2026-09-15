package com.example.bysjapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FamilyMemberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_member);

        // 初始化控件
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvDescription = findViewById(R.id.tv_description);
        Button btnConnectAccount = findViewById(R.id.btn_connect_account);

        // 点击连接账号按钮
        btnConnectAccount.setOnClickListener(v -> {
            // 跳转到绑定账号页面
            Intent intent = new Intent(FamilyMemberActivity.this, BindFamilyMemberActivity.class);
            startActivity(intent);
        });

    }
}
