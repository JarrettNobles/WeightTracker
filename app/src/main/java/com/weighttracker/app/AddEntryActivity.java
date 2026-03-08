package com.weighttracker.app;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for adding a new weight entry.
 * Allows the user to pick a date, enter a weight, take a progress photo, and save.
 */
public class AddEntryActivity extends AppCompatActivity {

    private TextView tvDate;
    private EditText etWeight;
    private TextView tvWeightUnit;
    private TextView tvError;
    private androidx.cardview.widget.CardView cardTip;
    private ImageView ivPhotoPreview;
    private View btnRemovePhoto;
    private View btnAddPhoto;

    private DataStore dataStore;
    private Date selectedDate;
    private String capturedPhotoPath;
    private Uri photoUri;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);

    // Camera launcher
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && capturedPhotoPath != null) {
                    ivPhotoPreview.setImageURI(null); // clear cache
                    ivPhotoPreview.setImageURI(photoUri);
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    btnRemovePhoto.setVisibility(View.VISIBLE);
                    btnAddPhoto.setVisibility(View.GONE);
                } else {
                    // Camera cancelled or failed — clean up temp file
                    if (capturedPhotoPath != null) {
                        new File(capturedPhotoPath).delete();
                        capturedPhotoPath = null;
                        photoUri = null;
                    }
                }
            });

    // Permission launcher
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        dataStore = DataStore.getInstance();

        // Bind views
        tvDate = findViewById(R.id.tv_date);
        etWeight = findViewById(R.id.et_weight);
        tvWeightUnit = findViewById(R.id.tv_weight_unit);
        tvError = findViewById(R.id.tv_error);
        cardTip = findViewById(R.id.card_tip);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        btnRemovePhoto = findViewById(R.id.btn_remove_photo);
        btnAddPhoto = findViewById(R.id.btn_add_photo);

        // Set default date to today
        selectedDate = new Date();
        tvDate.setText(DATE_FORMAT.format(selectedDate));

        // Set unit label
        tvWeightUnit.setText(dataStore.isMetric() ? "kg" : "lbs");

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Cancel button
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        // Date picker click
        findViewById(R.id.tv_date).setOnClickListener(v -> openDatePicker());
        findViewById(R.id.iv_calendar_icon).setOnClickListener(v -> openDatePicker());

        // Save button
        findViewById(R.id.btn_save).setOnClickListener(v -> saveEntry());

        // Tip card dismiss
        findViewById(R.id.btn_got_it).setOnClickListener(v ->
                cardTip.setVisibility(View.GONE));

        // Progress photo button
        btnAddPhoto.setOnClickListener(v -> onAddPhotoClicked());

        // Remove photo button
        btnRemovePhoto.setOnClickListener(v -> removePhoto());
    }

    /**
     * Handles the Add Progress Photo button click.
     * Checks camera permission, then launches the camera.
     */
    private void onAddPhotoClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Creates a temp photo file and launches the camera intent.
     */
    private void launchCamera() {
        File photosDir = new File(getFilesDir(), "photos");
        if (!photosDir.exists()) {
            photosDir.mkdirs();
        }

        String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
        File photoFile = new File(photosDir, fileName);
        capturedPhotoPath = photoFile.getAbsolutePath();

        photoUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider", photoFile);

        takePictureLauncher.launch(photoUri);
    }

    /**
     * Removes the captured photo preview and deletes the temp file.
     */
    private void removePhoto() {
        if (capturedPhotoPath != null) {
            new File(capturedPhotoPath).delete();
            capturedPhotoPath = null;
            photoUri = null;
        }
        ivPhotoPreview.setImageDrawable(null);
        ivPhotoPreview.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
        btnAddPhoto.setVisibility(View.VISIBLE);
    }

    /**
     * Opens an Android DatePickerDialog for the user to select a date.
     */
    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    selectedDate = selected.getTime();
                    tvDate.setText(DATE_FORMAT.format(selectedDate));
                    // Clear any date error
                    tvError.setVisibility(View.GONE);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
    }

    /**
     * Validates input and saves the weight entry to the DataStore.
     */
    private void saveEntry() {
        String weightStr = etWeight.getText().toString().trim();

        // Validate weight
        if (weightStr.isEmpty()) {
            showError("Weight cannot be empty.");
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
            if (weight <= 0) {
                showError("Please enter a valid weight value.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid weight value.");
            return;
        }

        // Validate date
        if (selectedDate == null) {
            showError("Please select a valid date.");
            return;
        }

        // Create and save entry
        WeightEntry entry = new WeightEntry(selectedDate, weight, dataStore.isMetric());
        if (capturedPhotoPath != null) {
            entry.setPhotoPath(capturedPhotoPath);
        }
        dataStore.addEntry(entry);

        // Navigate back to main screen
        finish();
    }

    /**
     * Displays a validation error message.
     */
    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
