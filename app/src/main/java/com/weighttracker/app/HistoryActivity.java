package com.weighttracker.app;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * History activity displaying all weight entries, a trend graph, and filter tabs.
 */
public class HistoryActivity extends AppCompatActivity {

    private Button btnTab7Days;
    private Button btnTab30Days;
    private Button btnTabAll;
    private WeightGraphView graphView;
    private RecyclerView recyclerView;
    private TextView tvNoHistory;

    private HistoryAdapter adapter;
    private DataStore dataStore;

    // Current filter: 0 = 7 days, 1 = 30 days, 2 = all
    private int currentFilter = 1; // default 30 days

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dataStore = DataStore.getInstance();

        // Bind views
        btnTab7Days = findViewById(R.id.btn_tab_7days);
        btnTab30Days = findViewById(R.id.btn_tab_30days);
        btnTabAll = findViewById(R.id.btn_tab_all);
        graphView = findViewById(R.id.graph_view);
        recyclerView = findViewById(R.id.recyclerview_history);
        tvNoHistory = findViewById(R.id.tv_no_history);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this, new ArrayList<>(dataStore.getWeightEntries()),
                (entryId) -> showDeleteConfirmation(entryId),
                (entryId) -> showPhotoDialog(entryId));
        recyclerView.setAdapter(adapter);

        // Tab listeners
        btnTab7Days.setOnClickListener(v -> setFilter(0));
        btnTab30Days.setOnClickListener(v -> setFilter(1));
        btnTabAll.setOnClickListener(v -> setFilter(2));

        // Set default tab
        setFilter(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    /**
     * Sets the active filter tab and updates the UI.
     */
    private void setFilter(int filter) {
        currentFilter = filter;

        // Update tab button styles
        btnTab7Days.setBackground(ContextCompat.getDrawable(this,
                filter == 0 ? R.drawable.tab_selected : R.drawable.tab_unselected));
        btnTab7Days.setTextColor(filter == 0
                ? ContextCompat.getColor(this, R.color.tab_selected_text)
                : ContextCompat.getColor(this, R.color.tab_unselected_text));

        btnTab30Days.setBackground(ContextCompat.getDrawable(this,
                filter == 1 ? R.drawable.tab_selected : R.drawable.tab_unselected));
        btnTab30Days.setTextColor(filter == 1
                ? ContextCompat.getColor(this, R.color.tab_selected_text)
                : ContextCompat.getColor(this, R.color.tab_unselected_text));

        btnTabAll.setBackground(ContextCompat.getDrawable(this,
                filter == 2 ? R.drawable.tab_selected : R.drawable.tab_unselected));
        btnTabAll.setTextColor(filter == 2
                ? ContextCompat.getColor(this, R.color.tab_selected_text)
                : ContextCompat.getColor(this, R.color.tab_unselected_text));

        refreshData();
    }

    /**
     * Refreshes the list and graph based on the current filter.
     */
    private void refreshData() {
        List<WeightEntry> allEntries = dataStore.getWeightEntries();
        List<WeightEntry> filteredEntries = filterEntries(allEntries);

        // Update adapter and graph with filtered entries
        adapter.updateEntries(new ArrayList<>(filteredEntries));
        graphView.setEntries(filteredEntries);
        graphView.invalidate();

        // Show/hide empty state
        if (filteredEntries.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Filters entries based on the current tab selection.
     */
    private List<WeightEntry> filterEntries(List<WeightEntry> entries) {
        if (currentFilter == 2) {
            // All
            return entries;
        }

        int daysBack = (currentFilter == 0) ? 7 : 30;
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DAY_OF_MONTH, -daysBack);

        List<WeightEntry> filtered = new ArrayList<>();
        for (WeightEntry entry : entries) {
            if (!entry.getDate().before(cutoff.getTime())) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    /**
     * Shows a confirmation dialog before deleting an entry.
     */
    private void showDeleteConfirmation(long entryId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dataStore.removeEntryById(entryId);
                    refreshData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog with the progress photo for the given entry, if it has one.
     */
    private void showPhotoDialog(long entryId) {
        WeightEntry entry = null;
        for (WeightEntry e : dataStore.getWeightEntries()) {
            if (e.getId() == entryId) {
                entry = e;
                break;
            }
        }
        if (entry == null || entry.getPhotoPath() == null) return;

        File photoFile = new File(entry.getPhotoPath());
        if (!photoFile.exists()) return;

        // Build a dialog with the photo
        ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setImageBitmap(BitmapFactory.decodeFile(entry.getPhotoPath()));

        new AlertDialog.Builder(this)
                .setTitle("Progress Photo")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .show();
    }
}
