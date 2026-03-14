package com.example.api.controller;

import com.example.api.model.TopSalesPerCity;
import com.example.api.model.TopSalesmanCountry;
import com.example.api.repository.TopSalesPerCityRepository;
import com.example.api.repository.TopSalesmanCountryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final TopSalesPerCityRepository cityRepository;
    private final TopSalesmanCountryRepository salesmanRepository;

    public AnalyticsController(TopSalesPerCityRepository cityRepository,
                               TopSalesmanCountryRepository salesmanRepository) {
        this.cityRepository = cityRepository;
        this.salesmanRepository = salesmanRepository;
    }

    @GetMapping("/top-sales-per-city")
    public List<TopSalesPerCity> topSalesPerCity() {
        return cityRepository.findAllByOrderByTotalAmountDesc();
    }

    @GetMapping("/top-salesman")
    public List<TopSalesmanCountry> topSalesman() {
        return salesmanRepository.findAllByOrderByTotalAmountDesc();
    }
}
