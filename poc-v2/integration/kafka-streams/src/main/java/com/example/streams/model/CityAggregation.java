package com.example.streams.model;

public class CityAggregation {

    private String city;
    private double totalAmount;
    private long saleCount;

    public CityAggregation() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public long getSaleCount() { return saleCount; }
    public void setSaleCount(long saleCount) { this.saleCount = saleCount; }
}
