package com.example.dinespot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class CommonRestaurant {
    private String name;
    private String address;
    private String imageUrl;
    private String restaurantId;
    private float rating;
    private int noOfTables;
    private GeoPoint location;
    private String status;
    private String phoneNumber;
    private String category; // New: e.g., "Italian", "Indian", "Desserts"
    private boolean isFavorite;
    private double distance; // Transient field for UI

    // Operating Hours
    private String openingTime; // e.g., "09:00 AM"
    private String closingTime; // e.g., "11:00 PM"

    // New: Specific Table Counts
    // Keys: "2", "4", "6", "10"
    private Map<String, Object> tableTypes = new HashMap<>();

    public CommonRestaurant() {}

    public CommonRestaurant(String name, String address, String imageUrl, String restaurantId, float rating,
                      int noOfTables, GeoPoint location, String status, String phoneNumber, String category) {
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.restaurantId = restaurantId;
        this.rating = rating;
        this.noOfTables = noOfTables;
        this.location = location;
        this.status = status;
        this.phoneNumber = phoneNumber;
        this.category = category;
    }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    @PropertyName("category")
    public String getCategory() { return category; }
    
    @PropertyName("category")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("tableTypes")
    public Map<String, Object> getTableTypes() { return tableTypes; }
    
    @PropertyName("tableTypes")
    public void setTableTypes(Map<String, Object> tableTypes) { this.tableTypes = tableTypes; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getAddress() {return address;}
    public void setAddress(String address) {this.address = address;}
    public String getImageUrl() {return imageUrl;}
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl;}
    public String getRestaurantId() {return restaurantId;}
    public void setRestaurantId(String restaurantId) {this.restaurantId = restaurantId;}
    public float getRating() {return rating;}
    public void setRating(float rating) {this.rating = rating;}
    public int getNoOfTables() {return noOfTables;}
    public void setNoOfTables(int noOfTables) {this.noOfTables = noOfTables;}
    public GeoPoint getLocation() {return location;}
    public void setLocation(GeoPoint location) {this.location = location;}
    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
    public String getPhoneNumber() {return phoneNumber;}
    public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;}
}
