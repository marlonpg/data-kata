-- =============================================
-- Flink SQL Pipeline Definitions
-- =============================================

SET 'execution.checkpointing.interval' = '30s';

-- =============================================
-- SOURCE TABLES (Kafka)
-- =============================================

CREATE TABLE sales_db (
  sale_id BIGINT,
  amount DOUBLE,
  description STRING,
  city STRING,
  country STRING,
  salesman STRING,
  `source` STRING,
  created_date BIGINT
) WITH (
  'connector' = 'kafka',
  'topic' = 'sales-db',
  'properties.bootstrap.servers' = 'kafka:29092',
  'properties.group.id' = 'flink-pipeline',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json',
  'json.ignore-parse-errors' = 'true'
);

CREATE TABLE sales_file (
  sale_id BIGINT,
  amount DOUBLE,
  description STRING,
  city STRING,
  country STRING,
  salesman STRING,
  `source` STRING,
  created_date BIGINT
) WITH (
  'connector' = 'kafka',
  'topic' = 'sales-file',
  'properties.bootstrap.servers' = 'kafka:29092',
  'properties.group.id' = 'flink-pipeline',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json',
  'json.ignore-parse-errors' = 'true'
);

CREATE TABLE sales_ws (
  sale_id BIGINT,
  amount DOUBLE,
  description STRING,
  city STRING,
  country STRING,
  salesman STRING,
  `source` STRING,
  created_date BIGINT
) WITH (
  'connector' = 'kafka',
  'topic' = 'sales-ws',
  'properties.bootstrap.servers' = 'kafka:29092',
  'properties.group.id' = 'flink-pipeline',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json',
  'json.ignore-parse-errors' = 'true'
);

-- =============================================
-- UNIFIED VIEW (merge all 3 sources)
-- =============================================

CREATE TEMPORARY VIEW all_sales AS
SELECT sale_id, amount, city, salesman FROM sales_db
UNION ALL
SELECT sale_id, amount, city, salesman FROM sales_file
UNION ALL
SELECT sale_id, amount, city, salesman FROM sales_ws;

-- =============================================
-- SINK TABLES (PostgreSQL)
-- =============================================

CREATE TABLE top_sales_per_city_sink (
  city STRING,
  total_amount DECIMAL(15,2),
  sale_count BIGINT,
  updated_at TIMESTAMP(3),
  PRIMARY KEY (city) NOT ENFORCED
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:postgresql://target-db:5432/analyticsdb',
  'table-name' = 'top_sales_per_city',
  'username' = 'analytics',
  'password' = 'analytics123',
  'driver' = 'org.postgresql.Driver'
);

CREATE TABLE top_salesman_country_sink (
  salesman STRING,
  total_amount DECIMAL(15,2),
  sale_count BIGINT,
  updated_at TIMESTAMP(3),
  PRIMARY KEY (salesman) NOT ENFORCED
) WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:postgresql://target-db:5432/analyticsdb',
  'table-name' = 'top_salesman_country',
  'username' = 'analytics',
  'password' = 'analytics123',
  'driver' = 'org.postgresql.Driver'
);

-- =============================================
-- EXECUTE BOTH PIPELINES
-- =============================================

-- Pipeline 1: Top Sales per City
-- Pipeline 2: Top Salesman in the Country

BEGIN STATEMENT SET;

INSERT INTO top_sales_per_city_sink
SELECT
  city,
  CAST(SUM(amount) AS DECIMAL(15,2)),
  COUNT(*),
  CURRENT_TIMESTAMP
FROM all_sales
GROUP BY city;

INSERT INTO top_salesman_country_sink
SELECT
  salesman,
  CAST(SUM(amount) AS DECIMAL(15,2)),
  COUNT(*),
  CURRENT_TIMESTAMP
FROM all_sales
GROUP BY salesman;

END;
