CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.agg_top_sales_per_city (
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    city_id VARCHAR(100) NOT NULL,
    city_name VARCHAR(255),
    salesman_id VARCHAR(100) NOT NULL,
    salesman_name VARCHAR(255),
    total_sales NUMERIC(18, 2) NOT NULL,
    rank_in_city INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (period_start, period_end, city_id, salesman_id)
);

CREATE TABLE IF NOT EXISTS analytics.agg_top_salesman_country (
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    salesman_id VARCHAR(100) NOT NULL,
    salesman_name VARCHAR(255),
    total_sales NUMERIC(18, 2) NOT NULL,
    national_rank INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (period_start, period_end, salesman_id)
);
