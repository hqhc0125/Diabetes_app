package com.example.bysjapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserManagement.db"; // 数据库名称
    private static final int DATABASE_VERSION = 2; // 数据库版本

    // 创建用户表的 SQL 语句
    private static final String CREATE_TABLE_USERS = "CREATE TABLE IF NOT EXISTS Users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT NOT NULL, " +
            "account TEXT UNIQUE NOT NULL, " +
            "password TEXT NOT NULL)";

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // 创建用户表
            db.execSQL(CREATE_TABLE_USERS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Users"); // 删除旧表
        onCreate(db); // 重新创建表
    }


    // 插入新用户数据方法
    public boolean insertUser(String username, String account, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("INSERT INTO Users (username, account, password) VALUES (?, ?, ?)",
                    new Object[]{username, account, password});
            return true; // 插入成功
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 插入失败
        } finally {
            db.close();
        }
    }

    // 查询用户数据方法
    public Cursor getUserByAccount(String account) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Users WHERE account = ?", new String[]{account});
    }

    // 清空用户表数据（可用于开发测试）
    public void clearTableData() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM Users");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    // 验证表列结构（调试用）
    @SuppressLint("Range")
    public void printTableColumns() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("PRAGMA table_info(Users);", null);
        while (cursor.moveToNext()) {
            // 打印列名
            System.out.println("Column: " + cursor.getString(cursor.getColumnIndex("name")));
        }
        cursor.close();
        db.close();
    }
}
