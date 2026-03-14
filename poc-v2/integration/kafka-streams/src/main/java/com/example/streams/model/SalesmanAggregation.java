package com.example.streams.model;

public class SalesmanAggregation {

    private String salesman;
    private double totalAmount;
    private long saleCount;

    public SalesmanAggregation() {}

    public String getSalesman() { return salesman; }
    public void setSalesman(String salesman) { this.salesman = salesman; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public long getSaleCount() { return saleCount; }
    public void setSaleCount(long saleCount) { this.saleCount = saleCount; }
}
