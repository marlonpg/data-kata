package com.example.streams.sink;

import com.example.streams.model.CityAggregation;
import com.example.streams.model.SalesmanAggregation;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class PostgresSink {

    private static final Logger log = LoggerFactory.getLogger(PostgresSink.class);
    private final HikariDataSource dataSource;

    public PostgresSink(
            @Value("${target.db.url}") String url,
            @Value("${target.db.user}") String user,
            @Value("${target.db.password}") String password) {
        this.dataSource = new HikariDataSource();
        this.dataSource.setJdbcUrl(url);
        this.dataSource.setUsername(user);
        this.dataSource.setPassword(password);
        this.dataSource.setMaximumPoolSize(5);
    }

    public void upsertCityAggregation(CityAggregation agg) {
        String sql = "INSERT INTO top_sales_per_city (city, total_amount, sale_count, updated_at) " +
                     "VALUES (?, ?, ?, NOW()) " +
                     "ON CONFLICT (city) DO UPDATE SET " +
                     "total_amount = EXCLUDED.total_amount, " +
                     "sale_count = EXCLUDED.sale_count, " +
                     "updated_at = NOW()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agg.getCity());
            ps.setDouble(2, agg.getTotalAmount());
            ps.setLong(3, agg.getSaleCount());
            ps.executeUpdate();
            log.debug("Upserted city aggregation: city={}, total={}, count={}", agg.getCity(), agg.getTotalAmount(), agg.getSaleCount());
        } catch (SQLException e) {
            log.error("Failed to upsert city aggregation for {}: {}", agg.getCity(), e.getMessage());
        }
    }

    public void upsertSalesmanAggregation(SalesmanAggregation agg) {
        String sql = "INSERT INTO top_salesman_country (salesman, total_amount, sale_count, updated_at) " +
                     "VALUES (?, ?, ?, NOW()) " +
                     "ON CONFLICT (salesman) DO UPDATE SET " +
                     "total_amount = EXCLUDED.total_amount, " +
                     "sale_count = EXCLUDED.sale_count, " +
                     "updated_at = NOW()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agg.getSalesman());
            ps.setDouble(2, agg.getTotalAmount());
            ps.setLong(3, agg.getSaleCount());
            ps.executeUpdate();
            log.debug("Upserted salesman aggregation: salesman={}, total={}, count={}", agg.getSalesman(), agg.getTotalAmount(), agg.getSaleCount());
        } catch (SQLException e) {
            log.error("Failed to upsert salesman aggregation for {}: {}", agg.getSalesman(), e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
