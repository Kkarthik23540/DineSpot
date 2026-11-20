package com.example.dinespot;

import java.util.List;
import java.util.Map;

public class CommonBooking {
    private String id;
    private String restaurantName;
    private String restaurantId;
    private String userId;
    private String dateTime;
    private String timeSlot;
    private String userName;
    private String status;
    private String phoneNumber;
    private String tableType;
    private String specialRequests;
    private String orderStatus; // "none", "Received", "Preparing", "Served"
    private List<Map<String, Object>> orderedItems;

    public CommonBooking() {}

    public CommonBooking(String id, String restaurantName, String dateTime, String timeSlot, String userName, String phoneNumber, String status, String restaurantId, String tableType, String userId, String specialRequests, String orderStatus, List<Map<String, Object>> orderedItems) {
        this.id = id;
        this.restaurantName = restaurantName;
        this.dateTime = dateTime;
        this.timeSlot = timeSlot;
        this.userName = userName;
        this.status = status;
        this.phoneNumber = phoneNumber;
        this.restaurantId = restaurantId;
        this.tableType = tableType;
        this.userId = userId;
        this.specialRequests = specialRequests;
        this.orderStatus = orderStatus;
        this.orderedItems = orderedItems;
    }

    public List<Map<String, Object>> getOrderedItems() { return orderedItems; }
    public void setOrderedItems(List<Map<String, Object>> orderedItems) { this.orderedItems = orderedItems; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public String getOrderStatus() { return orderStatus != null ? orderStatus : "none"; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTableType() { return tableType; }
    public void setTableType(String tableType) { this.tableType = tableType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

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
