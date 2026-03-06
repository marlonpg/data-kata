package com.example.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AnalyticsController {

    @GetMapping("/v1/analytics/top-sales-per-city")
    public Map<String, Object> topSalesPerCity(
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(defaultValue = "10") int limit) {
        return Map.of(
                "period", period,
                "limit", limit,
                "items", List.of(),
                "message", "Scaffold endpoint. Replace with DB-backed query."
        );
    }

    @GetMapping("/v1/analytics/top-salesman-country")
    public Map<String, Object> topSalesmanCountry(
            @RequestParam(defaultValue = "monthly") String period) {
        return Map.of(
                "period", period,
                "items", List.of(),
                "message", "Scaffold endpoint. Replace with DB-backed query."
        );
    }
}
