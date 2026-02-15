package com.weighttracker.app;

import android.app.Application;

/**
 * Custom Application class to ensure the DataStore singleton
 * is available throughout the app lifecycle.
 */
public class WeightTrackerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the DataStore singleton with application context for DB access
        DataStore.getInstance().init(this);
    }
}
