package com.example.streams.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleEvent {

    @JsonProperty("sale_id")
    private long saleId;

    private double amount;

    private String description;

    private String city;

    private String country;

    private String salesman;

    private String source;

    @JsonProperty("created_date")
    private long createdDate;

    public SaleEvent() {}

    public long getSaleId() { return saleId; }
    public void setSaleId(long saleId) { this.saleId = saleId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getSalesman() { return salesman; }
    public void setSalesman(String salesman) { this.salesman = salesman; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getCreatedDate() { return createdDate; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }
}
