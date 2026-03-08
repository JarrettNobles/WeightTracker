package com.weighttracker.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

/**
 * Settings activity for configuring user profile and preferences.
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etGoalWeight;
    private TextView tvGoalDate;
    private Spinner spinnerGender;
    private EditText etHeight;
    private EditText etBeginningWeight;
    private RadioButton radioImperial;
    private RadioButton radioMetric;

    private TextView tvGoalWeightUnit;
    private TextView tvHeightUnit;
    private TextView tvBeginningWeightUnit;

    private DataStore dataStore;
    private String selectedGoalDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dataStore = DataStore.getInstance();

        // Bind views
        etGoalWeight = findViewById(R.id.et_goal_weight);
        tvGoalDate = findViewById(R.id.tv_goal_date);
        spinnerGender = findViewById(R.id.spinner_gender);
        etHeight = findViewById(R.id.et_height);
        etBeginningWeight = findViewById(R.id.et_beginning_weight);
        radioImperial = findViewById(R.id.radio_imperial);
        radioMetric = findViewById(R.id.radio_metric);

        tvGoalWeightUnit = findViewById(R.id.tv_goal_weight_unit);
        tvHeightUnit = findViewById(R.id.tv_height_unit);
        tvBeginningWeightUnit = findViewById(R.id.tv_beginning_weight_unit);

        // Setup gender spinner
        String[] genderOptions = {"Select Gender", "Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Goal date picker
        findViewById(R.id.ll_goal_date).setOnClickListener(v -> openGoalDatePicker());

        // Unit preference — RadioGroup handles mutual exclusion automatically
        RadioGroup radioGroupUnit = findViewById(R.id.radio_group_unit);
        radioGroupUnit.setOnCheckedChangeListener((group, checkedId) -> {
            updateUnitLabels(checkedId == R.id.radio_metric);
        });

        // Save button
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveSettings());

        // Load existing settings
        loadSettings();
    }

    /**
     * Opens a DatePickerDialog for selecting the goal date.
     */
    private void openGoalDatePicker() {
        Calendar cal = Calendar.getInstance();
        // Default to one year from now
        cal.add(Calendar.YEAR, 1);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    selectedGoalDate = months[month] + " " + dayOfMonth + ", " + year;
                    tvGoalDate.setText(selectedGoalDate);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    /**
     * Updates unit label text views based on the selected unit preference.
     */
    private void updateUnitLabels(boolean isMetric) {
        String weightUnit = isMetric ? "kg" : "lbs";
        String heightUnit = isMetric ? "cm" : "in";

        tvGoalWeightUnit.setText(weightUnit);
        tvHeightUnit.setText(heightUnit);
        tvBeginningWeightUnit.setText(weightUnit);
    }

    /**
     * Loads previously saved settings into the UI fields.
     */
    private void loadSettings() {
        // Goal weight
        if (dataStore.getGoalWeight() > 0) {
            etGoalWeight.setText(String.format("%.1f", dataStore.getGoalWeight()));
        }

        // Goal date
        if (!dataStore.getGoalDate().isEmpty()) {
            selectedGoalDate = dataStore.getGoalDate();
            tvGoalDate.setText(selectedGoalDate);
        }

        // Gender
        String gender = dataStore.getGender();
        if (!gender.isEmpty()) {
            String[] genderOptions = {"Select Gender", "Male", "Female", "Other"};
            for (int i = 0; i < genderOptions.length; i++) {
                if (genderOptions[i].equals(gender)) {
                    spinnerGender.setSelection(i);
                    break;
                }
            }
        }

        // Height
        if (dataStore.getHeight() > 0) {
            etHeight.setText(String.format("%.1f", dataStore.getHeight()));
        }

        // Beginning weight
        if (dataStore.getBeginningWeight() > 0) {
            etBeginningWeight.setText(String.format("%.1f", dataStore.getBeginningWeight()));
        }

        // Unit preference
        if (dataStore.isMetric()) {
            radioMetric.setChecked(true);
        } else {
            radioImperial.setChecked(true);
        }

        // Update unit labels
        updateUnitLabels(dataStore.isMetric());
    }

    /**
     * Validates and saves all settings to the DataStore.
     */
    private void saveSettings() {
        // Goal weight
        String goalWeightStr = etGoalWeight.getText().toString().trim();
        if (!goalWeightStr.isEmpty()) {
            try {
                dataStore.setGoalWeight(Double.parseDouble(goalWeightStr));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid goal weight.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Goal date
        if (selectedGoalDate != null) {
            dataStore.setGoalDate(selectedGoalDate);
        }

        // Gender
        int genderIndex = spinnerGender.getSelectedItemPosition();
        if (genderIndex > 0) {
            String[] genderOptions = {"Select Gender", "Male", "Female", "Other"};
            dataStore.setGender(genderOptions[genderIndex]);
        }

        // Height
        String heightStr = etHeight.getText().toString().trim();
        if (!heightStr.isEmpty()) {
            try {
                dataStore.setHeight(Double.parseDouble(heightStr));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid height.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Beginning weight
        String beginWeightStr = etBeginningWeight.getText().toString().trim();
        if (!beginWeightStr.isEmpty()) {
            try {
                dataStore.setBeginningWeight(Double.parseDouble(beginWeightStr));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid beginning weight.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Unit preference
        boolean isMetric = radioMetric.isChecked();
        dataStore.setMetric(isMetric);

        // Persist settings to database
        dataStore.saveSettingsToDb();

        // Show success and go back
        Toast.makeText(this, "Settings saved successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
