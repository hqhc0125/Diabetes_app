package com.example.bysjapp;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class AccountSettingsActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private EditText etUsername, etAge, etHeight, etWeight, etOldPassword, etNewPassword, etConfirmPassword;
    private Spinner spGender;
    private Button btnSaveSettings;
    private UserDatabaseHelper dbHelper;
    private String selectedGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        dbHelper = new UserDatabaseHelper(this);

        ivProfilePicture = findViewById(R.id.iv_profile_picture_settings);
        etUsername = findViewById(R.id.et_username_settings);
        spGender = findViewById(R.id.sp_gender_settings);
        etAge = findViewById(R.id.et_age_settings);
        etHeight = findViewById(R.id.et_height_settings);
        etWeight = findViewById(R.id.et_weight_settings);
        etOldPassword = findViewById(R.id.et_old_password_settings);
        etNewPassword = findViewById(R.id.et_new_password_settings);
        etConfirmPassword = findViewById(R.id.et_confirm_password_settings);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        // 设置性别选择器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);
        spGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGender = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 保存设置按钮点击事件
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String username = etUsername.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(oldPassword) || !TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmPassword)) {
            if (TextUtils.isEmpty(oldPassword)) {
                Toast.makeText(this, "请输入原密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "请确认新密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "新密码和确认密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 更新数据库中的用户信息
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            // 更新用户名
            db.execSQL("UPDATE Users SET username = ? WHERE account = ?",
                    new Object[]{username, getIntent().getStringExtra("account")});

            // 更新性别、年龄、身高、体重等信息，可根据需求添加相应字段到数据库
            // db.execSQL("UPDATE Users SET gender = ?, age = ?, height = ?, weight = ? WHERE account = ?",
            //         new Object[]{selectedGender, age, height, weight, getIntent().getStringExtra("account")});

            // 更新密码
            if (!TextUtils.isEmpty(oldPassword) && !TextUtils.isEmpty(newPassword)) {
                // 验证原密码
                // 这里需要根据实际情况从数据库中获取原密码进行验证
                // 假设原密码验证通过
                db.execSQL("UPDATE Users SET password = ? WHERE account = ?",
                        new Object[]{newPassword, getIntent().getStringExtra("account")});
            }

            Toast.makeText(this, "设置保存成功", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存设置失败，请重试", Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }
}