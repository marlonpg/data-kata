package com.example.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "top_salesman_country")
public class TopSalesmanCountry {

    @Id
    private String salesman;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "sale_count")
    private Long saleCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getSalesman() { return salesman; }
    public void setSalesman(String salesman) { this.salesman = salesman; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Long getSaleCount() { return saleCount; }
    public void setSaleCount(Long saleCount) { this.saleCount = saleCount; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
