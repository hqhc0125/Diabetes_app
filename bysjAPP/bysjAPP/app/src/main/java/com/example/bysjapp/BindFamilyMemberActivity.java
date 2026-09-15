package com.example.bysjapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BindFamilyMemberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_family_member);

        // 初始化控件
        EditText etFamilyMemberId = findViewById(R.id.et_family_member_id);
        Button btnBind = findViewById(R.id.btn_bind);

        // 点击绑定按钮
        btnBind.setOnClickListener(v -> {
            String familyMemberId = etFamilyMemberId.getText().toString();
            if (familyMemberId.isEmpty()) {
                Toast.makeText(this, "请输入家庭成员账号或ID", Toast.LENGTH_SHORT).show();
            } else {
                // 模拟绑定成功
                Toast.makeText(this, "绑定成功！", Toast.LENGTH_SHORT).show();
                finish(); // 返回到上一页面
            }
        });
    }
}
