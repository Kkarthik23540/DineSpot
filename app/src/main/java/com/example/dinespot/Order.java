package com.example.dinespot;

public class Order {
    private String id;
    private String name;
    private int price;
    private String userId;
    private String restaurantId;

    public Order() {}

    public Order(String id, String name, int price, String userId, String restaurantId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.userId = userId;
        this.restaurantId = restaurantId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }
}
