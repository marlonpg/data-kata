FROM confluentinc/cp-kafka-connect:7.6.0

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:10.7.6

RUN cd /usr/share/confluent-hub-components/confluentinc-kafka-connect-jdbc/lib/ && \
    curl -sO https://jdbc.postgresql.org/download/postgresql-42.7.1.jar
