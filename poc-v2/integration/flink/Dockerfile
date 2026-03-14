FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM flink:1.18-java17

# Copy Prometheus metrics reporter
RUN cp /opt/flink/opt/flink-metrics-prometheus-*.jar /opt/flink/lib/

# Download connectors
RUN cd /opt/flink/lib && \
    wget -q https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-kafka/3.1.0-1.18/flink-sql-connector-kafka-3.1.0-1.18.jar && \
    wget -q https://repo1.maven.org/maven2/org/apache/flink/flink-connector-jdbc/3.1.2-1.18/flink-connector-jdbc-3.1.2-1.18.jar && \
    wget -q https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.1/postgresql-42.7.1.jar

# Download OpenLineage Flink listener
RUN cd /opt/flink/lib && \
    wget -q https://repo1.maven.org/maven2/io/openlineage/openlineage-flink/1.24.2/openlineage-flink-1.24.2.jar

# Copy pipeline runner JAR
COPY --from=build /app/target/flink-pipelines-1.0.0.jar /opt/flink/usrlib/flink-pipelines.jar

# Copy SQL
COPY sql/ /opt/flink/sql/
