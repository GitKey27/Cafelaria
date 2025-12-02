package com.example.cafelariapos;

public class SalesSummary {
    private String date;
    private double totalSales;
    private int orders;
    private double averageTicket;

    public SalesSummary(String date, double totalSales, int orders, double averageTicket) {
        this.date = date;
        this.totalSales = totalSales;
        this.orders = orders;
        this.averageTicket = averageTicket;
    }

    public String getDate() { return date; }
    public double getTotalSales() { return totalSales; }
    public int getOrders() { return orders; }
    public double getAverageTicket() { return averageTicket; }
}

