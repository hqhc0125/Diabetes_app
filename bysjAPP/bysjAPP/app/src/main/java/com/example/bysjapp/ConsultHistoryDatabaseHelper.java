package com.example.bysjapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ConsultHistoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ConsultHistory.db"; // 数据库名称
    private static final int DATABASE_VERSION = 1; // 数据库版本号

    public ConsultHistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE ConsultHistory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +        // 简短描述或标题
                "timestamp TEXT, " +    // 创建时间
                "content TEXT" +        // 完整对话内容
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS ConsultHistory");
        onCreate(db);
    }
}
