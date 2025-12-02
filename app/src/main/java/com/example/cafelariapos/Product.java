package com.example.cafelariapos;

public class Product {
    private String id;
    private String name;
    private double price;
    private int stock;
    private String category;
    private int lowStockThreshold;
    private String sku;
    private double taxRate;

    public Product(String id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = "";
        this.lowStockThreshold = 0;
        this.sku = "";
        this.taxRate = 0.0;
    }

    public Product(String id, String name, double price, int stock, String category, int lowStockThreshold, String sku, double taxRate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.lowStockThreshold = lowStockThreshold;
        this.sku = sku;
        this.taxRate = taxRate;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getCategory() { return category; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public String getSku() { return sku; }
    public double getTaxRate() { return taxRate; }

    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setCategory(String category) { this.category = category; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public void setSku(String sku) { this.sku = sku; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
}
