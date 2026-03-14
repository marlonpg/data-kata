package com.example.api.repository;

import com.example.api.model.TopSalesPerCity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopSalesPerCityRepository extends JpaRepository<TopSalesPerCity, String> {

    List<TopSalesPerCity> findAllByOrderByTotalAmountDesc();
}
