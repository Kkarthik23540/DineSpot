package com.example.dinespot;

public class CommonFoodItem {
    private String name;
    private int price;
    private String id;
    private String imageUrl;
    private String restaurantId;
    private String category; // e.g., "Starters", "Main Course", "Drinks"
    private boolean veg = true; // Maps to "veg" in Firestore
    private int quantity = 1; // Default quantity for cart
    private int stockCount = 99; // Default stock
    private boolean isAvailable = true; // Default to available

    public CommonFoodItem() {}

    public CommonFoodItem(String name, int price, String id, String imageUrl, String restaurantId, String category, boolean veg) {
        this.name = name;
        this.price = price;
        this.id = id;
        this.imageUrl = imageUrl;
        this.restaurantId = restaurantId;
        this.category = category;
        this.veg = veg;
        this.quantity = 1;
        this.isAvailable = true;
    }

    @com.google.firebase.firestore.PropertyName("veg")
    public boolean isVeg() { return veg; }
    
    @com.google.firebase.firestore.PropertyName("veg")
    public void setVeg(boolean veg) { this.veg = veg; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public int getStockCount() { return stockCount; }
    public void setStockCount(int stockCount) { this.stockCount = stockCount; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
