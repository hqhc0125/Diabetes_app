package com.example.bysjapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CommentDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "comment.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "comments";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_CONTENT = "content";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USERNAME + " TEXT, " +
            COLUMN_TIME + " TEXT, " +
            COLUMN_CONTENT + " TEXT);";

    public CommentDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 添加公共的 getter 方法
    public static String getTableName() {
        return TABLE_NAME;
    }

    public static String getColumnUsername() {
        return COLUMN_USERNAME;
    }

    public static String getColumnTime() {
        return COLUMN_TIME;
    }

    public static String getColumnContent() {
        return COLUMN_CONTENT;
    }
}