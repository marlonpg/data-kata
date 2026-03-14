package com.example.streams.serde;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerde<T> implements Serde<T> {

    private final ObjectMapper mapper;
    private final Class<T> type;

    public JsonSerde(Class<T> type) {
        this.type = type;
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, data) -> {
            if (data == null) return null;
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new org.apache.kafka.common.errors.SerializationException("Error serializing to JSON", e);
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return (topic, data) -> {
            if (data == null) return null;
            try {
                return mapper.readValue(data, type);
            } catch (Exception e) {
                throw new org.apache.kafka.common.errors.SerializationException("Error deserializing from JSON", e);
            }
        };
    }
}
