package com.example.api.repository;

import com.example.api.model.TopSalesmanCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopSalesmanCountryRepository extends JpaRepository<TopSalesmanCountry, String> {

    List<TopSalesmanCountry> findAllByOrderByTotalAmountDesc();
}
