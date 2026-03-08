package com.weighttracker.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Main activity — the home screen of the Weight Tracker app.
 * Displays current weight, BMI, avg weekly loss, total loss, and action buttons.
 */
public class MainActivity extends AppCompatActivity {

    // Weight display
    private TextView tvCurrentWeight;
    private TextView tvWeightUnitMain;
    private TextView tvLastEntry;
    private TextView tvChange;

    // Stats
    private TextView tvBmi;
    private TextView tvAvgWeeklyLoss;
    private TextView tvLossToDate;

    // Tip card
    private androidx.cardview.widget.CardView cardTip;

    private DataStore dataStore;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataStore = DataStore.getInstance();

        // Bind views
        tvCurrentWeight = findViewById(R.id.tv_current_weight);
        tvWeightUnitMain = findViewById(R.id.tv_weight_unit_main);
        tvLastEntry = findViewById(R.id.tv_last_entry);
        tvChange = findViewById(R.id.tv_change);
        tvBmi = findViewById(R.id.tv_bmi);
        tvAvgWeeklyLoss = findViewById(R.id.tv_avg_weekly_loss);
        tvLossToDate = findViewById(R.id.tv_loss_to_date);
        cardTip = findViewById(R.id.card_tip);

        // Button listeners
        findViewById(R.id.btn_add_weight_entry).setOnClickListener(v ->
                startActivity(new Intent(this, AddEntryActivity.class)));

        // Tip card dismiss
        findViewById(R.id.btn_got_it).setOnClickListener(v ->
                cardTip.setVisibility(View.GONE));

        // Bottom nav listeners
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            // Already on home — do nothing
        });
        findViewById(R.id.nav_history).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDisplay();
    }

    /**
     * Refreshes all displayed statistics from the DataStore.
     */
    private void updateDisplay() {
        String unit = dataStore.isMetric() ? "kg" : "lbs";
        tvWeightUnitMain.setText(unit);

        WeightEntry latest = dataStore.getLatestEntry();

        if (latest != null) {
            // Current weight
            tvCurrentWeight.setText(String.format("%.1f", latest.getWeight()));

            // Last entry date
            String dateStr = DATE_FORMAT.format(latest.getDate()).toUpperCase();
            tvLastEntry.setText("LAST ENTRY, " + dateStr);

            // Change from previous entry
            double change = dataStore.calculateLastChange();
            if (dataStore.getEntryCount() >= 2) {
                String changeStr = formatChange(change, unit);
                tvChange.setText("CHANGE: " + changeStr);
                tvChange.setTextColor(change <= 0
                        ? ContextCompat.getColor(this, R.color.primary_red)
                        : ContextCompat.getColor(this, R.color.green_positive));
            } else {
                tvChange.setText("");
            }
        } else {
            tvCurrentWeight.setText("--");
            tvLastEntry.setText("No entries yet");
            tvBmi.setText("--");
            tvChange.setText("");
        }

        // BMI
        double bmi = dataStore.calculateBMI();
        tvBmi.setText(bmi > 0 ? String.format("%.1f", bmi) : "--");

        // Avg weekly loss
        double avgWeeklyLoss = dataStore.calculateAvgWeeklyLoss();
        if (avgWeeklyLoss != 0) {
            tvAvgWeeklyLoss.setText(formatChange(avgWeeklyLoss, unit));
        } else {
            tvAvgWeeklyLoss.setText("--");
        }

        // Total loss to date
        double totalLoss = dataStore.calculateTotalLoss();
        if (totalLoss != 0) {
            tvLossToDate.setText(formatChange(totalLoss, unit));
        } else {
            tvLossToDate.setText("--");
        }
    }

    /**
     * Formats a change value with sign and unit.
     * Negative = loss (shown as e.g. "-2.5 kg"), positive = gain ("+1.0 kg").
     */
    private String formatChange(double change, String unit) {
        if (change < 0) {
            return String.format("-%.1f %s", Math.abs(change), unit);
        } else if (change > 0) {
            return String.format("+%.1f %s", change, unit);
        } else {
            return String.format("0.0 %s", unit);
        }
    }
}
