package com.weighttracker.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SQLiteOpenHelper for the WeightTracker database.
 * Manages weight_entries and settings tables.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // Weight entries table
    private static final String TABLE_ENTRIES = "weight_entries";
    private static final String COL_ID = "id";
    private static final String COL_DATE = "date";
    private static final String COL_WEIGHT = "weight";
    private static final String COL_IS_METRIC = "is_metric";
    private static final String COL_PHOTO_PATH = "photo_path";

    // Settings table
    private static final String TABLE_SETTINGS = "settings";
    private static final String COL_SETTINGS_ID = "id";
    private static final String COL_GOAL_WEIGHT = "goal_weight";
    private static final String COL_GOAL_DATE = "goal_date";
    private static final String COL_GENDER = "gender";
    private static final String COL_HEIGHT = "height";
    private static final String COL_BEGINNING_WEIGHT = "beginning_weight";
    private static final String COL_SETTINGS_IS_METRIC = "is_metric";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createEntries = "CREATE TABLE " + TABLE_ENTRIES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_DATE + " INTEGER NOT NULL, "
                + COL_WEIGHT + " REAL NOT NULL, "
                + COL_IS_METRIC + " INTEGER NOT NULL, "
                + COL_PHOTO_PATH + " TEXT)";
        db.execSQL(createEntries);

        String createSettings = "CREATE TABLE " + TABLE_SETTINGS + " ("
                + COL_SETTINGS_ID + " INTEGER PRIMARY KEY, "
                + COL_GOAL_WEIGHT + " REAL, "
                + COL_GOAL_DATE + " TEXT, "
                + COL_GENDER + " TEXT, "
                + COL_HEIGHT + " REAL, "
                + COL_BEGINNING_WEIGHT + " REAL, "
                + COL_SETTINGS_IS_METRIC + " INTEGER)";
        db.execSQL(createSettings);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // ─── Weight Entry CRUD ──────────────────────────────────────────────

    /**
     * Inserts a weight entry and returns the auto-generated row ID.
     */
    public long insertEntry(WeightEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, entry.getDate().getTime());
        values.put(COL_WEIGHT, entry.getWeight());
        values.put(COL_IS_METRIC, entry.isMetric() ? 1 : 0);
        values.put(COL_PHOTO_PATH, entry.getPhotoPath());
        long id = db.insert(TABLE_ENTRIES, null, values);
        return id;
    }

    /**
     * Deletes a weight entry by its database ID.
     */
    public void deleteEntry(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ENTRIES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Returns all weight entries sorted by date descending (newest first).
     */
    public List<WeightEntry> getAllEntries() {
        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ENTRIES, null, null, null, null, null,
                COL_DATE + " DESC");

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            long dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE));
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT));
            boolean isMetric = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_METRIC)) == 1;
            String photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH));

            WeightEntry entry = new WeightEntry(id, new Date(dateMs), weight, isMetric, photoPath);
            entries.add(entry);
        }
        cursor.close();
        return entries;
    }

    // ─── Settings CRUD ──────────────────────────────────────────────────

    /**
     * Saves settings using INSERT OR REPLACE with a fixed ID of 1.
     */
    public void saveSettings(double goalWeight, String goalDate, String gender,
                             double height, double beginningWeight, boolean isMetric) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SETTINGS_ID, 1);
        values.put(COL_GOAL_WEIGHT, goalWeight);
        values.put(COL_GOAL_DATE, goalDate);
        values.put(COL_GENDER, gender);
        values.put(COL_HEIGHT, height);
        values.put(COL_BEGINNING_WEIGHT, beginningWeight);
        values.put(COL_SETTINGS_IS_METRIC, isMetric ? 1 : 0);
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Loads settings into the given DataStore instance.
     * Returns true if settings were found, false otherwise.
     */
    public boolean loadSettings(DataStore dataStore) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, null,
                COL_SETTINGS_ID + " = ?", new String[]{"1"},
                null, null, null);

        if (cursor.moveToFirst()) {
            dataStore.setGoalWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_GOAL_WEIGHT)));
            String goalDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_GOAL_DATE));
            dataStore.setGoalDate(goalDate != null ? goalDate : "");
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER));
            dataStore.setGender(gender != null ? gender : "");
            dataStore.setHeight(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HEIGHT)));
            dataStore.setBeginningWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BEGINNING_WEIGHT)));
            dataStore.setMetric(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SETTINGS_IS_METRIC)) == 1);
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }
}
