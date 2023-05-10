package de.deepshore.kafka.models;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class AvroPack {
    private String key;
    private String keySchema;
    private String valueSchema;
    private String value;

    public AvroPack(String valueSchema, String value) {
        this.valueSchema = valueSchema;
        this.value = value;
    }

    public AvroPack(String keySchema, String key, String valueSchema, String value) {
        this.key = key;
        this.keySchema = keySchema;
        this.valueSchema = valueSchema;
        this.value = value;
    }

    public String getValueSchema() {
        return valueSchema;
    }

    public void setValueSchema(String valueSchema) {
        this.valueSchema = valueSchema;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeySchema() {
        return keySchema;
    }

    public void setKeySchema(String keySchema) {
        this.keySchema = keySchema;
    }
}
