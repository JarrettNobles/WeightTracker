package com.weighttracker.app;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton data store for weight entries and user settings.
 * Backed by SQLite via DatabaseHelper, with an in-memory cache for fast access.
 */
public class DataStore {

    private static DataStore instance;

    // Database helper
    private DatabaseHelper dbHelper;

    // Weight entries list (most recent first) — in-memory cache
    private List<WeightEntry> weightEntries;

    // User settings
    private double goalWeight;
    private String goalDate;       // formatted string e.g. "Dec 31, 2024"
    private String gender;         // "Male", "Female", "Other"
    private double height;         // in cm or inches depending on unit
    private double beginningWeight;
    private boolean isMetric;      // true = kg/cm, false = lbs/in

    private DataStore() {
        weightEntries = new ArrayList<>();
        goalWeight = 0;
        goalDate = "";
        gender = "";
        height = 0;
        beginningWeight = 0;
        isMetric = true; // default to metric
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    /**
     * Initializes the DataStore with a Context, creating the DatabaseHelper
     * and loading persisted data from SQLite.
     */
    public void init(Context context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
        // Load entries from DB into in-memory cache
        weightEntries = dbHelper.getAllEntries();
        // Load settings from DB
        dbHelper.loadSettings(this);
    }

    // ─── Weight Entries ─────────────────────────────────────────────────

    public void addEntry(WeightEntry entry) {
        // Insert into DB and get auto-generated ID
        if (dbHelper != null) {
            long id = dbHelper.insertEntry(entry);
            entry.setId(id);
        }
        // Insert at the beginning so most recent is first
        weightEntries.add(0, entry);
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < weightEntries.size()) {
            WeightEntry entry = weightEntries.get(index);
            // Delete photo file if it exists
            if (entry.getPhotoPath() != null) {
                File photoFile = new File(entry.getPhotoPath());
                if (photoFile.exists()) {
                    photoFile.delete();
                }
            }
            // Delete from DB
            if (dbHelper != null && entry.getId() != -1) {
                dbHelper.deleteEntry(entry.getId());
            }
            weightEntries.remove(index);
        }
    }

    public List<WeightEntry> getWeightEntries() {
        return weightEntries;
    }

    public int getEntryCount() {
        return weightEntries.size();
    }

    /**
     * Returns the most recent weight entry, or null if none exist.
     */
    public WeightEntry getLatestEntry() {
        if (weightEntries.isEmpty()) return null;
        return weightEntries.get(0);
    }

    /**
     * Returns the second most recent entry, or null if fewer than 2 entries.
     */
    public WeightEntry getPreviousEntry() {
        if (weightEntries.size() < 2) return null;
        return weightEntries.get(1);
    }

    // ─── Settings ───────────────────────────────────────────────────────

    public double getGoalWeight() {
        return goalWeight;
    }

    public void setGoalWeight(double goalWeight) {
        this.goalWeight = goalWeight;
    }

    public String getGoalDate() {
        return goalDate;
    }

    public void setGoalDate(String goalDate) {
        this.goalDate = goalDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getBeginningWeight() {
        return beginningWeight;
    }

    public void setBeginningWeight(double beginningWeight) {
        this.beginningWeight = beginningWeight;
    }

    public boolean isMetric() {
        return isMetric;
    }

    public void setMetric(boolean metric) {
        isMetric = metric;
    }

    /**
     * Persists the current settings to the database.
     */
    public void saveSettingsToDb() {
        if (dbHelper != null) {
            dbHelper.saveSettings(goalWeight, goalDate, gender, height, beginningWeight, isMetric);
        }
    }

    // ─── Computed Statistics ────────────────────────────────────────────

    /**
     * Calculates BMI given height (cm or inches) and current weight.
     * Returns 0 if data is insufficient.
     */
    public double calculateBMI() {
        WeightEntry latest = getLatestEntry();
        if (latest == null || height <= 0) return 0;

        double weightKg;
        double heightM;

        if (isMetric) {
            weightKg = latest.getWeight();
            heightM = height / 100.0; // cm to m
        } else {
            weightKg = latest.getWeight() * 0.453592; // lbs to kg
            heightM = height * 0.0254;                // inches to m
        }

        if (heightM <= 0) return 0;
        return weightKg / (heightM * heightM);
    }

    /**
     * Calculates average weekly weight loss based on all entries.
     * Returns 0 if fewer than 2 entries.
     */
    public double calculateAvgWeeklyLoss() {
        if (weightEntries.size() < 2) return 0;

        WeightEntry newest = weightEntries.get(0);
        WeightEntry oldest = weightEntries.get(weightEntries.size() - 1);

        long timeDiffMs = newest.getDate().getTime() - oldest.getDate().getTime();
        double timeDiffWeeks = timeDiffMs / (1000.0 * 60 * 60 * 24 * 7);

        if (timeDiffWeeks <= 0) return 0;

        double weightDiff = newest.getWeight() - oldest.getWeight();
        return weightDiff / timeDiffWeeks;
    }

    /**
     * Calculates total weight loss from beginning weight to current weight.
     * Returns 0 if no entries or no beginning weight set.
     */
    public double calculateTotalLoss() {
        WeightEntry latest = getLatestEntry();
        if (latest == null || beginningWeight <= 0) return 0;
        return latest.getWeight() - beginningWeight;
    }

    /**
     * Returns the change between the two most recent entries.
     * Returns 0 if fewer than 2 entries.
     */
    public double calculateLastChange() {
        if (weightEntries.size() < 2) return 0;
        return weightEntries.get(0).getWeight() - weightEntries.get(1).getWeight();
    }
}
