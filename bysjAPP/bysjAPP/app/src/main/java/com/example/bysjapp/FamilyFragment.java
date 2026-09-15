package com.example.bysjapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FamilyFragment extends Fragment {

    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private Button btnFavorites, btnSettings, btnFeedback, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family, container, false);

        // 初始化控件
        ivProfilePicture = view.findViewById(R.id.iv_profile_picture);
        tvUsername = view.findViewById(R.id.tv_username);
        btnFavorites = view.findViewById(R.id.btn_favorites);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnFeedback = view.findViewById(R.id.btn_feedback);
        Button btnFamily = view.findViewById(R.id.btn_family); // 添加家人管理按钮绑定
        btnLogout = view.findViewById(R.id.btn_logout);

        // 设置初始值
        tvUsername.setText("欢迎，用户123"); // 可替换为实际用户名

        // 点击事件处理
        btnFavorites.setOnClickListener(v -> {
            // 跳转到收藏夹页面
            Intent intent = new Intent(getActivity(), FavoritesActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            // 跳转到设置页面
            Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
            // 传递账号信息到设置界面，可根据实际情况修改
            intent.putExtra("account", "用户账号");
            startActivity(intent);
        });

        // 合并“切换账号”和“退出登录”逻辑
        View.OnClickListener logoutClickListener = v -> {
            String message = v.getId() == R.id.btn_logout ? "确定要退出账号吗" : "确定要切换账号吗";
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("提示")
                    .setMessage(message)
                    .setPositiveButton("确认", (dialog, which) -> {
                        // 处理退出或切换逻辑
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        requireActivity().finishAffinity(); // 关闭所有其他界面
                    })
                    .setNegativeButton("取消", null)
                    .show();
        };

        btnFeedback.setOnClickListener(logoutClickListener);
        btnLogout.setOnClickListener(logoutClickListener);

        btnFamily.setOnClickListener(v -> {
            // 跳转到家人管理界面
            Intent intent = new Intent(getActivity(), FamilyManagementActivity.class);
            startActivity(intent);
        });

        return view;
    }
}