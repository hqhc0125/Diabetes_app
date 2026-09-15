package com.example.bysjapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BloodSugarDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BloodSugar.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "BloodSugarRecords";

    public BloodSugarDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp TEXT, " +
                "average_value REAL, " +
                "max_value REAL, " +
                "min_value REAL, " +
                "analysis_text TEXT" +
                ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN analysis_text TEXT");
        }
    }
}