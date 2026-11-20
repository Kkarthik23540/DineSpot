package com.example.dinespot;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserMapPickerActivity extends CommonBaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvSelectedAddress;
    private EditText etSearch;
    private RecyclerView rvSuggestions;
    private UserSearchSuggestionAdapter suggestionAdapter;
    private Button btnConfirm;
    private FloatingActionButton fabMyLocation;
    private LatLng selectedLatLng;
    private String selectedAddressStr = "";
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private void setupSearchLogic() {
        suggestionAdapter = new UserSearchSuggestionAdapter(new ArrayList<>(), address -> {
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
            rvSuggestions.setVisibility(View.GONE);
            etSearch.setText(address.getAddressLine(0));
            etSearch.clearFocus();
        });

        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    fetchSuggestions(s.toString());
                } else {
                    rvSuggestions.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation(etSearch.getText().toString());
                rvSuggestions.setVisibility(View.GONE);
                return true;
            }
            return false;
        });
    }

    private void fetchSuggestions(String query) {
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                // Geocoding can be slow, keeping it off-thread
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        suggestionAdapter.updateData(addresses);
                        rvSuggestions.setVisibility(View.VISIBLE);
                    } else {
                        rvSuggestions.setVisibility(View.GONE);
                    }
                });
            } catch (IOException ignored) {}
        });
    }

    private void searchLocation(String query) {
        if (query == null || query.isEmpty()) return;

        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty() && mMap != null) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_map_picker);

        setupToolbar("Pick Location", true);

        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        etSearch = findViewById(R.id.et_search_location);
        rvSuggestions = findViewById(R.id.rv_search_suggestions);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        fabMyLocation = findViewById(R.id.fab_my_location);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearchLogic();
        fabMyLocation.setOnClickListener(v -> requestFreshLocation());

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("lat", selectedLatLng.latitude);
                resultIntent.putExtra("lng", selectedLatLng.longitude);
                resultIntent.putExtra("address", selectedAddressStr);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location on map", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 1));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            tvSelectedAddress.setText("Locating...");
            requestFreshLocation();
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 5));
        }

        mMap.setOnMapClickListener(latLng -> {
            rvSuggestions.setVisibility(View.GONE);
            etSearch.clearFocus();
        });

        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                rvSuggestions.setVisibility(View.GONE);
                etSearch.clearFocus();
            }
        });

        mMap.setOnCameraIdleListener(() -> {
            selectedLatLng = mMap.getCameraPosition().target;
            getAddressFromLatLng(selectedLatLng);
        });
    }

    private void centerOnLocation(Location location) {
        if (mMap == null || location == null) return;
        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 18f));
        selectedLatLng = current;
        getAddressFromLatLng(current);
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location freshLocation = locationResult.getLastLocation();
                if (freshLocation != null) {
                    centerOnLocation(freshLocation);
                }
            }
        }, Looper.getMainLooper());
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && selectedLatLng == null) {
                centerOnLocation(location);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    requestFreshLocation();
                }
            }
        }
    }

    private void getAddressFromLatLng(LatLng latLng) {
        if (latLng == null || tvSelectedAddress == null) return;
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        selectedAddressStr = address.getAddressLine(0);
                        tvSelectedAddress.setText(selectedAddressStr);
                    }
                });
            } catch (IOException ignored) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
