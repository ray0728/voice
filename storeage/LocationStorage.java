package com.ray.lab.voice.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.SparseArray;

import com.ray.lab.voice.util.LocationData;


public class LocationStorage {
    private static final String TAG = "LocationStorage";
    private long mStart = 0;
    private long mEnd = 0;
    private DBHelper mHelper;
    private static LocationStorage instance;

    public LocationStorage(Context context, String dbname) {
        mHelper = new DBHelper(context, dbname);
    }


    public void record(String time, int type, double lat, double lon) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("time", time);
        values.put("type", type);
        values.put("lat", lat);
        values.put("lon", lon);
        long ret = db.insert(DBHelper.TABLE_LOCATION, null, values);
        if (mStart == 0) {
            mStart = ret;
        } else {
            mEnd = ret;
        }
        db.close();
    }

    public void stop() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("startid", mStart);
        values.put("endid", mEnd);
        db.insert(DBHelper.TABLE_RECORD, null, values);
        mStart = 0;
        mEnd = 0;
        db.close();
    }

    public SparseArray<LocationData> getRecord(String condition) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        String selectQuery = "SELECT " + DBHelper.TABLE_RECORD + ".id, startid, endid, " + DBHelper.TABLE_LOCATION
                + ".time AS time FROM " + DBHelper.TABLE_RECORD + " INNER JOIN " + DBHelper.TABLE_LOCATION
                + " ON " + DBHelper.TABLE_LOCATION + ".id = " + DBHelper.TABLE_RECORD + ".startid";
        if (condition != null) {
            selectQuery += " AND " + DBHelper.TABLE_LOCATION + ".time LIKE '" + condition + "%'";
        }
        SparseArray<LocationData> records = new SparseArray<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        LocationData data = null;
        if (cursor.moveToFirst()) {
            do {
                data = new LocationData();
                data.setStartid(cursor.getLong(cursor.getColumnIndex("startid")));
                data.setEndid(cursor.getLong(cursor.getColumnIndex("endid")));
                data.setTime(cursor.getString(cursor.getColumnIndex("time")));
                records.append(cursor.getInt(0), data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    public SparseArray<LocationData> getLocationRecord(long start, long end) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + DBHelper.TABLE_LOCATION + " WHERE id BETWEEN " + start + " AND " + end;
        SparseArray<LocationData> records = new SparseArray<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        LocationData data = null;
        if (cursor.moveToFirst()) {
            do {
                data = new LocationData();
                data.setTime(cursor.getString(cursor.getColumnIndex("time")));
                data.setType(cursor.getInt(cursor.getColumnIndex("type")));
                data.setLat(cursor.getDouble(cursor.getColumnIndex("lat")));
                data.setLon(cursor.getDouble(cursor.getColumnIndex("lon")));
                records.append(cursor.getInt(0), data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }


    private class DBHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        public static final String TABLE_LOCATION = "location";
        public static final String TABLE_RECORD = "record";
        private final String[] CREATE_SQL_ARRAY = {
                "CREATE TABLE " + TABLE_RECORD + " (id INTEGER PRIMARY KEY AUTOINCREMENT, startid INTEGER, endid INTEGER)",
                "CREATE TABLE " + TABLE_LOCATION + " (id INTEGER PRIMARY KEY AUTOINCREMENT, time VARCHAR(50), type INTEGER, lat LONG, lon DOUBLE)",
        };

        public DBHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String sql : CREATE_SQL_ARRAY) {
                db.execSQL(sql);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORD);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
            onCreate(db);
        }
    }
}
