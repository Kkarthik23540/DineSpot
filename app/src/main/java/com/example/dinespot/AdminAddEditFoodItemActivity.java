package com.example.dinespot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminAddEditFoodItemActivity extends CommonBaseActivity {

    private TextInputEditText etName, etPrice, etStock;
    private android.widget.AutoCompleteTextView spinnerCategory;
    private SwitchMaterial switchAvailable;
    private android.widget.RadioGroup rgDietary;
    private Button btnSave;
    private android.widget.ProgressBar btnProgressBar;
    private FloatingActionButton btnPickImage;
    private ImageView ivFoodImage;
    private FirebaseFirestore db;
    private String restaurantId, foodItemId;
    private Uri imageUri;
    private String existingImageUrl;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(imageUri).into(ivFoodImage);
                    ivFoodImage.setAlpha(1.0f);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    Glide.with(this).load(imageUri).into(ivFoodImage);
                    ivFoodImage.setAlpha(1.0f);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_add_edit_food_item);
        setLightStatusBar(false); // Teal background needs light icons

        db = FirebaseFirestore.getInstance();
        // storage initialization removed as we are using Base64 workaround

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etStock = findViewById(R.id.etStock);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchAvailable = findViewById(R.id.switchAvailable);
        rgDietary = findViewById(R.id.rgDietary);
        ivFoodImage = findViewById(R.id.ivFoodImage);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnSave = findViewById(R.id.btnSave);
        btnProgressBar = findViewById(R.id.btnProgressBar);

        restaurantId = getIntent().getStringExtra("restaurant_id");
        foodItemId = getIntent().getStringExtra("food_item_id");

        setupToolbar(foodItemId != null ? "Edit Food Item" : "Add Food Item", true);
        setupCategorySpinner();

        if (foodItemId != null) {
            loadFoodItemData();
        }

        btnPickImage.setOnClickListener(v -> showImageSourceDialog());
        btnSave.setOnClickListener(v -> saveFoodItem());
    }

    private void showImageSourceDialog() {
        String[] options = {"Gallery", "Camera"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkGalleryPermission();
                    else checkCameraPermission();
                })
                .show();
    }

    private void checkGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            } else {
                openGallery();
            }
        }
    }

    private void openGallery() {
        imagePickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = createImageFile();
            imageUri = FileProvider.getUriForFile(this,
                    "com.example.dinespot.fileprovider",
                    photoFile);
            cameraLauncher.launch(imageUri);
        } catch (IOException ex) {
            Toast.makeText(this, "File error", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 100) dispatchTakePictureIntent();
            else if (requestCode == 101) openGallery();
        }
    }

    private void setupCategorySpinner() {
        String[] categories = {"Starters", "Main Course", "Drinks", "Desserts", "Bread", "Rice", "Others"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        if (spinnerCategory != null) {
            spinnerCategory.setAdapter(adapter);
            spinnerCategory.setText(categories[categories.length - 1], false); // Default: Others
        }
    }

    private void loadFoodItemData() {
        if (restaurantId == null || foodItemId == null) return;
        db.collection("restaurants").document(restaurantId)
                .collection("foodItems").document(foodItemId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        if (etName != null) etName.setText(doc.getString("name"));
                        if (etPrice != null) etPrice.setText(String.valueOf(doc.getLong("price")));
                        if (etStock != null) etStock.setText(String.valueOf(doc.getLong("stockCount") != null ? doc.getLong("stockCount") : 99));
                        
                        String cat = doc.getString("category");
                        if (spinnerCategory != null && cat != null) spinnerCategory.setText(cat, false);
                        
                        if (rgDietary != null) {
                            Boolean isVeg = doc.getBoolean("veg");
                            if (isVeg != null && !isVeg) {
                                rgDietary.check(R.id.rbNonVeg);
                            } else {
                                rgDietary.check(R.id.rbVeg);
                            }
                        }

                        if (switchAvailable != null) {
                            Boolean available = doc.getBoolean("available");
                            switchAvailable.setChecked(available != null ? available : true);
                        }
                        existingImageUrl = doc.getString("imageUrl");
                        if (existingImageUrl != null && !existingImageUrl.isEmpty() && ivFoodImage != null) {
                            if (existingImageUrl.startsWith("http")) {
                                Glide.with(this).load(existingImageUrl).into(ivFoodImage);
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
                                    Glide.with(this).load(bytes).into(ivFoodImage);
                                } catch (Exception e) {
                                    ivFoodImage.setImageResource(R.drawable.baseline_shopping_cart_24);
                                }
                            }
                            ivFoodImage.setAlpha(1.0f);
                        }
                    }
                });
    }

    private void saveFoodItem() {
        String name = etName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Fields missing", Toast.LENGTH_SHORT).show();
            return;
        }

        int stock = TextUtils.isEmpty(stockStr) ? 99 : Integer.parseInt(stockStr);

        toggleLoading(true);
        if (imageUri != null) {
            // Run processing in background to avoid "App keep stopping" (ANR)
            new Thread(() -> {
                String base64Image = convertImageToBase64(imageUri);
                runOnUiThread(() -> {
                    if (base64Image != null) {
                        saveToFirestore(name, priceStr, stock, base64Image);
                    } else {
                        toggleLoading(false);
                        Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } else {
            saveToFirestore(name, priceStr, stock, existingImageUrl);
        }
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            btnSave.setText("");
            btnSave.setEnabled(false);
            if (btnProgressBar != null) btnProgressBar.setVisibility(View.VISIBLE);
        } else {
            btnSave.setText(foodItemId == null ? "Add Item" : "Save Changes");
            btnSave.setEnabled(true);
            if (btnProgressBar != null) btnProgressBar.setVisibility(View.GONE);
        }
    }

    private String convertImageToBase64(Uri uri) {
        try {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            int targetWidth = 500;
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
            inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) return null;

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            bitmap.recycle();
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    private void uploadImageAndSave(String name, String price) {
        // This method is no longer needed for Base64 workaround
    }

    private void saveToFirestore(String name, String priceStr, int stock, String url) {
        int price = Integer.parseInt(priceStr);
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("price", price);
        data.put("stockCount", stock);
        data.put("category", spinnerCategory != null ? spinnerCategory.getText().toString() : "Others");
        data.put("veg", rgDietary != null && rgDietary.getCheckedRadioButtonId() == R.id.rbVeg);
        data.put("imageUrl", url);
        data.put("restaurantId", restaurantId);
        data.put("available", switchAvailable.isChecked());

        if (foodItemId == null) {
            com.google.firebase.firestore.DocumentReference newFoodRef = db.collection("restaurants").document(restaurantId).collection("foodItems").document();
            data.put("id", newFoodRef.getId());
            newFoodRef.set(data).addOnSuccessListener(v -> {
                Toast.makeText(this, "Food Item Added Successfully!", Toast.LENGTH_LONG).show();
                finish();
            }).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            db.collection("restaurants").document(restaurantId).collection("foodItems").document(foodItemId).update(data).addOnSuccessListener(v -> {
                Toast.makeText(this, "Food Item Updated Successfully!", Toast.LENGTH_LONG).show();
                finish();
            }).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}
