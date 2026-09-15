package com.example.bysjapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ConsultDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consult_detail);

        TextView tvContent = findViewById(R.id.tv_content);
        String content = getIntent().getStringExtra("content");
        tvContent.setText(content);
    }
}
