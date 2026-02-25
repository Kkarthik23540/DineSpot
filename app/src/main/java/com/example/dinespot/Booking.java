package com.example.dinespot;

public class Booking {
    private String restaurantName;
    private String dateTime;
    private String timeSlot;
    private String userName;
    private String status;
    private String phoneNumber;

    // Constructor
    public Booking(String restaurantName, String dateTime, String timeSlot, String userName, String phoneNumber) {
        this.restaurantName = restaurantName;
        this.dateTime = dateTime;
        this.timeSlot = timeSlot;
        this.userName = userName;
        this.status = status;
        this.phoneNumber = phoneNumber;
    }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
