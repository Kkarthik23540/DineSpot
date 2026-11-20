package com.example.dinespot;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminAddEditRestaurantActivity extends CommonBaseActivity {

    private TextInputEditText etName, etAddress, etPhone;
    private TextInputEditText etTables2, etTables4, etTables6, etTables10;
    private TextInputEditText etOpeningTime, etClosingTime;
    private RadioGroup rgStatus;
    private Button btnSave, btnPickLocation;
    private FloatingActionButton btnPickImage;
    private ImageView ivRestaurantImage;
    private android.widget.ProgressBar btnProgressBar;
    private FirebaseFirestore db;
    private String restaurantId;
    private Uri imageUri;
    private double selectedLat = 0, selectedLng = 0;
    private String existingImageUrl;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(imageUri).into(ivRestaurantImage);
                    ivRestaurantImage.setAlpha(1.0f);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    Glide.with(this).load(imageUri).into(ivRestaurantImage);
                    ivRestaurantImage.setAlpha(1.0f);
                }
            }
    );

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);
                    String address = result.getData().getStringExtra("address");
                    if (etAddress != null) etAddress.setText(address);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_add_edit_restaurant);
        setLightStatusBar(false); // Teal background needs light icons

        initViews();
        setupToolbar();

        db = FirebaseFirestore.getInstance();
        restaurantId = getIntent().getStringExtra("restaurant_id");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(restaurantId != null ? "Edit Restaurant" : "Add Restaurant");
        }

        if (restaurantId != null) loadRestaurantData();

        btnPickImage.setOnClickListener(v -> showImageSourceDialog());
        btnPickLocation.setOnClickListener(v -> mapPickerLauncher.launch(new Intent(this, UserMapPickerActivity.class)));
        
        etOpeningTime.setOnClickListener(v -> showTimePicker(etOpeningTime));
        etClosingTime.setOnClickListener(v -> showTimePicker(etClosingTime));

        btnSave.setOnClickListener(v -> saveRestaurant());
    }

    private void showTimePicker(TextInputEditText et) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selected.set(Calendar.MINUTE, minuteOfHour);
            String time = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selected.getTime());
            et.setText(time);
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        
        etTables2 = findViewById(R.id.etTables2);
        etTables4 = findViewById(R.id.etTables4);
        etTables6 = findViewById(R.id.etTables6);
        etTables10 = findViewById(R.id.etTables10);

        etOpeningTime = findViewById(R.id.etOpeningTime);
        etClosingTime = findViewById(R.id.etClosingTime);
        
        rgStatus = findViewById(R.id.rgStatus);
        ivRestaurantImage = findViewById(R.id.ivRestaurantImageDisplay);
        btnPickImage = findViewById(R.id.btnPickImageRes);
        btnPickLocation = findViewById(R.id.btnPickLocationRes);
        btnSave = findViewById(R.id.btnSave);
        btnProgressBar = findViewById(R.id.btnProgressBar);
    }

    private void setupToolbar() {
        setupToolbar(restaurantId != null ? "Edit Restaurant" : "Add Restaurant", true);
    }

    private void loadRestaurantData() {
        db.collection("restaurants").document(restaurantId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etName.setText(doc.getString("name"));
                etAddress.setText(doc.getString("address"));
                etPhone.setText(doc.getString("phoneNumber"));
                etOpeningTime.setText(doc.getString("openingTime"));
                etClosingTime.setText(doc.getString("closingTime"));
                
                Map<String, Object> rawTypes = (Map<String, Object>) doc.get("tableTypes");
                if (rawTypes != null) {
                    etTables2.setText(String.valueOf(rawTypes.getOrDefault("2", 0)));
                    etTables4.setText(String.valueOf(rawTypes.getOrDefault("4", 0)));
                    etTables6.setText(String.valueOf(rawTypes.getOrDefault("6", 0)));
                    etTables10.setText(String.valueOf(rawTypes.getOrDefault("10", 0)));
                }

                String status = doc.getString("status");
                if (rgStatus != null && status != null) {
                    if ("Full".equalsIgnoreCase(status)) rgStatus.check(R.id.rbFull);
                    else if ("Closed".equalsIgnoreCase(status)) rgStatus.check(R.id.rbClosed);
                    else rgStatus.check(R.id.rbOpen);
                }

                existingImageUrl = doc.getString("imageUrl");
                if (existingImageUrl != null && !existingImageUrl.isEmpty() && ivRestaurantImage != null) {
                    if (existingImageUrl.startsWith("http")) {
                        Glide.with(this).load(existingImageUrl).into(ivRestaurantImage);
                    } else {
                        try {
                            String cleanBase = existingImageUrl;
                            if (existingImageUrl.contains(",")) {
                                String[] parts = existingImageUrl.split(",");
                                if (parts.length > 1) {
                                    cleanBase = parts[1];
                                }
                            }
                            byte[] bytes = Base64.decode(cleanBase.trim(), Base64.DEFAULT);
                            Glide.with(this).load(bytes).into(ivRestaurantImage);
                        } catch (Exception e) {
                            ivRestaurantImage.setImageResource(R.drawable.baseline_image_24);
                        }
                    }
                    ivRestaurantImage.setAlpha(1.0f);
                }

                GeoPoint gp = doc.getGeoPoint("location");
                if (gp != null) { selectedLat = gp.getLatitude(); selectedLng = gp.getLongitude(); }
            }
        });
    }

    private void saveRestaurant() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) { Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show(); return; }

        toggleLoading(true);

        if (imageUri != null) {
            new Thread(() -> {
                String base64Image = convertImageToBase64(imageUri);
                runOnUiThread(() -> {
                    if (base64Image != null) saveToFirestore(name, base64Image);
                    else { toggleLoading(false); }
                });
            }).start();
        } else {
            saveToFirestore(name, existingImageUrl);
        }
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            btnSave.setText("");
            btnSave.setEnabled(false);
            if (btnProgressBar != null) btnProgressBar.setVisibility(View.VISIBLE);
        } else {
            btnSave.setText(restaurantId == null ? "Confirm Restaurant" : "Save Changes");
            btnSave.setEnabled(true);
            if (btnProgressBar != null) btnProgressBar.setVisibility(View.GONE);
        }
    }

    private void saveToFirestore(String name, String imageUrl) {
        Map<String, Integer> tableTypes = new HashMap<>();
        tableTypes.put("2", getIntFromEt(etTables2));
        tableTypes.put("4", getIntFromEt(etTables4));
        tableTypes.put("6", getIntFromEt(etTables6));
        tableTypes.put("10", getIntFromEt(etTables10));

        int totalTables = 0;
        for (int count : tableTypes.values()) totalTables += count;

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("address", etAddress.getText().toString());
        data.put("phoneNumber", etPhone.getText().toString());
        data.put("tableTypes", tableTypes);
        data.put("noOfTables", totalTables);
        data.put("imageUrl", imageUrl);
        data.put("openingTime", etOpeningTime.getText().toString());
        data.put("closingTime", etClosingTime.getText().toString());
        
        String status = "Open";
        int checkedId = rgStatus.getCheckedRadioButtonId();
        if (checkedId == R.id.rbFull) status = "Full";
        else if (checkedId == R.id.rbClosed) status = "Closed";
        
        // Removed forced status logic to allow Admin full control
        if ("Open".equalsIgnoreCase(status) && totalTables <= 0) {
            // Just a warning, not a forced change
            Toast.makeText(this, "Note: Restaurant is Open but has 0 total tables.", Toast.LENGTH_SHORT).show();
        }
        
        data.put("status", status);
        if (selectedLat != 0) data.put("location", new GeoPoint(selectedLat, selectedLng));

        if (restaurantId == null) {
            data.put("rating", 0.0);
            
            // Create a custom Document ID based on name (e.g., "Mehfil", "Avanthi Hotel")
            String customDocId = name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
            
            // Check if this name already exists
            db.collection("restaurants").document(customDocId).get().addOnSuccessListener(doc -> {
                String finalDocId = customDocId;
                if (doc.exists()) {
                    // Append a small random string if collision happens
                    finalDocId = customDocId + "_" + (int)(Math.random() * 1000);
                }
                
                data.put("restaurantId", finalDocId);
                db.collection("restaurants").document(finalDocId).set(data)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Restaurant Added Successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to Add: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        toggleLoading(false);
                    });
            });
        } else {
            db.collection("restaurants").document(restaurantId).update(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Restaurant Updated Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    toggleLoading(false);
                });
        }
    }

    private int getIntFromEt(TextInputEditText et) {
        String s = et.getText().toString().trim();
        return TextUtils.isEmpty(s) ? 0 : Integer.parseInt(s);
    }

    private String convertImageToBase64(Uri uri) {
        try {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            android.graphics.BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();

            // Calculate sample size to downsample directly from disk
            int targetWidth = 600;
            int targetHeight = 400;
            int inSampleSize = 1;
            if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            is = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();

            if (bitmap == null) return null;

            java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, os);
            bitmap.recycle(); // Free memory immediately
            return Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) { return null; }
    }

    private void showImageSourceDialog() {
        String[] options = {"Gallery", "Camera"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Image Source")
                .setItems(options, (d, w) -> {
                    if (w == 0) checkGalleryPermission(); else checkCameraPermission();
                }).show();
    }

    private void checkGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
            else openGallery();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            else openGallery();
        }
    }

    private void openGallery() {
        imagePickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        else dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            imageUri = FileProvider.getUriForFile(this, "com.example.dinespot.fileprovider", photoFile);
            cameraLauncher.launch(imageUri);
        } catch (IOException ex) { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 100) dispatchTakePictureIntent(); else if (requestCode == 101) openGallery();
        }
    }
}
