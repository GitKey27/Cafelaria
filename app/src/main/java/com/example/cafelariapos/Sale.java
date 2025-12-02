package com.example.cafelariapos;

import java.util.List;

public class Sale {
    private String id;
    private List<SaleItem> items;
    private double total;
    private long timestamp;

    public Sale(String id, List<SaleItem> items, double total, long timestamp) {
        this.id = id;
        this.items = items;
        this.total = total;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public List<SaleItem> getItems() { return items; }
    public double getTotal() { return total; }
    public long getTimestamp() { return timestamp; }
}

class SaleItem {
    private String productId;
    private String name;
    private int quantity;
    private double price;

    public SaleItem(String productId, String name, int quantity, double price) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

