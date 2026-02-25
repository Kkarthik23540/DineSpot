package com.example.dinespot;
import com.google.firebase.firestore.GeoPoint;
public class Restaurant {
    private String name;
    private String address;
    private String imageUrl;
    private String restaurantId;
    private float rating;
    private int noOfTables;
    private GeoPoint location;
    private String description;
    private String status;
    private String openingHours;
    public Restaurant() {}
    public Restaurant(String name, String address, String imageUrl, String restaurantId, float rating,
                      int noOfTables, GeoPoint location, String description, String status, String openingHours) {
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.restaurantId = restaurantId;
        this.rating = rating;
        this.noOfTables = noOfTables;
        this.location = location;
        this.description = description;
        this.status = status;
        this.openingHours = openingHours;}
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
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
    public String getOpeningHours() {return openingHours;}
    public void setOpeningHours(String openingHours) {this.openingHours = openingHours;}
    public void setClickable(boolean b) {}
}
