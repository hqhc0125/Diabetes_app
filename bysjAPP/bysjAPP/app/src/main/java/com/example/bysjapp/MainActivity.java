package com.example.bysjapp;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // 用于保存所有Fragment的实例
    private Fragment educationFragment;
    private Fragment consultFragment;
    private Fragment detectionFragment;
    private Fragment communityFragment;
    private Fragment familyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 禁用图标 tint，确保 PNG 图片正常显示
        bottomNavigationView.setItemIconTintList(null);

        // 初始化所有Fragment
        educationFragment = new EducationFragment();
        consultFragment = new ConsultFragment();
        detectionFragment = new DetectionFragment();
        communityFragment = new CommunityFragment();
        familyFragment = new FamilyFragment();

        // 默认加载“科普”界面
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, educationFragment, "EducationFragment")
                    .commit();
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                // 隐藏所有Fragment
                if (educationFragment.isAdded()) transaction.hide(educationFragment);
                if (consultFragment.isAdded()) transaction.hide(consultFragment);
                if (detectionFragment.isAdded()) transaction.hide(detectionFragment);
                if (communityFragment.isAdded()) transaction.hide(communityFragment);
                if (familyFragment.isAdded()) transaction.hide(familyFragment);

                // 根据点击的导航项显示对应的Fragment
                if (id == R.id.nav_education) {
                    if (!educationFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, educationFragment, "EducationFragment");
                    }
                    transaction.show(educationFragment);
                } else if (id == R.id.nav_consult) {
                    if (!consultFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, consultFragment, "ConsultFragment");
                    }
                    transaction.show(consultFragment);
                } else if (id == R.id.nav_detection) {
                    if (!detectionFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, detectionFragment, "DetectionFragment");
                    }
                    transaction.show(detectionFragment);
                } else if (id == R.id.nav_community) {
                    if (!communityFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, communityFragment, "CommunityFragment");
                    }
                    transaction.show(communityFragment);
                } else if (id == R.id.nav_family) {
                    if (!familyFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, familyFragment, "FamilyFragment");
                    }
                    transaction.show(familyFragment);
                }

                // 提交事务
                transaction.commit();
                return true;
            }
        });
    }
}
