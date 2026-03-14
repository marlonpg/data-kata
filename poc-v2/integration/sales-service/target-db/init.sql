CREATE TABLE top_sales_per_city (
    city VARCHAR(100) PRIMARY KEY,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    sale_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE top_salesman_country (
    salesman VARCHAR(100) PRIMARY KEY,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    sale_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
