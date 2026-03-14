CREATE TABLE sales (
    sale_id SERIAL PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Brazil',
    salesman VARCHAR(100) NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'database',
    created_date TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO sales (amount, description, city, country, salesman, source) VALUES
(250.00, 'Enterprise Software License', 'São Paulo', 'Brazil', 'Carlos Silva', 'database'),
(180.50, 'Cloud Storage Plan', 'Rio de Janeiro', 'Brazil', 'Ana Santos', 'database'),
(420.00, 'Security Suite Annual', 'Belo Horizonte', 'Brazil', 'Pedro Oliveira', 'database'),
(95.00, 'Basic Support Package', 'São Paulo', 'Brazil', 'Maria Costa', 'database'),
(310.75, 'Premium Analytics Tool', 'Curitiba', 'Brazil', 'Carlos Silva', 'database'),
(150.00, 'Email Marketing Service', 'Rio de Janeiro', 'Brazil', 'João Ferreira', 'database'),
(560.00, 'ERP Module', 'São Paulo', 'Brazil', 'Ana Santos', 'database'),
(200.00, 'CRM Integration', 'Salvador', 'Brazil', 'Pedro Oliveira', 'database'),
(75.50, 'Chat Support Plugin', 'Belo Horizonte', 'Brazil', 'Maria Costa', 'database'),
(340.00, 'Data Warehouse License', 'Curitiba', 'Brazil', 'Carlos Silva', 'database'),
(125.00, 'Social Media Manager', 'Porto Alegre', 'Brazil', 'João Ferreira', 'database'),
(480.00, 'AI Assistant Pro', 'São Paulo', 'Brazil', 'Ana Santos', 'database'),
(210.00, 'Project Management Tool', 'Rio de Janeiro', 'Brazil', 'Pedro Oliveira', 'database'),
(90.00, 'File Sharing Service', 'Salvador', 'Brazil', 'Maria Costa', 'database'),
(370.25, 'Network Monitor', 'Belo Horizonte', 'Brazil', 'Carlos Silva', 'database'),
(160.00, 'Backup Solution', 'Curitiba', 'Brazil', 'João Ferreira', 'database'),
(520.00, 'DevOps Pipeline Suite', 'São Paulo', 'Brazil', 'Ana Santos', 'database'),
(280.00, 'Testing Framework', 'Porto Alegre', 'Brazil', 'Pedro Oliveira', 'database'),
(110.00, 'DNS Management', 'Rio de Janeiro', 'Brazil', 'Maria Costa', 'database'),
(445.50, 'Container Platform', 'São Paulo', 'Brazil', 'Carlos Silva', 'database');
